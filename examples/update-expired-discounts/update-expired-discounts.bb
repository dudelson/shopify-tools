#!/usr/bin/env bb

;; This example updates the end date of multiple discounts using the admin API
;; Discounts that expire in 2026 are updated to expire one year later.
;; The current discount data was fetched using the admin API separately and is
;;  provided here as a json file.

(require '[clojure.string :as str]
         '[dudelson.shopify-tools :as st]
         '[cheshire.cheshire :as json])

(def STORE-HANDLE "abcdef-g")
(def ADMIN-API-ACCESS-TOKEN "<your access token here>")

(defn parse-input [filename]
  (-> filename
      slurp
      (json/decode true)))

(let [{:keys [graphql]} (st/configure-admin-api STORE-HANDLE ADMIN-API-ACCESS-TOKEN)
      data (parse-input "data.json")
      discounts (get-in data [:data :discountNodes :nodes])
      query-strs ["mutation($codeAppDiscount: DiscountCodeAppInput!, $id: ID!) {"
                  "  discountCodeAppUpdate(id: $id, codeAppDiscount: $codeAppDiscount) {"
                  "    codeAppDiscount { title status startsAt endsAt }"
                  "    userErrors { field message }"
                  "}}"]
      query (str/join "\n" query-strs)]
  (->> discounts
       (map
        (fn [discount]
          {:id (get-in discount [:discount :discountId])
           :codeAppDiscount {:endsAt (str/replace
                                      (get-in discount [:discount :startsAt])
                                      #"2026"
                                      "2027")}}))
       (#(doseq [vars %]
           (println
            "\n"
            (graphql query vars))
           (Thread/sleep 500)))))
