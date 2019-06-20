(ns graphql.domain.dictionary
  (:require [nlg.dictionary :as dictionary-api]
            [translate.dictionary :as translate-dict]))


(defn dictionary [_ _ _]
  (dictionary-api/list-dictionary-items))

(defn dictionary-item [_ arguments _]
  (dictionary-api/dictionary-item arguments))

(defn create-dictionary-item [_ arguments _]
  )

(defn delete-dictionary-item [_ arguments _]
  )

(defn phrase-usage-models [_ _ value]
  (:phrase-usage-model (dictionary-api/phrase-usage-models {:ids (:usageModels value)})))

(defn reader-usage [_ _ value]
  (:reader-flag-usage (dictionary-api/reader-flag-usages {:ids (:readerUsage value)})))

(defn reader-flag [_ _ value]
  (dictionary-api/reader-flag {:id (:readerFlag value)}))

(defn update-reader-flag-usage [_ arguments _]
  (dictionary-api/update-reader-flag-usage arguments))

(defn update-phrase-usage-model [_ arguments _]
  (dictionary-api/update-phrase-usage arguments))

(defn create-phrase-usage-model [_ {:keys [dictionaryItemId phrase defaultUsage] :or {defaultUsage :YES}} _]
  (let [usage-models (:phrases (dictionary-api/dictionary-item {:id dictionaryItemId}))
        new-phrase-model-id (:id (dictionary-api/create-phrase-usage-model {:phrase phrase :defaultUsage defaultUsage}))]
    (dictionary-api/update-dictionary-item-usage-models {:id dictionaryItemId :phrases (conj usage-models new-phrase-model-id)})))

(defn delete-phrase-usage-model [_ {:keys [id]} _]
  (let [dictionary-id (dictionary-api/dictionary-item-id-that-contains-phrase-model {:id id})
        usage-models (:phrases (dictionary-api/dictionary-item {:id dictionary-id}))]
    (dictionary-api/delete-phrase-usage-model {:id id})
    (dictionary-api/update-dictionary-item-usage-models {:id dictionary-id :phrases (remove #(= id %) usage-models)})))
