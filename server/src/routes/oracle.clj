(ns routes.oracle
  (:require 
    [compojure.core :refer :all]
    [taoensso.timbre :as timbre]
    [ring.util.response :refer [response]]
    [core.oracle :refer :all]
    [routes :refer [preflight]]))

(timbre/refer-timbre)

(defn oracle-routes-fn [c]
  (let [api (:oracle-api c)]
    (defroutes oracle-routes
      (OPTIONS api request (preflight request))
      (POST api request 
        (let [oracle (:oracle c)
              data (:body request)
              suggestions (recommend oracle (:table data) (:columns data))]
          (response suggestions)))
      )))
