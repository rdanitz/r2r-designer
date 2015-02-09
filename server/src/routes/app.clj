(ns routes.app
  "This configures and holds the web app with all its routes."
  (:require
    [clojure.stacktrace :as st]
    [taoensso.timbre :as timbre]
    [ring.middleware.cors :refer :all]
    [ring.middleware.params :refer :all]
    [ring.middleware.multipart-params :refer :all]
    [ring.middleware.json :refer :all]
    [ring.middleware.file-info :refer :all]
    [compojure.core :refer :all]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [routes.db :refer [db-routes-fn]]
    [routes.csv :refer [csv-routes-fn]]
    [routes.oracle :refer [oracle-routes-fn]]
    [routes.transform :refer [transform-routes-fn]]
    ))

(timbre/refer-timbre)

;; some default routes

(defroutes app-routes
  (route/resources "/" {:root "public"})
  (route/not-found "Not Found!"))

;; CORS headers

(defn allow-origin [handler]
  (fn [request]
    (let [response (handler request)
          headers (:headers response)]
      (assoc response :headers (assoc headers "Access-Control-Allow-Origin" "*")))))

(defn allow-content-type [handler]
  (fn [request]
    (let [response (handler request)
          headers (:headers response)]
      (assoc response :headers (assoc headers "Access-Control-Allow-Content-Type" "*")))))

(defn wrap-dir-index [handler]
  (fn [req]
    (handler
     (update-in req [:uri]
                #(if (= "/" %) "/index.html" %)))))

(defn monitor 
  "middleware to monitor incoming requests and outgoing responses"
  [handler]
  (fn [{:keys [request-method uri query-string] :as request}]
    (let [response (handler request)]
      (debug request)
      (info request-method uri query-string)
      (let [{:keys [status body]} response]
        (info status body)
        (debug response)
        response))))

(defn wrap-exception 
  "catches every non-catched exceptions and wraps it into a 500 
   with a corresponding fault string"
  [f]
  (fn [request]
    (try (f request)
      (catch Exception e
        (let [stacktrace (with-out-str (st/print-stack-trace e))]
          {:status 500
           :body stacktrace})))))

(defn app-fn 
  "creates an app dynamically according to the current system"
  [component]
  ;; 'routes' bundles all routes which constitutes the API
  (-> (routes (db-routes-fn component)
              (csv-routes-fn component)
              (oracle-routes-fn component) 
              (transform-routes-fn component) 
              app-routes)
      wrap-params
      wrap-multipart-params
      (wrap-json-body {:keywords? true})
      (wrap-json-response {:pretty true})
      wrap-exception
      allow-content-type
      allow-origin
      wrap-dir-index
      wrap-file-info
      monitor
      ))
