(ns clj-sort-files.core
  (:require [clojure.data.json :as json]
            [clojure.tools.cli :refer [parse-opts]])
  (:use [clj-sort-files.cli])
  (:gen-class))


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
  where the sort field is one of the keys."
  [files field direction]
  (->> files
       (map file-hash-map)
       (sort-by field)
       direction))

(comment
  (defn sort-paths
    "Returns an array of hashes (one per path)
    where the key is the path and the value is a sorted array
    of the contained files"
    [paths {:keys [sort-field sort-order sort-limit]
            :or {sort-field :updated sort-order :desc}}]
    (->> paths
         (map (juxt identity read-dir))
         (map (fn [[path files]]
                {path (take (or sort-limit (count files))
                      (sort-files files
                                  sort-field
                                  (if (= sort-order :asc) identity reverse)))}))))
  )

(defn sort-path
  "Returns an array of hashes (one per path)
  where the key is the path and the value is a sorted array
  of the contained files"
  [path {:keys [sort-field sort-order sort-limit]
         :or {sort-field :updated sort-order :desc}}]
  (let [files (read-dir path)]
    {path (take (or sort-limit (count files))
                (sort-files files
                            sort-field
                            (if (= sort-order :asc) identity reverse)))}))


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
        (json/pprint (map #(sort-path % options) arguments)
                   :escape-slash false)
        (catch clojure.lang.ExceptionInfo e
          (if (= :path-not-found (-> e ex-data :cause))
            (exit 1 (str "Directory not found: " (-> e ex-data :path)))
            (throw e))))

      (exit 1 (usage summary)))))
