(ns acc-text.nlg.gf.syntax
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]))

(s/def ::label keyword?)

(s/def ::symbol string?)

(s/def ::literal string?)

(s/def ::value (s/or ::symbol ::literal))

(s/def ::row (s/keys :req [::label ::symbol ::values]))

(defn values->cf [values]
  (string/join " " (map (fn [{literal :acc-text.nlg.gf.syntax/literal
                              symbol  :acc-text.nlg.gf.syntax/symbol}]
                          (if literal
                            (format "\"%s\"" literal)
                            symbol)) values)))

(defn ->cf [rows]
  (map (fn [{label :acc-text.nlg.gf.syntax/label
             symbol :acc-text.nlg.gf.syntax/symbol
             values :acc-text.nlg.gf.syntax/values}]
         (format "%s. %s :== %s" label symbol (values->cf values))) rows))

(s/fdef values->cf
  :args (s/coll-of ::value :min-count 1)
  :ret  string?)

(s/fdef ->cf
  :args (s/coll-of ::row :min-count 1)
  :ret  string?)
