(ns data.datomic.impl
  (:require [api.config :refer [conf]]
            [clojure.tools.logging :as log]
            [data.protocol :as protocol]
            [datomic.api :as d]
            [mount.core :refer [defstate]]
            [data.datomic.utils :as utils :refer [remove-nil-vals]]
            [data.utils :refer [ts-now gen-uuid]]
            [data.datomic.blockly :as blockly]
            [jsonista.core :as json]))

(def read-mapper (json/object-mapper {:decode-key-fn true}))

(defn encode-results [results]
  (map json/write-value-as-string results))

(defn decode-results [results]
  (map #(json/read-value % read-mapper) results))

(defstate conn :start (utils/get-conn conf))

(defmulti transact-item (fn [resource-type _ _] resource-type))

(defmethod transact-item :data-files [_ key data-item]
  @(d/transact conn [{:data-file/id       key
                      :data-file/filename (:filename data-item)
                      :data-file/content  (:content data-item)}]))

(defn prepare-reader-flag [flag value]
  {:reader-flag/id    (gen-uuid)
   :reader-flag/name  flag
   :reader-flag/value value})

(defn prepare-reader-flags [flags]
  (for [[flag value] flags]
    (prepare-reader-flag flag value)))

(defn prepare-dictionary-item [key data-item]
  {:db/id                            [:dictionary-combined/id key]
   :dictionary-combined/id           key
   :dictionary-combined/name         (:name data-item)
   :dictionary-combined/partOfSpeech (:partOfSpeech data-item)
   :dictionary-combined/phrases      (->> (:phrases data-item)
                                          (map (fn [{:keys [id text flags]}]
                                                 (remove-nil-vals
                                                  {:phrase/id    id
                                                   :phrase/text  text
                                                   :phrase/flags (prepare-reader-flags flags)})))
                                          (remove empty?))})

(defmethod transact-item :dictionary-combined [_ key data-item]
  (try
    @(d/transact conn [(remove-nil-vals (dissoc (prepare-dictionary-item key data-item) :db/id))])
    (assoc data-item :key key)
    (catch Exception e (.printStackTrace e))))

(defmethod transact-item :results [_ key data-item]
  @(d/transact conn [(remove-nil-vals
                       {:results/id    key
                        :results/ts    (ts-now)
                        :results/ready (:ready data-item)})]))

(defn prepare-inflections [inflections]
  (->> inflections
       (map (fn [[key value]]
              (remove-nil-vals
               {:inflection/id    (gen-uuid)
                :inflection/key   key
                :inflection/value value})))
       (remove nil?)))

(defn prepare-tenses [tenses]
  (->> tenses
       (map (fn [[key value]]
              (remove-nil-vals
               {:tense/id     (gen-uuid)
                :tense/key    key
                :tense/value  value})))
       (remove nil?)))

(defn prepare-multilang-dict [id {:keys [key language pos definition inflections gender tenses sense]}]
  {:db/id                            [:dictionary-multilang/id id]
   :dictionary-multilang/id          id
   :dictionary-multilang/key         key
   :dictionary-multilang/language    (keyword language)
   :dictionary-multilang/pos         (keyword pos)
   :dictionary-multilang/gender      gender
   :dictionary-multilang/sense       sense
   :dictionary-multilang/tenses      (prepare-tenses tenses)
   :dictionary-multilang/inflections (prepare-inflections inflections)
   :dictionary-multilang/definition  definition})

(defn read-inflection [inflection]
  (log/debugf "Inflection: %s" inflection)
  inflection)

(defn read-tense [tense]
  (log/debugf "Tense: %s" tense)
  tense)

(defn read-multilang-dict-item [item]
  (log/spyf "Multilang dict item: %s"
            {:id          (:dictionary-multilang/id item)
             :key         (:dictionary-multilang/key item)
             :language    (:dictionary-multilang/language item)
             :pos         (:dictionary-multilang/pos item)
             :gender      (:dictionary-multilang/gender item)
             :definition  (:dictionary-multilang/definition item)
             :sense       (:dictionary-multilang/sense item)
             :tenses      (map read-tense (:dictionary-multilang/tenses item))
             :inflections (map read-inflection (:dictionary-multilang/inflections item))}))

(defmethod transact-item :dictionary-multilang [_ key data-item]
  (try
    @(d/transact conn [(remove-nil-vals
                        (dissoc (prepare-multilang-dict key data-item) :db/id))])
    (catch Exception e (.printStackTrace e))))

(defmethod transact-item :reader-flag [_ key value]
  (try
    @(d/transact conn [(remove-nil-vals
                        (dissoc (prepare-reader-flag key value) :db/id))])
    (catch Exception e (.printStackTrace e))))

(defn prepare-rgl-syntax-params [params]
  (->> params
       (map (fn [{:keys [id type role]}]
              (remove-nil-vals
               {:param/id   id
                :param/type type
                :param/role role})))
       (remove empty?)))

(defn prepare-rgl-syntax [syntax]
  (->> syntax
       (map (fn [{:keys [role ret value params pos type]}]
              (remove-nil-vals
                {:syntax/role   role
                 :syntax/ret    ret
                 :syntax/value  value
                 :syntax/pos    pos
                 :syntax/type   type
                 :syntax/params (prepare-rgl-syntax-params params)})))
       (remove empty?)))

(defn prepare-rgl [key {:keys [name label module kind roles frames]}]
  (remove-nil-vals
    {:db/id      [:rgl/id key]
     :rgl/id     key
     :rgl/kind   kind
     :rgl/roles  (->> roles
                      (map (fn [{:keys [type label input]}]
                             (remove-nil-vals
                               {:role/type  type
                                :role/label label
                                :role/input input})))
                      (remove empty?))
     :rgl/label  label
     :rgl/name   name
     :rgl/module module
     :rgl/frames (->> frames
                      (map (fn [{:keys [examples syntax]}]
                             (remove-nil-vals
                               {:frame/examples (seq examples)
                                :frame/syntax   (prepare-rgl-syntax syntax)})))
                      (remove empty?))}))

(defmethod transact-item :rgl [_ key data-item]
  (try
    @(d/transact conn [(remove-nil-vals (dissoc (prepare-rgl key data-item) :db/id))])
    (assoc data-item :id key)
    (catch Exception e (.printStackTrace e))))

(defmethod transact-item :default [resource-type key _]
  (log/warnf "Default implementation of transact-item for the '%s' with key '%s'" resource-type key)
  (throw (RuntimeException. (format "DATOMIC TRANSACT-ITEM FOR '%s' NOT IMPLEMENTED" resource-type))))

(defmulti pull-entity (fn [resource-type _] resource-type))

(defmethod pull-entity :data-files [_ key]
  (let [data-file (ffirst (d/q '[:find (pull ?e [*])
                                 :in $ ?key
                                 :where
                                 [?e :data-file/id ?key]]
                               (d/db conn)
                               key))]
    (when data-file
      {:id       (:data-file/id data-file)
       :filename (:data-file/filename data-file)
       :content  (:data-file/content data-file)})))

(defn restore-reader-flags [flags]
  (into {} (for [{:reader-flag/keys [name value]} flags]
             [name value])))

(defmethod pull-entity :reader-flag [_ key]
  (:reader-flag/value (ffirst (d/q '[:find (pull ?e [*])
                                     :in $ ?key
                                     :where [?e :reader-flag/name ?key]]
                                   (d/db conn)
                                   key))))

