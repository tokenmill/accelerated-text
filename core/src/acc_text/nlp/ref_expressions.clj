(ns acc-text.nlp.ref-expressions
  (:require [acc-text.nlp.utils :as nlp]
            [clojure.tools.logging :as log]
            [clojure.string :as str]))

(defmulti ignored-token? (fn [lang _] lang))

(defmethod ignored-token? "Eng" [_ value] (contains? #{"i" "it"} (str/lower-case value)))

(defmethod ignored-token? :default [_ _] false)

(defn remove-ignored-tokens [lang [idx value]] (ignored-token? lang value))

(defn filter-by-refs-count [[_ refs]] (>= (count refs) 2))

(defn token-distance [[d1 _] [d2 _]]
  (if (seq? d1)
    (- d2 (second d1))
    (- d2 d1)))

(defn merge-tokens [[p1 v1] [p2 v2]] [(flatten (seq [p1 p2])) (str/join " " (log/spyf :debug "Merging: %s" [v1 v2]))])

(defn normalize-merged [tokens]
  (mapcat (fn [[p v]]
         (log/debugf "p: %s v: %s" (pr-str p) v)
            (if (seq? p)
              (concat
               [[(first p) v]]
               (map (fn [idx] [idx ""]) (rest p)))
              [[p v]])) (reverse tokens)))

(defn merge-nearby [tokens]
  (loop [stack tokens
         result ()]
    (if (empty? stack)
      (normalize-merged result)
      (let [[head next & tail] stack]
        (if (and next (= 1 (token-distance head next)))
          (recur (cons (merge-tokens head next) tail) result)
          (recur (rest stack) (cons head result))))))


(defn filter-last-location-token
  [all-tokens group]
  (filter (fn [[idx token]]
            (let [next-token (nth all-tokens (inc idx) "$")]
              (log/tracef "Idx: %s Token: %s Next Token: %s" idx token next-token)
              ;; If it's last word in sentence, don't create ref.
              (not (= "." next-token))))
          group))

(defn referent? [token] (nlp/starts-with-capital? token))

(defn identify-potential-refs
  [lang tokens]
  (->> (map-indexed vector tokens)
       (filter #(referent? (second %)))
       (remove #(remove-ignored-tokens lang %))
       (merge-nearby)
       (map #(log/spyf :debug "Partial result: %s" %))
       (group-by second)
       (filter filter-by-refs-count)
       (map (comp rest second))
       (mapcat (partial filter-last-location-token tokens))))

(defmulti add-replace-token (fn [lang _] lang))

(defmethod add-replace-token "Eng" [_ [idx value]]
  (if (nlp/ends-with-s? value) [idx "its"] [idx "it"]))

(defmethod add-replace-token "Est" [_ [idx _]] [idx "see"])

(defmethod add-replace-token "Ger" [_ [idx _]] [idx "es"])

(defmethod add-replace-token :default [_ _] nil)

(defn check-for-dupes
  "If we have replacement tokens which are exact same in a row [it, it] then mark the next one
  for deletion"
  [replace-tokens]
  (if (= 1 (count replace-tokens))
    replace-tokens
    (loop [[[_ token1 :as t1] [idx2 token2 :as t2] & tokens] replace-tokens
           deduped []]
      (if (empty? t2)
        (concat deduped [t1])
        (if (= token1 token2)
          (recur tokens (concat deduped [t1 [idx2 :delete]]))
          (recur tokens deduped))))))

(defn apply-ref-expressions
  [lang text]
  (let [tokens (nlp/tokenize text)
        smap (->> tokens
                  (identify-potential-refs lang)
                  (map (partial add-replace-token lang))
                  (check-for-dupes)
                  (into {}))]
    (nlp/rebuild-sentences
      (map-indexed (fn [idx v]
                     (cond (= :delete (get smap idx)) ""
                           (contains? smap idx) (get smap idx)
                           :else v))
                   tokens))))
