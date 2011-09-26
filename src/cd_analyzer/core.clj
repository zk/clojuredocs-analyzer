(ns cd-analyzer.core
  (:use [cd-analyzer.util] 
	[cd-analyzer.language]
	[cd-analyzer.database]
	[clojure.pprint :only (pprint)])
  (:import [java.io File FileReader]
	   [clojure.lang LineNumberingPushbackReader]))


(defn indt
  ([l] (apply str (take l (repeat " "))))
  ([l i] (indt (+ l i))))

(defn to-var-map [v]
  (let [{:keys [name ns doc line file arglists added]} (meta v)]
    {:name (str name)
     :ns (str ns)
     :doc (remove-leading-whitespace doc)
     :line line
     :file file
     :added added
     :source (source-for v)
     :arglists arglists
     :vars-in (map #(let [meta (meta %)]
		      {:ns (str (:ns meta))
		       :name (str (:name meta))}) (vars-in v))}))

(defn parse-ns-map [root-path]
  (fn [file]
    (when-let [ns (file-to-ns file)]
      (let [m (meta ns)]
	{:name (.name ns)
	 :file file
	 :doc (:doc m)
	 :web-path (.replace (.getAbsolutePath file) (.getAbsolutePath root-path) "")
	 :vars (map to-var-map (ns-to-vars ns))}))))