(defmethod pull-entity :dictionary-combined [_ key]
  (let [dictionary-entry (ffirst (d/q '[:find (pull ?e [*])
                                        :in $ ?key
                                        :where [?e :dictionary-combined/id ?key]]
                                      (d/db conn)
                                      key))]
    (when dictionary-entry
      {:key     (:dictionary-combined/id dictionary-entry)
       :name    (:dictionary-combined/name dictionary-entry)
       :phrases (map (fn [phrase] {:id    (:phrase/id phrase)
                                   :text  (:phrase/text phrase)
                                   :flags (restore-reader-flags (:phrase/flags phrase))})
                     (:dictionary-combined/phrases dictionary-entry))})))

(defmethod pull-entity :results [_ key]
  (let [entity (->> (d/q '[:find (pull ?e [*])
                           :where
                           [?e :results/id ?key]]
                         (d/db conn)
                         key)
                    (flatten)
                    (filter (comp (partial = key) :results/id))
                    (sort-by :results/ts #(compare %2 %1))
                    (first))]
    (when entity
      {:id      key
       :ready   (:results/ready entity)
       :error   (:results/error entity)
       :message (:results/message entity)
       :results (decode-results (:results/results entity))})))

(defn read-rgl-entity [entity]
  {:id     (:rgl/id entity)
   :kind   (:rgl/kind entity)
   :name   (:rgl/name entity)
   :label  (:rgl/label entity)
   :module (:rgl/module entity)
   :roles  (map (fn [role]
                  (remove-nil-vals
                    {:type  (:role/type role)
                     :label (:role/label role)
                     :input (:role/input role)}))
                (:rgl/roles entity))
   :frames (map (fn [frame]
                  (remove-nil-vals
                    {:examples (:frame/examples frame)
                     :syntax   (map (fn [syntax]
                                      (remove-nil-vals
                                        {:role   (:syntax/role syntax)
                                         :ret    (:syntax/ret syntax)
                                         :value  (:syntax/value syntax)
                                         :params (map (fn [param]
                                                        (remove-nil-vals
                                                          {:id   (:param/id param)
                                                           :type (:param/type param)
                                                           :role (:param/role param)}))
                                                      (:syntax/params syntax))
                                         :pos    (:syntax/pos syntax)
                                         :type   (:syntax/type syntax)}))
                                    (:frame/syntax frame))}))
                (:rgl/frames entity))})

