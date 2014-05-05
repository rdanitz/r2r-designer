(ns server.components.db
  (:require
    [clojure.tools.logging :refer (info warn error debug)]
    [com.stuartsierra.component :as c]
    )
  )

(defrecord Database [spec]
  c/Lifecycle

  (start [component]
    (info "starting database adapter ...")
    component
    )

  (stop [component]
    (info "stopping database adapter ...")
    component
    )
  )
  
(defn new-database [opts]
  (map->Database {:spec (select-keys opts [:subprotocol :subname :user :password])})
  )
