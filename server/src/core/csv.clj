(ns core.csv
  "Core CSV functionality."
  (:require
    [com.stuartsierra.component :as c]
    [taoensso.timbre :as timbre]
    [clojure.java.io :as io]
    [clojure.data.csv :as csv]
    [clojure.string :as str])
  )

(timbre/refer-timbre)

(defn separator 
  "guesses (stupidly) the separator used in a csv file"
  [file]
  (let [first-line (re-find #".*[\n\r]" (slurp file))
        tabs (count (filter #(= \tab %) first-line))
        commata (count (filter #(= \, %) first-line))]
    (if (> tabs commata)
      \tab
      \,)))

(defn set-file 
  "sets a new CSV file and its separator"
  [c file]
  (reset! (:csv-file c) file)
  (reset! (:separator c) (separator file)))

(defn get-data 
  "retrieves the first 10 data items of the saved csv file"
  [c]
  (let [file @(:csv-file c)
        separator @(:separator c)]
    (if file
      (with-open [r (io/reader file)] (doall (take 10 (csv/read-csv r :separator separator))))
      [])))
