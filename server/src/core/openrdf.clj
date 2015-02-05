(ns core.openrdf
  "core OpenRDF functions"
  (:require
    [com.stuartsierra.component :as c]
    [taoensso.timbre :as timbre])
  (:import
    org.openrdf.rio.RDFFormat
    org.openrdf.model.Resource))

(timbre/refer-timbre)

(defn upload! 
  "uploads a N3 triple dump to OpenRDF"
  [c f]
  (let [repo @(:server c)
        uri (:base-uri c)]
    (with-open [conn (.getConnection repo)]
      (.add conn f uri RDFFormat/NTRIPLES (into-array Resource [])))
    (str (:host c) "/" (:repo c))))
