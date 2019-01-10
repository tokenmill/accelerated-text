(ns lt.tokenmill.nlg.generator.segment-test
  (:require [clojure.test :refer :all]
            [lt.tokenmill.nlg.generator.segment :refer :all]
            [lt.tokenmill.nlg.generator.templates :refer :all]))

(deftest test-dummy-generator
  (let [test-data {:product-name "Nike Air Max 95 Premium"
                   :relation "provide"
                   :adverb "exceptional"
                   :features ["support" "comfort"]}
        expected "The Nike Air Max 95 Premium provides exceptional support and comfort."]
    (testing "Generate 'Nike Air Max 95 Premium' example"
      (is (= expected (product-1 test-data))))
    (testing "Call correct template with given parameters"
      (is (= expected (generate-text test-data))))))
  

