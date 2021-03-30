(ns data.entities.document-plan
  (:require [api.config :refer [conf]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [data.db :as db]
            [data.utils :as utils]
            [mount.core :refer [defstate]]
            [data.entities.user-group :as user-group]))

(defstate document-plans-db :start (db/db-access :document-plan conf))

(defn list-document-plans
  ([group-id]
   (db/scan! document-plans-db {}))
  ([kind group-id]
   (sort-by :name (filter #(= kind (:kind %)) (db/scan! document-plans-db {})))))

(defn get-document-plan [document-plan-id]
  (db/read! document-plans-db document-plan-id))

(defn delete-document-plan [document-plan-id]
  (db/delete! document-plans-db document-plan-id))

(defn add-document-plan
  ([document-plan group-id] (add-document-plan document-plan (or (:id document-plan) (utils/gen-rand-str 16)) group-id))
  ([document-plan provided-id group-id]
   (let [plan (db/write! document-plans-db provided-id document-plan true)]
     (user-group/link-dp group-id provided-id)
     plan)))

(defn update-document-plan [document-plan-id document-plan]
  (db/update! document-plans-db document-plan-id document-plan))

(defn document-plan-path []
  (get conf :document-plan-path (io/resource "document-plans")))

(defn load-document-plan [f]
  (let [dp (utils/read-json f)]
    (cond-> dp (string? (:documentPlan dp)) (update :documentPlan utils/read-json-str))))

(defn initialize []
  (doseq [{id :id :as dp}
          (->> (document-plan-path)
               (utils/list-files)
               (filter #(string/ends-with? (.getName %) ".json"))
               (map load-document-plan))]
    (add-document-plan dp id)))
