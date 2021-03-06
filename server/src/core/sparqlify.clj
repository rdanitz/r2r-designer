(ns core.sparqlify
  "This is a clean wrapper around the Sparqlify functions, that are needed.
   Essentially, these are (dumber) re-implementations of the specific Main methods."
  (:require
    [com.stuartsierra.component :as c]
    [clojure.java.shell :as sh]
    [clojure.java.io :as io]
    [taoensso.timbre :as timbre])
  (:import 
    java.io.File
    org.apache.commons.cli.GnuParser
    org.apache.commons.cli.Options
    org.apache.jena.riot.out.NTriplesWriter
    org.aksw.jena_sparql_api.core.utils.QueryExecutionUtils
    ;; org.aksw.sparqlify.core.RdfViewSystemOld
    org.aksw.sparqlify.util.SparqlifyUtils
    org.aksw.sparqlify.web.SparqlifyCliHelper
    org.aksw.sparqlify.web.SparqlFormatterUtils
    org.aksw.sparqlify.csv.CsvParserConfig
    org.aksw.sparqlify.csv.CsvMapperCliMain
    org.aksw.sparqlify.csv.InputSupplierCSVReader 
    ))

(timbre/refer-timbre)

(defn mapping-to-file 
  "writes the mapping as string to a temporary file"
  [mapping]
  (let [mapping-file (if mapping (File/createTempFile "mapping" ".sml"))] 
    (when mapping-file 
      (spit mapping-file mapping))
    mapping-file))

;; needed?
;; (defn init-sparqlify! []
;;   (RdfViewSystemOld/initSparqlifyFunctions))

;; pseudo-parse pseudo-options into config [...]
(defn mapping-to-config 
  "pretends to be called as from the command line;
   returns a CLI config object"
  [mapping-file]
  (let [options (doto (Options.) (.addOption "m" "mapping" true ""))
        parser (GnuParser.)
        cmd (.parse parser options (into-array String ["-m" mapping-file]))
        config (SparqlifyCliHelper/parseSmlConfigs cmd nil)]
    config))

;; sadly cannot easily kill sub-processes when invoking jar's through shell but ... common, seriously guys?
;; rough rewrite of sparqlify-core's main; most of the config is not needed? some vars in main are never used
(defn config-sparqlify
  "returns a configured Sparqlify engine"
  [c mapping-file]
  ;; (init-sparqlify!)
  (let [pool @(:pool (:datasource c))
        config (mapping-to-config mapping-file)]
    (SparqlifyUtils/createDefaultSparqlifyEngine pool config nil nil)))

(defn start-sparql-endpoint! 
  "exposes a Sparqlify endpoint for a given mapping"
  [sparqlify mapping-file]
  (let [port (:port sparqlify)
        qef (config-sparqlify sparqlify mapping-file)
        server (org.aksw.sparqlify.web.Main/createSparqlEndpoint qef port)]
    (info "publishing new SPARQL endpoint")
    (if sparqlify (c/stop sparqlify))
    (reset! (:server sparqlify) server)
    (c/start sparqlify)
    (str (:host sparqlify) ":" (:port sparqlify) "/sparql")))

(defn sparqlify-dump 
  "dumps the Sparqlify endpoint regarding the given mapping;
   returns a temp file containing the dump as N3 triples."
  [c mapping-file]
  (let [qef (config-sparqlify c mapping-file)
        it (QueryExecutionUtils/createIteratorDumpTriples qef)
        f (File/createTempFile "dump" ".nt")]
    (with-open [out (io/output-stream f)] 
      (NTriplesWriter/write out it))
    f))

(defn sparqlify-csv 
  "wrapper to sparqlify-csv command line tool;
   returns a temp file containing the dump as N3 triples."
  [c mapping-file]
  (let [csv-file @(:csv-file (:datasource c))]
    (with-open [in (io/input-stream mapping-file)]
      (let [template-config (CsvMapperCliMain/readTemplateConfig in nil)
            view (first (.getDefinitions template-config)) ; pick only(?) view
            f (File/createTempFile "dump" ".nt")
            csv-config (doto (CsvParserConfig.) (.setFieldSeparator @(:separator (:datasource c))))
            csv-reader (InputSupplierCSVReader. csv-file csv-config)
            results (CsvMapperCliMain/createResultSetFromCsv csv-reader true (int 100))
            it (CsvMapperCliMain/createTripleIterator results view)]
        (with-open [out (io/output-stream f)]
          (SparqlFormatterUtils/writeText out it))
        f))))
