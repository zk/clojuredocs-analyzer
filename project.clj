(defproject cd-analyzer "0.1.0-SNAPSHOT"
  :description "FIXME: write"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.0.6"]
		 [mysql/mysql-connector-java "5.1.12"]
		 [org.slf4j/slf4j-api "1.6.1"]
		 [ch.qos.logback/logback-classic "0.9.24"]
		 [clj-stacktrace"0.1.3"]
                 [hiccup "0.3.6"]]
  :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]]
  :main main)
