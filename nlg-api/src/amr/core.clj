(ns amr.core
  (:require [data-access.entities.amr :as amr-entity]))

(def see-amr
  {:id "see",
 :thematic-roles
 (list {:type "Agent"}
  {:type "coAgent"}),
 :frames
 (list
  {:examples (list "Harry sees Sally."),
   :syntax
   (list
    {:pos :NP, :value "Agent"}
    {:pos :VERB}
    {:pos :PREP}
    {:pos :NP, :value "coAgent"})})})

(defn get-rule
  [id]
  (case id
    "See" see-amr
    (amr-entity/get-verbclass id)))
