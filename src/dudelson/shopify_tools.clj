;; ---------------------------------------------------------
;; dudelson.shopify-tools
;;
;; Utilities for managing shopify-related data and projects
;; ---------------------------------------------------------

;; TODO LIST
;; [ ] do I need `gen-class` in my ns decl? It was automatically added by practicalli template but I don't know what it does.
;; [ ] add types to everything using spec
;; [ ] add error handling and automatic retries to `call-admin-api`
;; [ ] function `call-admin-api` should be private. How do I mark a function as private in clojure?
;; [ ] call-admin-api should take optional arg `api-version`. How do I do optional args in clojure?

(ns dudelson.shopify-tools
  (:require [clojure.data.xml :as xml]
            [babashka.http-client :as http]
            [cheshire.core :as json])
  (:import [org.jsoup Jsoup])
  (:gen-class))

(defn call-admin-api [store-name, access-token, body]
  (http/post (format "https://%s.myshopify.com/admin/api/2026-07/graphql.json" store-name)
             {:headers {:content-type "application/json"
                        :x-shopify-access-token access-token}
              :body (json/encode body)}))

(defn configure-admin-api [store-name access-token]
  (let [admin-api (partial call-admin-api store-name access-token)]
    {:call-admin-api admin-api
     :grahql (fn [query-str vars] (admin-api {:query query-str, :variables vars}))}))

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
