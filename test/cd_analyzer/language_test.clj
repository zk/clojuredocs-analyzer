(ns cd-analyzer.language-test
  (:use [cd-analyzer.language] :reload-all)
  (:use [lazytest describe expect]
	[clojure.contrib.pprint])
  (:require [ring.adapter.httpcore])
  (:import [java.io File]))

(def resources (mkfile "." "resources"))

(def clojure-git-dir (mkfile resources "clojure-git"))
(def empty-git-dir (mkfile resources "empty-git"))
(def clojure-commit "77be185a6ca00a338a6647462e14206bad0e9802")

(def cljs-in-dir (mkfile resources "cljs-in"))
(def empty-dir (mkfile resources "empty-dir"))

(def ns-to-vars-test-data [['map #'clojure.core/map]
			   ['reduce #'clojure.core/reduce]
			   ['ring.adapter.httpcore.proxy$java.lang.Object$ContentProducer$2f493dd0 
			    #'ring.adapter.httpcore/ring.adapter.httpcore.proxy$java.lang.Object$ContentProducer$2f493dd0]])

(def mocked-ns-vars
     (binding [ns-interns (fn [_] ns-to-vars-test-data)]
       (ns-to-vars (find-ns 'clojure.core))))

(describe mkfile
  (given "from strings" [foo-bar (mkfile "foo" "bar")]
	 (it "returns a file instance"
	   (instance? java.io.File foo-bar))
	 (it "has the correct path"
	   (= "foo/bar" (.getPath foo-bar))))
  (given "from strings and files" [from-file-and-string (mkfile (mkfile "foo" "bar") "baz")]
	 (it "returns a file instance"
	   (instance? java.io.File from-file-and-string))
	 (it "has the correct path"
	   (= "foo/bar/baz" (.getPath from-file-and-string)))))

(describe git-dir-to-site-url
  (it "works with a valid git dir"
    (= "http://github.com/clojure/clojure" (git-dir-to-site-url (mkfile resources "clojure-git"))))
  (it "returns nil with an invalid git dir"
    (= nil (git-dir-to-site-url (mkfile resources "empty-git")))))

(describe git-dir-to-web-src-dir
  (it "works with a valid git dir"
    (= "http://github.com/clojure/clojure/blob" (git-dir-to-web-src-dir (mkfile resources "clojure-git"))))
  (it "returns nil with an invalid git dir"
    (= nil (git-dir-to-web-src-dir (mkfile resources "empty-git")))))

(describe git-dir-to-commit
  (it "works with a valid git dir"
    (= clojure-commit (git-dir-to-commit clojure-git-dir)))
  (do-it "returns nil for invlid dirs"
    (expect (= nil (git-dir-to-commit empty-git-dir)))
    (expect (= nil (git-dir-to-commit resources)))
    (expect (= nil (git-dir-to-commit (mkfile "no-exist"))))))

(describe cljs-in
  (do-it "finds clojure files"
    (expect (= 3 (count (cljs-in cljs-in-dir))))
    (expect (= 0 (count (cljs-in empty-dir))))))

(describe file-to-ns-str
  (it "works when the ns decl is the first form"
    (= "hello.world" (file-to-ns-str (mkfile resources "file-to-ns-str" "easy.clj"))))
  (it "works when the ns decl is not the first form"
    (= "comment.before.ns" (file-to-ns-str (mkfile resources "file-to-ns-str" "comment-before-ns.clj")))))

(describe ns-to-vars
  (it "correctly filters 'unknown' vars"
    (= 2 (count mocked-ns-vars)))
  (it "correctly transforms vars to var-maps"
    (= 'map (:name (meta (first mocked-ns-vars)))))
  (it "throws an exception on a nil parameter"
    (try (ns-to-vars nil) false (catch Exception e true))))

(describe symbols-in
  (it "finds 35 symbols in 'map'"
    (= 35 (count (symbols-in map))))
  (it "returns nil for nil input"
    (= nil (symbols-in nil))))

(describe vars-in
  (it "finds 18 symbols in map"
    (= 18 (count (vars-in map))))
  (it "reutrns empty for nil input"
    (= '() (vars-in nil)))
  (it "filters passed var from vars in"
    (not (.contains (vars-in #'clojure.core/map) #'clojure.core/map))))