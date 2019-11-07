(ns acc-text.nlg.verbnet.cf
  (:require [acc-text.nlg.gf.cf-format :as cf]
            [clojure.string :as string]))

(defn build-grammars
  [frames root variables]
  (concat
   (list root)
   frames
   variables))

(defn frame->cf
  [syntax-type themrole-idx {syntax :syntax}]
  (cf/gf-syntax-item "Compl" syntax-type (string/join " " (map
                                                           (fn
                                                             [{:keys [pos value]}]
                                                             (case pos
                                                               :NP (get themrole-idx value)
                                                               :LEX (format "\"%s\"" value)
                                                               :VERB "V2"
                                                               :PREP (format "\"%s\"" value)))
                                                           syntax))))

(defn vn->cf
  [{:keys [members frames thematic-roles]}]
  (let [themrole-idx (into {} (map-indexed (fn [idx {type :type}] [type (format "NP%d" idx)]) thematic-roles))
        root         (cf/gf-syntax-item "Pred" "S" "VP")
        variables    (concat
                      (map (fn [{name :name}] (cf/gf-morph-item "Action" "V2" name)) members)
                      (map (fn [[k v]] (cf/gf-morph-item "Actor" v (cf/data-morphology-value k))) themrole-idx))]
    (-> (partial frame->cf "VP" themrole-idx)
        (map frames)
        (build-grammars root variables))))

