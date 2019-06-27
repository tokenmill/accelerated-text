(ns graphql.dictionary-test
  (:require [clojure.test :refer :all]
            [graphql.core :as graph]
            [jsonista.core :as json]
            [clojure.tools.logging :as log]
            [clojure.set :as set]
            [data-access.entities.dictionary :as dict-entity]))

(defn prepare-environment [f]
  (dict-entity/create-dictionary-item {:key "test-phrase"
                                       :partOfSpeech "VB"
                                       :phrases ["t1" "t2" "t3"]})
  (f)
  (dict-entity/delete-dictionary-item "test-phrqase"))

(defn normalize-resp [resp]
  (-> resp (json/write-value-as-string) (json/read-value)))

(defn exists-pair?
  [col [name-part phrases-part]]
  (-> (filter (fn [pair] ;; We cannot deconstruct OrderedMap
                (let [[k1 p1] (first pair)
                      [k2 p2] (second pair)]
                  (and (= p1 (get name-part k1))
                       (= (set p2) (set (get phrases-part k2))))))
              (flatten col))
      (empty?)
      (not)))

(defn exists-item?
  [col item]
  (->> (filter (fn [d] (= d item)) col)
      (empty?)
      (not)))

(deftest ^:integration full-query-test
  (let [result (graph/nlg {:query "{dictionary{items{name partOfSpeech phrases{id text defaultUsage readerFlagUsage{id usage flag{id name}}}}}}"})]
    (is (nil? (:errors result)))))

(deftest ^:integration list-dictionary-phrases
  (let [resp (graph/nlg {:query "{dictionary{items{name phrases{text}}}}"})
        result (->> (:data resp)
                    :dictionary
                    :items)]
    (log/debugf "Result:\t %s\n" (pr-str result))
    (is (exists-pair? result (list {:name "test-phrase"} {:phrases '({:text "t1"} {:text "t2"} {:text "t3"})})))))

(deftest ^:integration get-dictionary-item
  (let [resp (graph/nlg {:query "{dictionaryItem(id: \"test-phrase\"){name partOfSpeech phrases{text}}}"})
        result (->> (:data resp)
                    :dictionaryItem)]
    (is (= "test-phrase" (:name result)))
    (is (= :VB  (:partOfSpeech result)))
    (is (= #{{:text "t1"}
             {:text "t2"}
             {:text "t3"}}
           (set (:phrases result))))))

(use-fixtures :once prepare-environment)
