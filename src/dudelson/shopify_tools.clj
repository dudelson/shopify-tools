;; ---------------------------------------------------------
;; dudelson.shopify-tools
;;
;; Utilities for managing shopify-related data and projects
;; ---------------------------------------------------------

;; TODO LIST
;; [general]
;; [X] do I need `gen-class` in my ns decl? It was automatically added by
;;     practicalli template but I don't know what it does.
;; [X] add error handling and automatic retries to `call-admin-api`
;; [X] call-admin-api should take optional arg `api-version`. How do I do
;;     optional args in clojure?
;; [ ] add types to everything using spec
;;
;; [Admin API]
;; [ ] compile data from multiple requests together using the graphql cursor if
;;     we get a config option to do so (?)
;;       - this is a little bit iffy bc this requires the pageInfo obj to be
;;         requested by the query, which is passed in from the caller
;;       - but i guess i can search for it in the query string
;;       - but maybe it should be passed in the opts in order to make it explicit
;; [ ] accept a config option to automatically cache response
;;       - some queries are expensive so we want to save the response data to
;;         disk, and read that cached responsed if possible
;;         if we get the same query again.

(ns dudelson.shopify-tools
  (:require [clojure.data.xml :as xml]
            [babashka.http-client :as http]
            [cheshire.core :as json])
  (:import [org.jsoup Jsoup]))

(defn try-admin-api
  "Try an admin API call one time.
  Error signalling is kept very straightforward for this function: `nil` is
  interpreted as meaning the call failed and should be retried if possible.
  A successful API call should never return simply `nil`."
  [store-handle access-token body {:keys [api-version]}]
  (try
    (-> (format "https://%s.myshopify.com/admin/api/%s/graphql.json"
                store-handle api-version)
        (http/post {:headers {:content-type "application/json"
                              :x-shopify-access-token access-token}
                    :body (json/encode body)})
        :body
        (json/decode true))
    (catch Exception e
      (printf "Error while making admin API call. %s%n" (ex-message e)))))

(def ADMIN-API-DEFAULT-OPTS {:n-retries 3
                             :api-version "2026-07"
                             ;; these are currently unused
                             :use-cursor false
                             :cache-response false})

(defn call-admin-api
  "Note that <n> retries means the call will be made n+1 times in total, since
  the first attempt does not count as a retry."
  ([store-handle access-token body]
   (call-admin-api store-handle access-token body ADMIN-API-DEFAULT-OPTS))
  ([store-handle access-token body {:keys [n-retries] :as opts}]
   (loop [n 0]
     (if-let [resp (try-admin-api store-handle access-token body opts)]
       (if-not (:errors resp)
         resp
         (throw (ex-info "Admin API request complete with errors"
                         {:gql-errors (:errors resp)})))
       (if (< n n-retries)
         (do
           (Thread/sleep (min (* n 1000) 10000))
           (recur (inc n)))
         (throw (ex-info "Admin API request timed out" {})))))))

(defn graphql
  ([store-handle access-token query-str vars]
   (graphql store-handle access-token query-str vars ADMIN-API-DEFAULT-OPTS))
  ([store-handle access-token query-str vars opts]
   (call-admin-api
    store-handle
    access-token
    {:query query-str, :variables vars}
    opts)))

(defn configure-admin-api [store-handle access-token]
  (let [admin-api (partial call-admin-api store-handle access-token)
        graphql (partial graphql store-handle access-token)]
    {:call-admin-api admin-api
     :graphql graphql}))

(defn http-get [url]
  (try
    (http/get url)
    (catch Exception e
      (if-let [data (ex-data e)]
        (format "Fetch for %s failed with status code %d (%s)"
                url (:status data) (.getMessage e))
        (format "Fetch for %s failed: %s" url (.getMessage e))))))

(defn fetch-html
  "Fetches HTML for some webpage. <url> should be a fully qualified URL."
  [url]
  (-> url http-get :body Jsoup/parse))

(defn fetch-xml
  "Fetch XML. <url> should be fully qualified. Returns XML seq."
  [url]
  (-> url http-get :body xml/parse-str xml-seq))
