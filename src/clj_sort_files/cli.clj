(ns clj-sort-files.cli
  (:require [clojure.string :as string])
  (:gen-class))

(def cli-options
  [;; First three strings describe a short-option, long-option with optional
   ;; example argument description, and a description. All three are optional
   ;; and positional.
   ["-s" "--sort-order DIR" "Sorting direction"
    :default :desc
    :parse-fn #(keyword %)
    :validate [#(some #{%} [:asc :desc]) "Must be \"asc\" or \"desc\""]]
   ["-f" "--sort-field FILED" "define ordering field [name, updated]"
    :parse-fn #(keyword %)
    :validate [#(some #{%} [:name :updated]) "Must be \"name\" or \"updated\""]]
   ["-l" "--sort-limit N" "Limit results per path"
    :parse-fn #(Integer/parseInt %)]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["clj-sort-files"
        ""
        "Usage: clj-sort-files [options] path1 path2 ..."
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))
