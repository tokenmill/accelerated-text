(ns lt.tokenmill.nlg.api.resource
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [lt.tokenmill.nlg.api.utils :as utils]
            [cheshire.core :as ch])
  (:import (java.io BufferedWriter)))


(defn preflight
  []
  {:status 200})

(defn dummy [& args] {:status 200
                :body {:text "I do nothing."}})

(defn build-resource
  [resources decode-body]
  (let [{:keys [get-handler post-handler delete-handler put-handler]} resources]
    (fn [_ is os _]
      (let [input (utils/decode-body is)
            method (input :httpMethod)
            path-params (input :pathParameters)
            query-params (input :queryStringParameters)
            request-body (if decode-body (ch/decode (input :body) true) (input :body))
            {:keys [status body]} (case (keyword method)
                                    :GET    (if get-handler (get-handler query-params path-params) (dummy))
                                    :DELETE (if delete-handler (delete-handler path-params) (dummy))
                                    :POST   (if post-handler (post-handler request-body) (dummy))
                                    :PUT    (if put-handler (put-handler path-params request-body) (dummy))
                                    :OPTIONS (preflight))]
        (log/debugf "Method: %s" method)
        (log/debugf "Path Params: %s" path-params)
        (log/debugf "Query Params: %s" query-params)
        (log/debugf "Received '%s' and produced output '%s'" input body)
        (with-open [^BufferedWriter w (io/writer os)]
          (.write w ^String (utils/resp status body)))))))
  
  
