(defproject cd-analyzer "0.1.0-SNAPSHOT"
  :description "FIXME: write"
  :dependencies [[org.clojure/clojure "1.3.0-alpha1"]
                 [org.clojure/clojure-contrib "1.2.0"]
		 [mysql/mysql-connector-java "5.1.12"]]
  :dev-dependencies [[leiningen/lein-swank "1.2.0-SNAPSHOT"]
		     [lein-run "1.0.0-SNAPSHOT"]
		     [com.stuartsierra/lazytest "1.0.2"]
                     [swank-clojure "1.3.0-SNAPSHOT"]]
  :main main)