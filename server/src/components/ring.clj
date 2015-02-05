(ns components.ring
  "Encapsulates the Ring web server holding the web app."
  (:require
    [com.stuartsierra.component :as c]
    [taoensso.timbre :as timbre]
    [routes.app :refer [app-fn]]
    [ring.server.standalone :refer [serve]]))

(timbre/refer-timbre)

(defrecord Ring [server app-fn opts db-api oracle-api]
  c/Lifecycle

  (start [component] 
    (info "starting ring adapter ...")
    (let [app (app-fn component)]
      (reset! server (serve app opts))
      component))

  (stop [component] 
    (info "stopping ring adapter ...") 
    (if @server
      (.stop @server)
      (reset! server nil))
    component))

(defn new-ring 
  "Creates a new Ring adapter, holding the server and all api pathes."
  [app-fn opts]
  (map->Ring {:opts opts 
              :server (atom nil) 
              :app-fn app-fn
              :db-api "/api/v1/db"
              :csv-api "/api/v1/csv"
              :oracle-api "/api/v1/oracle"
              :transform-api "/api/v1/transform"
              :file-store (atom {})}))
