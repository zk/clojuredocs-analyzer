(ns cd-analyzer.language
  (:use [clojure.contrib.pprint]
	[clojure.repl :only (source-fn)]
	[clojure.string :only (split-lines trim)])
  (:import [clojure.lang LineNumberingPushbackReader]
	   [java.io File FileReader StringReader]))

(defn mkfile [& parts]
  (->> parts
       (map #(if (instance? File)
	       (.getAbsolutePath %)
	       %))
       (interpose "/")
       (apply str)
       (File.)))

(defn file-exists? [#^File file]
  (.exists file))

(defn dirs-in [dir]
  (let [dir-file (java.io.File. dir)]
    (map #(.getAbsolutePath %) (filter #(.isDirectory %) (seq (.listFiles dir-file))))))

(defn files-in [dir]
  (let [dir-file (java.io.File. dir)]
    (map #(.getAbsolutePath %) (filter #(.isFile %) (seq (.listFiles dir-file))))))

(defn file-name [path]
  (.getName (java.io.File. path)))

(defn parent-file [file]
  (.getParentFile file))

(defn git-dir-to-site-url [#^File git-dir]
  (when-let [config (try (split-lines (slurp (mkfile git-dir "config"))) (catch Exception e nil))]
    (when-let [remote-origin (drop 1 (drop-while #(not (.contains % "[remote \"origin\"]")) config))]
      (let [url (->> (re-find #"http://.*\.git" (second remote-origin))
		     (reverse)
		     (drop 4)
		     (reverse)
		     (apply str))]
	(when (.contains url "github")
	  url)))))

(defn git-dir-to-web-src-dir [#^File git-dir]
  (when-let [url (git-dir-to-site-url git-dir)]
    (str url "/blob")))

(defn git-dir-to-commit [#^File git-dir]
  (let [master (mkfile git-dir "refs" "heads" "master")]
    (when (file-exists? master)
      (trim (slurp master)))))

(defn cljs-in [file]
  (when (and (not (nil? file)) (not= "" file))
    (filter #(re-matches #".*\.clj" (.getName %)) (file-seq file))))

(defn file-to-ns-str [#^File f]
  (let [rdr (LineNumberingPushbackReader. (FileReader. f))]
    (loop [r rdr]
      (when-let [rep (try (read rdr) (catch Exception e (println "EOF encountered: " f) nil))]
	(if (= (str (first rep)) "ns")
	  (str (second rep))
	  (recur rdr))))))

(defn file-to-ns [#^File f]
  (when-let [ns-str (file-to-ns-str f)]
    (try 
     (require (symbol ns-str))
     (find-ns (symbol ns-str))
     (catch Exception e (println (str "Warning: couldn't resolve ns " ns-str))))))

(defn ns-to-vars [ns]
  (if (nil? ns)
    (throw (Exception. "ns-to-vars: ns parameter is nil"))
    (->> (ns-interns ns)
	 (map second)
	 (filter #(not (:private (meta %))))
	 (filter #(try 
		   ;; hack to prevent errors when import unknown (to me) var types
		   ;; ex. unquote-splicing, unquote, *depth*, *state*, *sb*
		   (not (class? (var-get %)))
		   (catch Exception e true))))))

(defn symbols-from-reader-rep [list]
  (defn recur-symbols [list-rep]
    (let [symbols (filter symbol? list-rep)
	  seqs (filter seq? list-rep)]
      (flatten (cons symbols (map recur-symbols seqs)))))
  (sort (set (recur-symbols list))))

(defn source-for [v] 
  (try 
   (source-fn (symbol (str (:ns (meta v))) (str (:name (meta v)))))
   (catch Exception e nil)))

(defn symbols-in [v]
  (defn recur-symbols [list-rep]
    (let [symbols (filter symbol? list-rep)
	  seqs (filter seq? list-rep)]
      (flatten (cons symbols (map recur-symbols seqs)))))
  (when-let [src (source-for v)]
    (when-let [rdr (try
		    (LineNumberingPushbackReader. (StringReader. src))
		    (catch Exception e nil))]
      (when-let [rep (try (read rdr) (catch Exception e (throw (Exception. (str "symbols-in: " e)))))]
	(filter identity (sort (set (recur-symbols rep))))))))

(defn vars-in [v]
  (let [symbols (symbols-in v)
	resolved (map #(try (resolve %) (catch Exception e nil)) symbols)]
    (->> resolved
	 (filter identity)
	 (filter #(not (:private (meta %))))
	 (remove #(= % v)))))



