(ns cd-analyzer.util)

(defmacro with-temp-dir [dir & body]
  `(when-let [temp-file# (java.io.File/createTempFile (.toString (java.util.UUID/randomUUID)) "")]
    (.delete temp-file#)
    (.mkdirs temp-file#)
    (when-let [temp-dir# (.getAbsolutePath temp-file#)]
      ((fn [~dir]
	 (let [res# (do ~@body)]
	   (clojure.contrib.io/delete-file-recursively temp-dir#)
	   res#)) temp-dir#))))

(def *reporting-on* true)

(defn report [& ss]
  (when *reporting-on* (print (apply str ss))))

(defn reportln [& ss]
  (when *reporting-on* (print (apply str ss)) (println)))

