;; ---------------------------------------------------------
;; dudelson.shopify-tools.-test
;;
;; Example unit tests for dudelson.shopify-tools
;;
;; - `deftest` - test a specific function
;; - `testing` logically group assertions within a function test
;; - `is` assertion:  expected value then function call
;; ---------------------------------------------------------


(ns dudelson.shopify-tools-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [dudelson.shopify-tools :as shopify-tools]))


(deftest application-test
  (testing "TODO: Start with a failing test, make it pass, then refactor"

    ;; TODO: fix greet function to pass test
    (is (= "dudelson application developed by the secret engineering team"
           (shopify-tools/greet)))

    ;; TODO: fix test by calling greet with {:team-name "Practicalli Engineering"}
    (is (= (shopify-tools/greet "Practicalli Engineering")
           "dudelson service developed by the Practicalli Engineering team"))))
