(ns routes
  "The 'routes' namespaces defines the backend API of the r2r-designer.
   You can find the specification within /server/resource/public/api-docs.raml."
  (:require 
    [ring.util.response :refer [response]]))

(defn preflight 
  "Implements Cross-Origin-Resource-Sharing by adding a corresponding header."
  [request]
  (assoc
    (response "CORS enabled")
    :headers {"Access-Control-Allow-Origin" "*" 
              "Access-Control-Allow-Methods" "PUT, DELETE, POST, GET, OPTIONS, XMODIFY" 
              "Access-Control-Max-Age" "2520"
              "Access-Control-Allow-Credentials" "true"
              "Access-Control-Request-Headers" "x-requested-with, content-type, origin, accept"
              "Access-Control-Allow-Content-Type" "*" 
              "Access-Control-Allow-Headers" "x-requested-with, content-type, origin, accept"}))
