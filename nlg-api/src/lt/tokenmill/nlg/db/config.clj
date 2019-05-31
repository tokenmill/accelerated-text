(ns lt.tokenmill.nlg.db.config)

(def blockly-table "blockly-workspace")
(def results-table "nlg-results")
(def data-table "data")
(def lexicon-table "lexicon")
(def data-bucket "accelerated-text-data-files")
(def grammar-bucket "ccg-grammar")

(defn client-opts []
  {:endpoint (or (System/getenv "DYNAMODB_ENDPOINT") "http://localhost:8000")
   :profile "tm"})
