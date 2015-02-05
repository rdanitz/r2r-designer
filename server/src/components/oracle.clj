(ns components.oracle
  "Encapsulates the Suggestion API (LOV or LinDA oracle)."
  (:require
    [com.stuartsierra.component :as c]
    [taoensso.timbre :as timbre]
    [core.oracle :refer :all]))

(timbre/refer-timbre)

(defrecord Oracle [kb endpoint sample threshold limit n]
  c/Lifecycle

  (start [component]
    (info "starting oracle ...")
    (reset! (:kb component) 
            (new-server (:endpoint component)))
    component) 

  (stop [component]
    (info "stopping oracle ...")
    (reset! (:kb component) nil)
    component))
  
(defn new-oracle 
  "Creates a new Oracle adapter with endpoint."
  [endpoint]
  (map->Oracle {:endpoint endpoint
                ;; knowledge base
                :kb (atom nil)
                ;; number of data points to regard
                :sample 20
                ;; min score for suggestions
                :threshold 0.4
                ;; number of suggestions
                :limit 20
                :n 5}))