(defn parse-lein [#^File file]
  (let [rdr (LineNumberingPushbackReader. (FileReader. file))
	lein (try (read rdr) (catch Exception e (throw (Exception. (str "parse-lein: " e)))))
	props (reduce #(assoc %1 (first %2) (second %2)) {} (partition 2 (drop 3 lein)))
	autodoc (-> (:autodoc props)
		    (dissoc :root)
		    (dissoc :source-path)
		    (dissoc :output-path))]
    (merge {:name (str (second lein))
	    :version (nth lein 2)
	    :description (:description props)
	    :dependencies (:dependencies props)
	    :source-root (mkfile (parent-file file) "src")}
	   autodoc)))

(defn parse-git [#^File git-root]
  {:web-src-dir (git-dir-to-web-src-dir git-root)
   :commit (git-dir-to-commit git-root)
   :site-url (git-dir-to-site-url git-root)})


(defn parse-project [#^File project-file]
  (let [project (parse-lein project-file)
	project (assoc project :cljs (cljs-in (:source-root project)))
	project (assoc project :namespaces (map (parse-ns-map (parent-file project-file)) (:cljs project)))]
    project))

(defn parse-library [#^File root]
     (let [git-map (parse-git (mkfile root ".git"))
	   lein-map (parse-lein (mkfile root "project.clj"))
	   project-cljs (filter #(= "project.clj" (.getName %)) (file-seq root))
	   projects (map parse-project project-cljs)
	   library (-> (merge git-map lein-map)
		       (dissoc :dependencies))]
       (assoc library :projects projects)))

(defn parse-clojure-core [#^File root]
  (let [git-map (parse-git (mkfile root ".git"))
	clojure-map {:name "Clojure Core"
		     :description "Clojure core environment and runtime library."
		     :site-url "http://clojure.org"
		     :copyright "&copy Rich Hickey.  All rights reserved."
		     :license "<a href=\"http://www.eclipse.org/legal/epl-v10.html\">Eclipse Public License 1.0</a>"
		     :version "1.3.0"
		     :source-root (mkfile root "src" "clj")}
	project clojure-map
	project (assoc project :cljs (cljs-in (:source-root project)))
	project (assoc project :namespaces (map (parse-ns-map root) (:cljs project)))
	library (merge {:projects [project]} git-map clojure-map)]
    library))

(defn parse-clojure-contrib [#^File root]
  (let [git-map (parse-git (mkfile root ".git"))
	cc-map {:name "Clojure Contrib"
			:description "The user contributions library, clojure.contrib, is a collection of namespaces each of which implements features that we believe may be useful to a large part of the Clojure community."
			:site-url "http://richhickey.github.com/clojure-contrib/"
			:copyright "&copy Rich Hickey.  All rights reserved."
			:license "<a href=\"http://www.eclipse.org/legal/epl-v10.html\">Eclipse Public License 1.0</a>"
			:version "1.2.0"
			:source-root (mkfile root "src" "main" "clojure")}
	project cc-map
	project (assoc project :cljs (cljs-in (:source-root project)))
	project (assoc project :namespaces (map (parse-ns-map root) (:cljs project)))
	library (merge {:projects [project]} git-map cc-map)]
    library))

(defn get-projects [lib]
  (:projects lib))

(defn get-nss [lib]
  (reduce concat (map :namespaces (:projects lib))))

(defn get-vars [lib]
  (->> lib
       (:projects)
       (map :namespaces)
       (reduce concat)
       (map :vars)
       (reduce concat)))

(defn pad [width thing]
  (apply str (take (- width (count (str thing))) (repeat " "))))

(def *pad-width* 70)

(defn update-ns [i ns-map]
  (let [indt (partial indt i)]
    (reportln (indt) (:name ns-map) (pad *pad-width* (:name ns-map)) "(ns)")))

(defn update-project [i project]
  (let [indt (partial indt i)]
    (reportln (indt) (:name project) (pad *pad-width* (:name project)) "(project)")
    (doseq [ns (:namespaces project)]
      (update-ns (+ i 2) ns))
      (reportln)))

(defn update-library [i library]
  (let [indt (partial indt i)]
    (reportln (indt) "Updating Library: " (:name library))
    (doseq [project (:projects library)]
      (update-project (+ i 2) project))))

(defn report-on-lib [library]
  (let [start (System/currentTimeMillis)]
    (try
     (reportln)
     (reportln (:name library) " :: Import Library Task")
     (reportln "=========================================")
     (let [col-width 55
	   pad (partial pad col-width)
	   indt (partial indt 2)
	   projects (get-projects library)
	   num-projects (count projects)
	   nss (get-nss library)
	   num-nss (count nss)
	   vars (get-vars library)
	   num-vars (count vars)]
       (reportln (indt) num-projects " projects, " num-nss " namespaces, " num-vars " vars.")
       (reportln)
       (store-lib library)
       (doseq [p (sort-by :name projects)]
	 (reportln (indt) (:name p) (pad (:name p)) "(project)")
         (doseq [ns (sort-by :name (:namespaces p))]
           (reportln (indt) (:name ns) (pad (:name ns)) "(ns)")
           (store-ns-map library ns)
           (doseq [v (sort-by :name (:vars ns))]
             (reportln (indt 4) (:ns v) "/" (:name v) (pad (:name v)) "(var)")
             (store-var-map library ns v))))
       (reportln))
     (catch Exception e
       (reportln "=========================================")
       (reportln "Import process failed: " e)))
    (reportln (indt 2) "Took " (/ (- (System/currentTimeMillis) start) 1000.0) "s")))

(comment
  (doseq [ns (sort-by :name nss)]
    (report (indt) (:name ns) (pad (:name ns)) "(ns)")
    (if (store-ns-map ns)
      (reportln " Ok")
      (reportln " Error")))
  (reportln)
  (doseq [v (sort-by :name vars)]
    (report (indt) (:name v) (pad (:name v)) "(var)")
    (if ((store-var-map (:name library) (:version library)) v)
      (reportln " Ok")
      (reportln " Error")))
  (reportln)
  (doseq [v (sort-by :name vars)]
    (let [v-to-vs-str (str (:name v))]
      (report (indt) v-to-vs-str (pad v-to-vs-str) "(" (count (:vars-in v)) " references)"))
    (if (store-var-references v)
      (reportln " Ok")
      (reportln " Error")))
  (reportln)
  (reportln (indt) "Looking for vars to remove...")
  (if (= 0 num-vars)
    (reportln (indt) "No vars found, skipping removal of stale vars.")
    (let [removed (remove-stale-vars (:name library) start)]
      (if (= 0 (count removed))
        (reportln (indt 2) "No vars removed.")
        (do
          (reportln (indt 2) "Removed " (count removed) " vars:")
          (doseq [vr removed]
            (reportln (indt 4) (:name vr) (pad (:name vr)) "(" (:ns vr) ")"))))))
  (reportln "=========================================")
  (reportln (indt) num-projects " projects, " num-nss " namespaces, " num-vars " vars found in " (:name library)))

(defn run-update [root-dir]
  (report-on-lib (parse-library (File. root-dir))))

(defn run-update-clojure-core [root-dir]
  (report-on-lib (parse-clojure-core (File. root-dir))))

(defn run-update-clojure-contrib [root-dir]
  (report-on-lib (parse-clojure-contrib (File. root-dir))))

#_(run-update-clojure-core "/home/zkim/clojure")
#_(run-update-clojure-contrib "/Users/zkim/clojurelibs/clojure-contrib")
#_(pprint (parse-clojure-core (File. "/Users/zkim/clojurelibs/clojure")))
