(ns cd-analyzer.util
  (:use [clojure.java.io :only (delete-file file)]))

(defn delete-file-recursively
  "Delete file f. If it's a directory, recursively delete all its contents.
Raise an exception if any deletion fails unless silently is true."
  [f & [silently]]
  (let [f (file f)]
    (if (.isDirectory f)
      (doseq [child (.listFiles f)]
        (delete-file-recursively child silently)))
        (delete-file f silently)))

(defmacro with-temp-dir [dir & body]
  `(when-let [temp-file# (java.io.File/createTempFile (.toString (java.util.UUID/randomUUID)) "")]
    (.delete temp-file#)
    (.mkdirs temp-file#)
    (when-let [temp-dir# (.getAbsolutePath temp-file#)]
      ((fn [~dir]
	 (let [res# (do ~@body)]
	   (delete-file-recursively temp-dir#)
	   res#)) temp-dir#))))

(def *reporting-on* true)

(defn report [& ss]
  (when *reporting-on* (print (apply str ss))))

(defn reportln [& ss]
  (when *reporting-on* (print (apply str ss)) (println)))

