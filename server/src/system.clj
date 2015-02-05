(ns system
  "This holds the system configuration at runtime.
   Every state is encapsuled in the #'system var."
  (:require
    [taoensso.timbre :as timbre]
    [com.stuartsierra.component :as c]
    [components.datasource :refer :all]
    [components.oracle :refer :all]
    [components.sparqlify :refer :all]
    [components.openrdf :refer :all]
    [components.ring :refer :all]
    [components.logging :refer :all]
    [routes.app :refer [app-fn]])
  (:gen-class))

(timbre/refer-timbre)

(defn new-system 
  "Creates a new system configuration as pure data."
  [db-opts app-fn ring-opts oracle-sparql-endpoint log-config sparqlify-opts openrdf-opts] 
  (c/system-map
    :log (c/using (new-logger log-config) [])
    :datasource (c/using (new-datasource db-opts) [:log])
    :oracle (c/using (new-oracle oracle-sparql-endpoint) [:datasource :log])
    :sparqlify (c/using (new-sparqlify sparqlify-opts) [:datasource :log])
    :openrdf (c/using (new-openrdf openrdf-opts) [:log])
    :ring (c/using (new-ring app-fn ring-opts) [:datasource :oracle :sparqlify :openrdf :log])
    ))

;; configuration options

;; logging
(def log-config {:ns-whitelist [] 
                 :ns-blacklist []})

;; initial database configuration
(def db-opts {:driver "" 
              :host "" 
              :name "" 
              :username "" 
              :password ""}) 

;; ring web server options
(def ring-opts {:port 3000 
                :open-browser? false 
                :join true 
                :auto-reload? true})

;; SPARQL endpoint for additional information from the oracle.
;; As of now, the Linked Open Vocabulary SPARQL endpoint has been discontinued;
;; use /resources/lov.n3 to populate a private OpenRDF repository or use another one.
(def oracle-sparql-endpoint "http://localhost:8080/openrdf-sesame")

;; Where to open up the SPARQL endpoint?
(def sparqlify-opts {:host "http://localhost"
                     :port 7531}) 

;; Where to dump to the created data?
(def openrdf-opts {:host "http://localhost:8080/openrdf-sesame"
                   :repo "r2r"
                   :base-uri "http://mycompany.com"})

;; This holds the system at runtime.
;; You can dereference the state with '@system'.
(def system (atom (new-system 
                    db-opts 
                    #'app-fn 
                    ring-opts 
                    oracle-sparql-endpoint 
                    log-config 
                    sparqlify-opts
                    openrdf-opts)))

;; Creates the web app by calling a creater function.
(def app (app-fn @system))
 
(defn init 
  "called when web app is initialized"
  []
  (info "init r2r-designer/server.system")
  (when @system (swap! system c/start)))

(defn destroy 
  "called when web app is destroyed"
  []
  (info "destroy r2r-designer/server.system")
  (when @system (swap! system c/stop)))

;; Configure new system and start it.
;; When running as a jar, the init fn has to be called by hand.
(defn -main []
  (info "calling server.system/-main")
  (init))
