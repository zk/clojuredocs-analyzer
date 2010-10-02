(ns cd-analyzer.runner
  (:use [cd-analyzer.util]
	[cd-analyzer.language]
	[cd-analyzer.core]
	[clojure.contrib.pprint]
	[clojure.java.shell]
	[clojure.contrib.except :only (throwf)]))


(defn clone-target-repo [git-url download-to-dir opts]
  (let [local-repo-path (str 
			 download-to-dir
			 "/"
			 (second (re-find #".*\/([^/]*)\.git" git-url)))]
    #_(sh (str "rm -rf " local-repo-path) ".")
    #_(swank.core/break)
    (let [cmd-res (sh "git" "clone" git-url :dir download-to-dir)
	  res (str (:out cmd-res) " " (:err cmd-res))]
      (cond (> (count (re-find #"git-upload-pack not found" res)) 0) (println "Error: " res)
	    (> (count (re-find #"already exists and is not an empty directory." res)) 0) (println "Error: " res)
	    :else (do
                    (sh "git" "checkout" (str "origin/" (get opts :branch "master")) "-b" (get opts :branch "master"))
                    (println (str "Clone of " git-url " successful."))
                    local-repo-path)))))

(defn gen-dep-vec [project-map]
  `[~(symbol (:name project-map)) ~(:version project-map)])

(defn gen-project-def [project-map]
  (let [name-sym (:name project-map)
	version (:version project-map)]
    `(~(symbol "defproject") 
      ~(symbol "cd-temp")
      "0.1.0-SNAPSHOT"
      :dependencies [[~(symbol "org.clojure/clojure") "1.2.0-RC1"]
		     [~(symbol "org.clojure/clojure-contrib") "1.2.0-RC1"]
		     [~(symbol "cd-analyzer") "0.1.0-SNAPSHOT"]
		     [~(symbol (str name-sym)) ~version]]
      :dev-dependencies [[~(symbol "lein-run") "1.0.0-SNAPSHOT"]])))

(defn write-temp-project-file [project-root temp-dir]
  (spit (str temp-dir "/project.clj") (gen-project-def
				       (parse-project (mkfile project-root "project.clj")))))


(defn run-update-for [git-url & opts]
  (let [opts (merge {:branch "master"} (apply (partial assoc {}) opts)) 
        start (System/currentTimeMillis)]
    #_ (when (not (.exists (java.io.File. proj-root))) (throwf "Couldn't find project root: %s" proj-root))
    (reportln (str "Running update for " git-url " on " (java.util.Date.)))
    (reportln "-----------------------------------------")
    (with-temp-dir tmp-dir
      (report "Cloning repo... ")
      (let [repo-dir (clone-target-repo git-url tmp-dir opts)
	    tmp-proj-dir (.getAbsolutePath (java.io.File. (str tmp-dir "/temp-proj")))]
	(.mkdirs (java.io.File. tmp-proj-dir))
	(reportln "Done.")
	(write-temp-project-file repo-dir tmp-proj-dir)
	(reportln "Building / Installing")
	(let [res (sh "lein" "install" :dir repo-dir)]
	  (reportln (:out res))
	  (reportln (:err res)))
	(reportln)
	(reportln "Updating Dependencies.")
	(let [res (sh "lein" "deps" :dir tmp-proj-dir)]
	  (reportln (:out res))
	  (reportln (:err res)))
	(reportln)
	(reportln "Running import process.")
	(let [res (sh "lein" "run" "cd-analyzer.core" "run-update" repo-dir :dir tmp-proj-dir)]
	  (reportln (:out res))
	  (reportln (:err res)))))
    (reportln)
    (reportln "-----------------------------------------")
    (reportln "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    (reportln)))

(defn run-update-and-log [path git-url]
  (reportln "Running update " path " " git-url)
  (let [file (java.io.File. path)]
    (when (not (.exists file))
      (.createNewFile file))
    (binding [*out* (java.io.FileWriter. file true)]
      (run-update-for git-url)))
  (reportln "Done."))


