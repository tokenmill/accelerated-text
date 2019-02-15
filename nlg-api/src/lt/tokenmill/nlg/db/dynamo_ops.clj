(ns lt.tokenmill.nlg.db.dynamo-ops
  (:require [taoensso.faraday :as far]
            [lt.tokenmill.nlg.db.config :as config]
            [lt.tokenmill.nlg.api.utils :as utils]))


(defn resolve-table
  [type]
  (case type
    :results config/results-table
    :data config/data-table
    :blockly config/blockly-table))

(defprotocol DBAccess
  (read-item [this key])
  (write-item [this key data])
  (update-item [this key data])
  (delete-item [this key])
  (list-items [this limit]))

(defn read! [this key] (read-item this key))
(defn write!
  ([this data]
   (write-item this (utils/gen-uuid) data))
  ([this key data]
   (write-item this key data)))
(defn update! [this key data] (update-item this key data))
(defn delete! [this key] (delete-item this key))
(defn list! [this limit] (list-items this limit))

(defn db-access
  [resource-type]
  (let [table-name (resolve-table resource-type)]
    (reify
      DBAccess
      (read-item [this key]
        (far/get-item config/client-opts table-name {:key key}))
      (write-item [this key data]
        (let [body (-> data
                       (assoc :key key)
                       (assoc :createdAt (utils/ts-now))
                       (assoc :updatedAt (utils/ts-now)))]
          (do
            (far/put-item config/client-opts table-name body)
            body)))
      (update-item [this key data]
        (let [original (far/get-item config/client-opts table-name {:key key})
              body (-> (merge original data)
                       (assoc :updatedAt (utils/ts-now))
                       (assoc :key key))]
          (do
            (far/put-item config/client-opts table-name body)
            body)))
      (delete-item [this key]
        (far/delete-item config/client-opts table-name {:key key}))
      (list-items [this limit]
        (far/scan config/client-opts table-name)))))

(defn get-workspace
  [key]
  (far/get-item config/client-opts config/blockly-table {:id key}))

(defn list-workspaces
  [limit]
  (far/scan config/client-opts config/blockly-table {:limit limit}))


(defn write-workspace
  [key workspace]
  (let [body (assoc workspace :id key)]
    (do
      (far/put-item
       config/client-opts
       config/blockly-table
       body)
      body)))

(defn add-workspace
  [key workspace]
  (let [body (assoc workspace :createdAt (utils/ts-now))]
    (write-workspace key body)))

(defn update-workspace
  [key workspace]
  (let [original (get-workspace key)
        body (merge
              original
              (assoc workspace :updatedAt (utils/ts-now)))]
    (write-workspace key body)))
  

(defn delete-workspace
  [key]
  (far/delete-item config/client-opts config/blockly-table {:id key}))
