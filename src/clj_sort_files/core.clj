(ns clj-sort-files.core
  (:require [clojure.data.json :as json]
            [clojure.tools.cli :refer [parse-opts]])
  (:use [clj-sort-files.cli])
  (:gen-class))

(defn- reverse-cmp [a b]
  (compare b a))

;; taken from https://groups.google.com/forum/#!topic/clojure-dev/9ctJC-LXNps
(defn- apply-kw
  "Like apply, but f takes keyword arguments and the last argument is
  not a seq but a map with the arguments for f"
  [f & args]
  {:pre [(map? (last args))]}
  (apply f (apply concat
                  (butlast args) (last args))))

(defn read-dir
  "Return a seq of the files contained in a directory"
  [path]
  (let [dir (clojure.java.io/file path)]
    (if (.exists dir)
      (file-seq dir)
      (throw (ex-info "Path not found" {:cause :path-not-found :path path})))))

(defn file-hash-map
  "Returns an hash-map from a Java File object"
  [file]
  {:path (.getPath file)
   :name (.getName file)
   :is-file (.isFile file)
   :updated (.lastModified file)})

(defn sort-files
  "Returns a sorted sequence of file-hash-maps,
  where the sort field is one of the keys.
  Sort order is descending by default"
  ([files field]
   (sort-files files field reverse-cmp))
  ([files field comparatorfn]
   (sort-by field comparatorfn
         (map (fn [file] (file-hash-map file))
              files))))

(defn sort-paths
  "Returns an array of hashes (one per path)
  where the key is the path and the value is a sorted array
  of the contained files"
  [paths & options]
  (let [{:keys [sort-field sort-order sort-limit]
         :or {sort-field :updated sort-order :desc}} options
        cmp (if (= sort-order :asc)
              compare
              reverse-cmp)]
    (map (fn [path]
           (let [files
                 (if sort-limit
                   (take sort-limit (sort-files (read-dir path) sort-field cmp))
                   (sort-files (read-dir path) sort-field cmp))]
             {path files}))
         paths)))



(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (cond
      (:help options) (exit 0 (usage summary))
      (< (count arguments) 1) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors)))
    ;; Execute program with options
    (if (seq arguments)
      (try
        (json/pprint (apply-kw sort-paths arguments options)
                   :escape-slash false)
        (catch clojure.lang.ExceptionInfo e
          (if (= :path-not-found (-> e ex-data :cause))
            (exit 1 (str "Directory not found: " (-> e ex-data :path)))
            (throw e)))
        )

      (exit 1 (usage summary)))))



