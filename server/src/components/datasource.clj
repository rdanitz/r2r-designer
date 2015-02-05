(ns components.datasource
  "Encapsulates a database connection, as well as CSV file management."
  (:require
    [taoensso.timbre :as timbre]
    [com.stuartsierra.component :as c]
    [core.db :refer [new-pool]]))

(timbre/refer-timbre)

(defrecord Datasource [spec pool csv-file]
  c/Lifecycle

  (start [c]
    (info "starting datasource adapter ...")
    (reset! (:pool c) (new-pool c))
    c)

  (stop [c]
    (info "stopping datasource adapter ...")
    (when (:pool c) 
      (if @(:pool c) (.close @(:pool c)))
      (reset! (:pool c) nil))
    (reset! (:csv-file c) nil)
    (reset! (:separator c) nil)
    c))
  
(defn new-datasource 
  "Creates a new datasource component.
   Consists of:
   'spec': holds the db params,
   'pool': connection pool for the db,
   'csv-file': current active file descriptor,
   'separator': according csv file separator,
   'max-pool': number of connections."
  [opts]
  (map->Datasource {:spec (atom (select-keys opts [:driver :host :name :username :password]))
                    :pool (atom nil)
                    :csv-file (atom nil)
                    :separator (atom nil)
                    :max-pool 10}))
