(ns dev
  "Tools for interactive development with the REPL. This file should
   not be included in a production build of the application."
  (:require
    ;; external namespaces
    [clojure.java.io :as io]
    [clojure.java.shell :as sh]
    [clojure.java.javadoc :refer (javadoc)]
    [clojure.java.jdbc :as jdbc]
    [clojure.pprint :refer (pprint)]
    [clojure.reflect :refer (reflect)]
    [clojure.repl :refer (apropos dir doc find-doc pst source)]
    [clojure.string :as string]
    [clojure.test :as test]
    [clojure.tools.namespace.repl :refer (refresh refresh-all)]
    [clojure.tools.reader.edn :as edn]
    [clojure.set :refer :all]
    [clojure.java.classpath :refer :all]
    [clojure.data.json :as json]
    [clojure.data.csv :as data-csv]
    [com.stuartsierra.component :as c]
    [ring.server.standalone :refer :all]
    [ring.middleware.file-info :refer :all]
    [ring.middleware.file :refer :all]
    [clj-http.client :as client]
    [taoensso.timbre :as timbre]
    [edu.ucdenver.ccp.kr.kb :as kb]
    [edu.ucdenver.ccp.kr.rdf :as rdf]
    [edu.ucdenver.ccp.kr.sparql :as sparql]
    [edu.ucdenver.ccp.kr.sesame.kb :as sesame]
    ;; internal namespaces
    [components.datasource :refer :all]
    [components.oracle :refer :all]
    [components.sparqlify :refer :all]
    [components.openrdf :refer :all]
    [components.ring :refer :all]
    [core.db :refer :all]
    [core.csv :as csv]
    [core.oracle :refer :all]
    [core.sparqlify :refer :all]
    [core.openrdf :refer :all]
    [routes.app :refer [app-fn]]
    [system :refer [new-system]]))

(timbre/refer-timbre)

;; The #'system var holds the system during development in the REPL.

(def system
  "A Var containing an object representing the application under
  development."
  nil)

(def log-config {:ns-whitelist []
                 :ns-blacklist []})

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  (let [db-opts {:driver "org.postgresql.ds.PGSimpleDataSource"
                 :host "localhost"
                 :name "mydb"
                 :username "postgres"
                 :password ""
                 }
        ring-opts {:port 3000
                   :open-browser? false
                   :join true
                   :auto-reload? true}
        oracle-sparql-endpoint "http://localhost:8080/openrdf-sesame"
        sparqlify-opts {:host "http://localhost"
                        :port 7531}
        openrdf-opts {:host "http://localhost:8080/openrdf-sesame"
                      :repo "r2r"
                      :base-uri "http://mycompany.com"}]
    (alter-var-root #'system (constantly (new-system db-opts #'app-fn ring-opts oracle-sparql-endpoint log-config sparqlify-opts openrdf-opts)))))

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (alter-var-root #'system c/start))

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  (alter-var-root #'system
    (fn [s] (when s (c/stop s))))
  :stopped)

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after 'dev/go))