(defmethod pull-entity :rgl [_ key]
  (let [entity (ffirst (d/q '[:find (pull ?e [*])
                              :in $ ?key
                              :where [?e :rgl/id ?key]]
                            (d/db conn)
                            key))]
    (cond-> entity (some? entity) (read-rgl-entity))))

(defmethod pull-entity :dictionary-multilang [_ key]
  (map
   (fn [[item]] (read-multilang-dict-item item))
   (d/q '[:find (pull ?e [*])
          :in $ ?key
          :where [?e :dictionary-multilang/key ?key]]
        (d/db conn)
        key)))

(defmethod pull-entity :default [resource-type key]
  (log/warnf "Default implementation of pull-entity for the '%s' with key '%s'" resource-type key)
  (throw (RuntimeException. (format "DATOMIC PULL-ENTITY FOR '%s' NOT IMPLEMENTED" resource-type))))

(defmulti pull-n (fn [resource-type _] resource-type))

(defmethod pull-n :data-files [_ limit]
  (let [resp (map first (d/q '[:find (pull ?e [*])
                               :where [?e :data-file/id]]
                             (d/db conn)))]

    (map (fn [df] {:id       (:data-file/id df)
                   :filename (:data-file/filename df)
                   :content  (:data-file/content df)}) (take limit resp))))

(defmethod pull-n :reader-flag [_ limit]
  (restore-reader-flags
    (take limit (map first (d/q '[:find (pull ?e [*])
                                  :where [?e :reader-flag/value]]
                                (d/db conn))))))

(defmethod pull-n :dictionary-combined [_ limit]
  (take limit (map (fn [[item]]
                     {:key          (:dictionary-combined/id item)
                      :name         (:dictionary-combined/name item)
                      :partOfSpeech (:dictionary-combined/partOfSpeech item)
                      :phrases      (map (fn [phrase] {:id    (:phrase/id phrase)
                                                       :text  (:phrase/text phrase)
                                                       :flags (restore-reader-flags (:phrase/flags phrase))})
                                         (:dictionary-combined/phrases item))})
                   (d/q '[:find (pull ?e [*])
                          :where [?e :dictionary-combined/id]]
                        (d/db conn)))))


(defmethod pull-n :dictionary-multilang [_ limit]
  (take limit (map (fn [[item]] (read-multilang-dict-item item))
                   (d/q '[:find (pull ?e [*])
                          :where [?e :dictionary-multilang/id]]
                        (d/db conn)))))

(defmethod pull-n :rgl [_ limit]
  (take limit (map (fn [[item]]
                     (read-rgl-entity item))
                   (d/q '[:find (pull ?e [*])
                          :where [?e :rgl/id]]
                        (d/db conn)))))

(defmethod pull-n :default [resource-type limit]
  (log/warnf "Default implementation of list-items for the '%s' with key '%s'" resource-type limit)
  (throw (RuntimeException. (format "DATOMIC PULL-N FOR '%s' NOT IMPLEMENTED" resource-type))))

