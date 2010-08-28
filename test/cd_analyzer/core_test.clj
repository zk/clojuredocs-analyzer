(ns cd-analyzer.core-test
  (:use [cd-analyzer.core])
  (:use [lazytest.describe]
	[lazytest.expect]))


(describe to-var-map
  (given "a var-map of 'reduce'" [{:keys [name ns doc line file added 
					  source arglists vars-in]} 
				  (to-var-map #'clojure.core/reduce)]
	 (do-it "gives the correct values"
	   (expect (= "reduce" name))
	   (expect (= "clojure.core" ns))
	   (expect (> (count doc) 0))
	   (expect (= 773 line))
	   (expect (= "1.0"))
	   (expect (= "clojure/core.clj" file))
	   (expect (> (count source) 0))
	   (expect (> (count arglists) 0))
	   (expect (> (count vars-in) 0)))))

(comment
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
		       :name (str (:name meta))}) (vars-in v))})