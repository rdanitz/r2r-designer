(ns routes.db
  (:require 
    [compojure.core :refer :all]
    [taoensso.timbre :as timbre]
    [ring.util.codec :as codec]
    [core.db :refer :all]
    [clojure.data.json :refer [write-str]]
    [routes :refer [preflight]]))  

(timbre/refer-timbre)

(defn db-routes-fn [component]
  (let [api (:db-api component)
        db (:datasource component)]
    (defroutes db-routes
      (GET (str api "/tables") [] 
           (try 
             (write-str (get-tables db))
             (catch Exception _ {:status 500})))

      (GET (str api "/table-columns") [] 
           (try 
             (write-str (get-table-columns db))
             (catch Exception _ {:status 500})))

      (GET (str api "/column") [table name :as r] (write-str (query-column db table name)))

      (OPTIONS (str api "/test") request (preflight request))
      (POST (str api "/test") [driver host name username password :as r]
        (let [spec {:driver driver
                    :host host
                    :name name 
                    :username username
                    :password password}
              result (test-db spec)]
          {:status 200 :body (str result)}))

      ; TODO: password is sent in plain text!
      (OPTIONS (str api "/register") request (preflight request))
      (POST (str api "/register") [driver host name username password :as r]
        (let [db (:datasource component)
              new-spec {:host host
                        :driver driver
                        :name name 
                        :username username
                        :password password}]
          (register-db db new-spec)
          {:status 200})))))
