(require '[clojure.string :as str]
         '[dudelson.shopify-tools :as st])
(import '[org.jsoup Jsoup])

;; Crawls shopify site to verify that every instance of the "contact us" link is up to date
;; Crawling is done by fetching the root sitemap and then the component sitemaps.
;; Products and collections are omitted, except for a manually specified list.
;; Prints all such instances to stdout so I can manually verify the correctness of the links.
;; In principle this output could also be piped to another command for further processing.
;; Note that this example will not exactly work in its present form because shopify-tools
;;  requires fully qualified URLs, so all relative URLs used here would need to be normalized.

(def ROOT-URL "https://example.com")

;; we don't need to crawl every product and collection, just sampling one of each template is enough
(def additional-urls-to-crawl ["/"
                               "/collections/my-cool-collection"
                               "/products/my-cool-product"
                               "/products/example-gift-card"])

(defn crawl-primary-sitemap []
  ;; crawl primary sitemap and return URLs of component sitemaps
  (->> "/sitemap.xml"
       st/fetch-xml
       (filter #(some-> % :tag name (str/ends-with? "loc")))
       (map :content)
       flatten))

(defn crawl-component-sitemap [sitemap-url]
  ;; crawl component sitemap and return seq of URLs
  (->> sitemap-url
       st/fetch-xml
       (filter (fn [x] (some-> x :tag (#(and (not (str/includes? (namespace %) "image")) (str/ends-with? (name %) "loc"))))))
       (map :content)
       flatten))

(defn main []
  (let [component-sitemaps (->> (crawl-primary-sitemap)
                                (remove (fn [url] (some #(str/includes? url %) ["agentic" "products" "collections"]))))
        sitemap-urls (->> component-sitemaps
                          (map crawl-component-sitemap)
                          flatten
                          (concat additional-urls-to-crawl))]
    ;(println "Got component sitemaps:" (str/join "\n  - " component-sitemaps))
    ;(println "Got sitemap URLs:" (str/join "\n  - " sitemap-urls))
    (println "Instances of 'Contact Us' link:")
    (->> sitemap-urls
         (map
          (fn [url]
            (try
              (-> url
                  st/fetch-html
                  (.select "a[href*=\"contact-us\"]")
                  (.asList)
                  seq)
              (catch Exception e
                (format "Exception for url %s" url)))))
         flatten
         (map #(.attr % "href"))
         (#(doseq [url %]
             (println url))))))

(main)
