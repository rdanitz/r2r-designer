(ns core.oracle
  "naïve implementation of the Oracle RDF suggestion engine"
  (:require
    [clojure.data.json :as json]
    [clojure.string :refer [join split]]
    [clj-http.client :as client]
    [edu.ucdenver.ccp.kr.kb :refer :all]
    [edu.ucdenver.ccp.kr.rdf :refer :all]
    [edu.ucdenver.ccp.kr.sparql :refer :all]
    [edu.ucdenver.ccp.kr.sesame.kb :as sesame]
    [taoensso.timbre :as timbre]
    [core.db :as db]))

(timbre/refer-timbre)

(defn new-server 
  "returns a sesame server wrapping the given endpoint"
  [endpoint]
  (open 
    (sesame/new-sesame-server
      :server endpoint
      :repo-name "lov")))

(defn add-namespaces 
  "adds RDF namespaces to a knowledge base"
  [kb]
  (update-namespaces kb
   '(("rdf" "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
     ("rdfs" "http://www.w3.org/2000/01/rdf-schema#")
     ("dcterms" "http://purl.org/dc/terms/")
     ("skos" "http://www.w3.org/2004/02/skos/core#")
     )))

;; These are utility functions to retrieve several useful information about an RDF entity:

(defn get-label
  "tries to find a label for the RDF entity"
  [c entity]
  (let [kb (add-namespaces @(:kb c))]
    (let [result (sparql-query kb (format "select distinct * where { <%s> <http://www.w3.org/2000/01/rdf-schema#label> ?x. } limit 1" entity))]
      (map '?/x result))))

(defn get-comment 
  "tries to find a comment for the RDF entity"
  [c entity]
  (let [kb (add-namespaces @(:kb c))]
    (let [result (sparql-query kb (format "select distinct * where { <%s> <http://www.w3.org/2000/01/rdf-schema#comment> ?x. } limit 1" entity))]
      (map '?/x result))))

(defn get-title 
  "tries to find the title for the RDF entity"
  [c entity]
  (let [kb (add-namespaces @(:kb c))]
    (let [result (sparql-query kb (format "select distinct * where { <%s> <http://purl.org/dc/terms/title> ?x. } limit 1" entity))]
      (map '?/x result))))

(defn get-description 
  "tries to find a description for the RDF entity"
  [c entity]
  (let [kb (add-namespaces @(:kb c))]
    (let [result1 (sparql-query kb (format "select distinct * where { <%s> <http://purl.org/dc/terms/description> ?x. } limit 1" entity))
          entity2 (if (and entity (.endsWith entity "#")) (subs entity 0 (dec (count entity)))) 
          result2 (if entity2 (sparql-query kb (format "select distinct * where { <%s> <http://purl.org/dc/terms/description> ?x. } limit 1" entity2)) [])
          merged (concat result1 result2)]
      (map '?/x merged))))

(defn get-definition 
  "tries to find a definition for the RDF entity"
  [c entity]
  (let [kb (add-namespaces @(:kb c))]
    (let [result (sparql-query kb (format "select distinct * where { <%s> <http://www.w3.org/2004/02/skos/core#definition> ?x. } limit 1" entity))]
      (map '?/x result))))

(defn significant 
  "returns all hits with score > .4"
  [c results]
  (filter #(> (:score %) (:threshold c)) results))

(defn cut 
  "takes the first n matches"
  [c results]
  (take (:n c) results))

(defn shaped 
  "filters unneccesary information from the matches"
  [results]
  (let [filter-fn #(select-keys % [:score :uri :prefixedName :vocabulary.prefix])
        filtered (map filter-fn results)]
    filtered))

(defn transform 
  "adds a localName and the vocabulary URI to the matches"
  [entity-map]
  (let [prefixedName (first (:prefixedName entity-map))
        vocabPrefix (first (:vocabulary.prefix entity-map))
        localName (if (and prefixedName vocabPrefix) (subs prefixedName (inc (.length vocabPrefix))))
        uri (first (:uri entity-map))
        vocab (if (and uri localName) (subs uri 0 (- (.length uri) (.length localName))))]
    (assoc entity-map
           :localName (if localName [localName] [])
           :vocabulary.uri (if vocab [vocab] []))))

(defn enrich 
  "adds additional useful information like comments, descriptions, definition, etc."
  [c entity-map]
  (let [uri (first (:uri entity-map))
        vocab (first (:vocabulary.uri entity-map))]
    (assoc entity-map 
           :label (get-label c uri)
           :comment (get-comment c uri)
           :definition (get-definition c uri)
           :vocabulary.title (get-title c vocab)
           :vocabulary.description (get-description c vocab)
           )))

(defn process
  "combines all specified transformations (inner and outer transformation)"
  [c raw]
  (let [;; transformation on the whole result set
        outer (->> raw 
                  (significant c)
                  (cut c)
                  shaped)
        ;; transformation on individual matches
        inner (map (comp #(enrich c %) transform) outer)]
    inner))

(defn query-lov
  "queries the Linked Open Vocabulary search API; type is either 'class' or 'property'"
  [c type query]
  (if (and c query (seq query))
    (let [api "http://lov.okfn.org/dataset/lov/api/v2/search"
          results (-> (client/get api {:query-params {:q query :type type}}) :body json/read-json :results)]
      results)
    []))

(defn expr->items 
  "splits an expr to several items"
  [expr] 
  (filter seq (split expr #"[\s,;]+")))

(defn search-classes 
  "searches for classes in LOV"
  [c expr]
  (process c (apply concat (map #(query-lov c "class" %) (expr->items expr)))))

(defn search-properties 
  "searches for properties in LOV"
  [c expr]
  (process c (apply concat (map #(query-lov c "property" %) (expr->items expr)))))

(defn item-or-tag 
  "preferrably returns a tag for a given table, if existing;
   otherwise returns the name of the table"
  [item tag] (if (empty? tag) item tag))

(defn recommend 
  "returns suggestions for a given table and column;
   database has to be configured"
  [c table columns]
  (let [t-rec {:name (:name table) 
               :recommend (search-classes c (item-or-tag (:name table) (:tag table)))}
        c-rec (for [i columns] 
                {:name (:name i) 
                 :recommend (search-properties c (item-or-tag (:name i) (:tag i)))})]
    {:table t-rec :columns c-rec}))
