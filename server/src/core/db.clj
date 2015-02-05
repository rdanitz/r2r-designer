(ns core.db
  "Core SQL database (PostgreSQL) functionality."
  (:require
    [com.stuartsierra.component :as c]
    [taoensso.timbre :as timbre]
    [clojure.java.io :as io]
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as str])
  (:import
    [java.sql DriverManager]
    [com.zaxxer.hikari HikariDataSource HikariConfig]))

(timbre/refer-timbre)

(defn new-pool 
  "creates a new Hikari connection pool"
  [c]
  (let [max-pool (:max-pool c)
        spec @(:spec c)
        ds (HikariDataSource.)]
    (doto ds 
      (.setMaximumPoolSize max-pool)
      (.setConnectionTestQuery "SELECT 1")
      (.setDataSourceClassName (:driver spec))
      (.addDataSourceProperty "serverName" (:host spec))
      (.addDataSourceProperty "databaseName" (:name spec))
      (.addDataSourceProperty "user" (:username spec))
      (.addDataSourceProperty "password" (:password spec)))
    ds))

(defn test-db 
  "tries to connect to a given database via spec"
  [spec]
  (let [host (:host spec)
        name (:name spec)
        user (:username spec)
        pass (:password spec)
        connection-str (str "jdbc:postgresql://" host "/" name)]
    (try 
      (let [conn (DriverManager/getConnection connection-str user pass)]
        (.close conn)
        true)
      (catch Exception e false))))

(defn register-db 
  "setse a new database configuration as active"
  [db new-spec]
  (info "registering new data source")
  (if db (c/stop db))
  (reset! (:spec db) new-spec)
  (c/start db))

(defn get-tables 
  "returns all db tables"
  [c]
  (let [ds {:datasource @(:pool c)}
        result (jdbc/query ds "SELECT table_name FROM INFORMATION_SCHEMA.TABLES WHERE table_schema = 'public'")
        tables (map :table_name result)]
    (debug tables)
    tables))

(defn get-columns
  "returns all columns for table"
  [c table]
  (let [ds {:datasource @(:pool c)}
        result (jdbc/query ds (str "SELECT column_name FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name = '" table "'"))
        columns (map :column_name result)]
    (debug columns)
    columns))

(defn get-table-columns 
  "returns a map with tables mapped to their columns"
  [c]
  (let [ds {:datasource @(:pool c)}
        tables (get-tables c)
        table-columns (apply merge (for [table tables] {table (get-columns c table)}))]
    table-columns))

(defn query-column 
  "returns a column"
  [c table column]
  (let [ds {:datasource @(:pool c)}
        result (jdbc/query ds (str "SELECT \"" column "\" FROM " table))
        values (map second (map first result))]
    (debug values)
    values))
