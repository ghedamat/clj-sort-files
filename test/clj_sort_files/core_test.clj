(ns clj-sort-files.core-test
  (:use clojure.test
        clj-sort-files.core))

(defn- get-current-directory []
  (. (java.io.File. ".") getCanonicalPath))

(deftest read-dir-test
  (testing "read-dir reads all files in a dir"
    (is (= 2 (count (read-dir "test/fixtures"))))))

(run-tests)