(defmulti scan (fn [resource-type _] resource-type))

(defmethod scan :default [resource-type opts]
  (log/warnf "Default implementation of SCAN for the '%s' with key '%s'" resource-type opts)
  (throw (RuntimeException. (format "DATOMIC SCAN FOR '%s' NOT IMPLEMENTED" resource-type))))

(defn query-multilang-dictionary
  ([key] ;; Hackish way, but at the time system is built this way
   (d/q '[:find (pull ?e [*])
          :in $ ?key
          :where [?e :dictionary-multilang/key ?key]]
            (d/db conn)
            key))
  ([key pos senses]
   (d/q '[:find (pull ?e [*])
          :in $  [?key ?pos ?senses]
          :where [?e :dictionary-multilang/key ?key]
                 [?e :dictionary-multilang/pos ?pos]
                 [?e :dictionary-multilang/sense ?sense]
                 [(contains? ?senses ?sense)]]
            (d/db conn)
            [key pos (set senses)])))

(defmethod scan :dictionary-multilang [_ {:keys [key pos senses]}]
  (map (fn [[item]] (read-multilang-dict-item item))
       (if (and (some? pos) (some? senses))
         (query-multilang-dictionary key (keyword pos) senses)
         (query-multilang-dictionary key))))

(defmulti delete (fn [resource-type _] resource-type))

(defmethod delete :dictionary-combined [_ key]
  @(d/transact conn [[:db.fn/retractEntity [:dictionary-combined/id key]]])
  nil)

(defmethod delete :rgl [_ key]
  @(d/transact conn [[:db.fn/retractEntity [:rgl/id key]]])
  nil)

(defmethod delete :default [resource-type opts]
  (log/warnf "Default implementation of DELETE for the '%s' with key '%s'" resource-type opts)
  (throw (RuntimeException. (format "DATOMIC DELETE FOR '%s' NOT IMPLEMENTED" resource-type))))

(defmulti update! (fn [resource-type _ _] resource-type))

(defmethod update! :results [_ key data-item]
  @(d/transact conn [(remove-nil-vals
                       {:db/id           [:results/id key]
                        :results/ready   (:ready data-item)
                        :results/error   (:error data-item)
                        :results/results (encode-results (:results data-item))
                        :results/message (:message data-item)
                        :results/ts      (ts-now)})]))

(defmethod update! :dictionary-combined [resource-type key data-item]
  (try
    @(d/transact conn [[:db.fn/retractEntity [:dictionary-combined/id key]]])
    @(d/transact conn [(remove-nil-vals (dissoc (prepare-dictionary-item key data-item) :db/id))])
    (catch Exception e
      (log/errorf "Error %s with data %s" e val)))
  (pull-entity resource-type key))

(defmethod update! :default [resource-type key data]
  (log/errorf "Default UPDATE for %s with key %s and %s" resource-type key data)
  (throw (RuntimeException. (format "DATOMIC UPDATE FOR '%s' NOT IMPLEMENTED" resource-type))))

(defmethod delete :blockly [_ key]
  (blockly/delete conn key))
(defmethod pull-entity :blockly [_ key]
  (blockly/pull-entity conn key))
(defmethod update! :blockly [_ key data-item]
  (blockly/update! conn key data-item))
(defmethod scan :blockly [_ _]
  (blockly/scan conn))
(defmethod transact-item :blockly [_ key data-item]
  (blockly/transact-item conn key data-item))

(defn db-access
  [resource-type config]
  (log/debugf "Datomic for: %s with config %s" resource-type config)
  (reify
    protocol/DBAccess
    (read-item [this key]
      (pull-entity resource-type key))
    (write-item [this key data update-count?]
      (transact-item resource-type key data))
    (update-item [this key data]
      (try
        (update! resource-type key data)
        (catch Exception e
          (.printStackTrace e))))
    (delete-item [this key] (delete resource-type key))
    (list-items [this limit] (pull-n resource-type limit))
    (scan-items [this opts] (scan resource-type opts))
    (batch-read-items [this ids]
      (throw (RuntimeException. (format "DATOMIC BATCH-READ-ITEMS FOR '%s' NOT IMPLEMENTED" resource-type))))))
