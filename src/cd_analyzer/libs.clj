(ns cd-analyzer.libs
  (:use [cd-analyzer.core]
	[cd-analyzer.runner]
	[cd-analyzer.database]))

;;
;; Scratch file, ignore.
;;

#_(time 
   (do     
     (run-update-clojure-core "/Users/zkim/clojurelibs/clojure")
     (run-update-clojure-contrib "/Users/zkim/clojurelibs/clojure-contrib")
     (run-update "/Users/zkim/clojurelibs/leiningen")
     (run-update "/Users/zkim/clojurelibs/swank-clojure")
     (run-update "/Users/zkim/clojurelibs/clj-ssh")
     (run-update "/Users/zkim/clojurelibs/pallet")
     (run-update "/Users/zkim/clojurelibs/enlive")
     (run-update "/Users/zkim/clojurelibs/circumspec")
     (run-update "/Users/zkim/clojurelibs/Midje")
     (run-update "/Users/zkim/clojurelibs/ring")
     (run-update "/Users/zkim/clojurelibs/incanter")
     (run-update "/Users/zkim/clojurelibs/trammel")))

(run-update "/Users/zkim/clojurelibs/ring")
#_(run-update-and-log "./logs/enlive-import.log" "http://github.com/cgrand/enlive.git")


#_(binding [*db* {:classname "com.mysql.jdbc.Driver"
                  :subprotocol "mysql"
                  :subname "//localhost:4444/clojuredocs?user=root&password=gammaClojureDocs1024"
                  :create true
                  :username "root"
                  :password "gammaClojureDocs1024"}]
    (run-update-clojure-core "/Users/zkim/clojurelibs/clojure"))

#_ (binding [*db* {:classname "com.mysql.jdbc.Driver"
                   :subprotocol "mysql"
                   :subname "//localhost:4444/clojuredocs?user=root&password=gammaClojureDocs1024"
                   :create true
                   :username "root"
                   :password "gammaClojureDocs1024"}]
     (run-update-clojure-contrib "/Users/zkim/clojurelibs/clojure-contrib"))



