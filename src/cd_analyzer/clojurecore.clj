(ns cd-analyzer.clojurecore
  (:use [hiccup.core]
	[cd-analyzer.core]
	[clojure.contrib.sql]
	[clojure.pprint :only (pprint)])
  (:require [clojure.zip :as zip]))

(declare db)
(def spheres [{:name "Simple Values"
	       :categories [{:name "Numbers"
			     :groups [{:name "Arithmetic"
				       :symbols '(+ - * / quot rem mod inc dec max min with-precision)}
				      {:name "Compare"
				       :symbols '(= == not= < > <= >=)}
				      {:name "Bitwise Operations"
				       :symbols '(bit-and bit-or bit-xor bit-flip bit-not bit-clear bit-set bit-shift-left bit-shift-right bit-test)}
				      {:name "Cast"
				       :symbols '(byte short int long float double bigint bigdec num rationalize)}
				      {:name "Test"
				       :symbols '(nil? identical? zero? pos? neg? even? odd?)}]}
			    {:name "Symbols / Keywords"
			     :groups [{:name "Create"
				       :symbols '(keyword symbol)}
				      {:name "Use"
				       :symbols '(name intern namespace)}
				      {:name "Test"
				       :symbols '(keyword? symbol?)}]}
			    {:name "Strings / Characters"
			     :groups [{:name "Create"
				       :symbols '(str print-str println-str pr-str prn-str with-out-str)}
				      {:name "Use"
				       :symbols '(count get subs format)}
				      {:name "Cast / Test"
				       :symbols '(char char? string?)}]}
			    {:name "Regular Expressions"
			     :groups [{:name "Create"
				       :symbols '(re-pattern re-matcher)}
				      {:name "Use"
				       :symbols '(re-find re-matches re-seq re-groups)}]}]}
	      {:name "Operations"
	       :categories [{:name "Flow Control"
			     :groups [{:name "Normal"
				       :symbols '(if if-not if-let when when-not when-let when-first cond condp case do eval loop recur trampoline while)}
				      {:name "Exceptional"
				       :symbols '(try catch finally throw assert)}
				      {:name "Delay"
				       :symbols '(delay delay? deref force)}
				      {:name "Function Based"
				       :symbols '(repeatedly iterate)}
				      {:name "Sequence Based"
				       :symbols '(dotimes doseq for)}
				      {:name "Laziness"
				       :symbols '(lazy-seq lazy-cat doall dorun)}]}
			    {:name "Type Inspection"
			     :groups [{:name "Clojure Types"
				       :symbols '(type extends? satisfies?)}
				      {:name "Java Types"
				       :symbols '(class bases supers class? instance? isa? cast)}]}
			    {:name "Concurrency"
			     :groups [{:name "General"
				       :symbols '(deref get-validator set-validator!)}
				      {:name "Atoms"
				       :symbols '(atom swap! reset! compare-and-set!)}
				      {:name "Refs"
				       :symbols '(ref sync dosync ref-set alter commute ensure io! ref-history-count ref-max-history ref-min-history)}
				      {:name "Agents"
				       :symbols '(agent send send-off await await-for agent-error restart-agent shutdown-agents *agent* error-handler set-error-handler! error-mode set-error-mode! release-pending-sends)}
				      {:name "Futures"
				       :symbols '(future future-call future-done? future-cancel future-cancelled? future?)}
				      {:name "Thread Local Values"
				       :symbols '(bound-fn bound-fn* get-thread-bindings push-thread-bindings pop-thread-bindings thread-bound?)}
				      {:name "Misc"
				       :symbols '(locking pcalls pvalues pmap seque promise deliver add-watch remove-watch)}]}]}

	      {:name "Functions"
	       :categories [{:name "General"
			     :groups [{:name "Create"
				       :symbols '(fn defn defn- definline identity constantly memfn comp complement partial juxt memoize)}
				      {:name "Call"
				       :symbols '(-> ->> apply)}
				      {:name "Test"
				       :symbols '(fn? ifn?)}]}
			    {:name "Multifunctions"
			     :groups [{:name "Create"
				       :symbols '(defmulti defmethod)}
				      {:name "Inspect and Modify"
				       :symbols '(get-method methods prefer-method prefers remove-method remove-all-methods)}]}
			    {:name "Macros"
			     :groups [{:name "Create"
				       :symbols '(defmacro macroexpand macroexpand-1 gensym)}]}
			    {:name "Java Interop"
			     :groups [{:name "Use"
				       :symbols '(doto .. set!)}
				      {:name "Arrays"
				       :symbols '(make-array object-array boolean-array byte-array char-array short-array int-array long-array float-array double-array aclone to-array to-array-2d into-array)}
				      {:name "Use"
				       :symbols '(aget aset aset-boolean aset-char aset-byte aset-int aset-long aset-short aset-float aset-double alength amap areduce)}
				      {:name "Cast"
				       :symbols '(booleans bytes chars ints shorts longs floats doubles)}]}
			    {:name "Proxies"
			     :groups [{:name "Create"
				       :symbols '(proxy get-proxy-class construct-proxy init-proxy)}
				      {:name "Misc"
				       :symbols '(proxy-mappings proxy-super update-proxy)}]}]}
	      {:name "Collections / Sequences"
	       :categories [{:name "Collections"
			     :groups [{:name "Generic Operations"
				       :symbols '(count empty not-empty into conj)}
				      {:name "Content Tests"
				       :symbols '(contains? distinct? empty? every? not-every? some not-any?)}
				      {:name "Capabilities"
				       :symbols '(sequential? associative? sorted? counted? reversible?)}
				      {:name "Type Tests"
				       :symbols '(coll? seq? vector? list? map? set?)}]}
			    {:name "Vectors"
			     :groups [{:name "Create"
				       :symbols '(vec vector vector-of)}
				      {:name "Use"
				       :symbols '(conj peek pop get assoc subvec rseq)}]}
			    {:name "Lists"
			     :groups [{:name "Create"
				       :symbols '(list list*)}
				      {:name "Use"
				       :symbols '(cons conj peek pop first rest)}]}
			    {:name "Maps"
			     :groups [{:name "Create"
				       :symbols '(hash-map array-map zipmap sorted-map sorted-map-by bean frequencies)}
				      {:name "Use"
				       :symbols '(assoc assoc-in dissoc find key val keys vals get get-in update-in select-keys merge merge-with)}
				      {:name "Use (Sorted Maps)"
				       :symbols '(rseq subseq subseq rsubseq rsubseq)}]}
			    {:name "Sets"
			     :groups [{:name "Create"
				       :symbols '(hash-set set sorted-set sorted-set-by)}
				      {:name "Use"
				       :symbols '(conj disj get)}]}
			    {:name "Structs"
			     :groups [{:name "Create"
				       :symbols '(defstruct create-struct struct struct-map accessor)}
				      {:name "Use"
				       :symbols '(get assoc)}]}
			    {:name "Sequences"
			     :groups [{:name "Create"
				       :symbols '(seq sequence repeat replicate range repeatedly iterate lazy-seq lazy-cat cycle interleave interpose tree-seq xml-seq enumeration-seq iterator-seq file-seq line-seq resultset-seq)}
				      {:name "Use (General)"
				       :symbols '(first second last rest next ffirst nfirst fnext nnext nth nthnext rand-nth butlast take take-last take-nth take-while drop drop-last drop-while keep keep-indexed)}
				      {:name "Use ('Modification')"
				       :symbols '(conj concat distinct group-by partition partition-all partition-by split-at split-with filter remove replace shuffle)}
				      {:name "Use (Iteration)"
				       :symbols '(for doseq map map-indexed mapcat reduce reductions max-key min-key doall dorun)}]}
			    {:name "Transients"
			     :groups [{:name "Create"
				       :symbols '(transient persistent!)}
				      {:name "Use (General)"
				       :symbols '(conj! pop! assoc! dissoc! disj!)}
				      {:name "Use ('Modification')"
				       :symbols '(conj concat distinct group-by partition partition-all partition-by split-at split-with filter remove replace shuffle)}
				      {:name "Use (Iteration)"
				       :symbols '(for doseq map map-indexed mapcat reduce reductions max-key min-key doall dorun)}]}]}
	      {:name "Code Structure"
	       :categories [{:name "Varibles"
			     :groups [{:name "Create"
				       :symbols '(def defonce intern declare)}
				      {:name "Use"
				       :symbols '(set! alter-var-root binding with-bindings with-bindings* with-local-vars letfn gensym)}
				      {:name "Inspect"
				       :symbols '(var find-var var-get var? bound? resolve ns-resolve special-symbol?)}]}
			    {:name "Namespaces"
			     :groups [{:name "Create &amp; Delete"
				       :symbols '(ns create-ns remove-ns)}
				      {:name "Inspect"
				       :symbols '(*ns* ns-name all-ns the-ns find-ns ns-publics ns-interns ns-refers ns-aliases ns-imports ns-map)}
				      {:name "Use"
				       :symbols '(in-ns ns-resolve ns-unalias ns-unmap alias)}
				      {:name "Misc"
				       :symbols '(namespace-munge print-namespace-doc)}]}
			    {:name "Hierarchies"
			     :groups [{:name "General"
				       :symbols '(make-hierarchy derive underive parents ancestors descendants isa?)}]}
			    {:name "User Defined Types"
			     :groups [{:name "General"
				       :symbols '(defprotocol defrecord deftype reify extend extend-protocol extend-type extenders)}]}
			    {:name "Metadata"
			     :groups [{:name "General"
				       :symbols '(meta with-meta vary-meta reset-meta! alter-meta!)}]}]}
	      {:name "Environment"
	       :categories [{:name "Require / Import"
			     :groups [{:name "General"
				       :symbols '(use require import refer-clojure refer)}]}
			    {:name "Code"
			     :groups [{:name "General"
				       :symbols '(*compile-files* *compile-path* *file* *warn-on-reflection* compile load load-file load-reader load-string read read-string gen-class gen-interface loaded-libs test)}]}
			    {:name "IO"
			     :groups [{:name "General"
				       :symbols '(*in* *out* *err* print printf println pr prn print-str println-str pr-str prn-str newline flush read-line slurp spit with-in-str with-out-str with-open)}]}
			    {:name "REPL"
			     :groups [{:name "General"
				       :symbols '(*1 *2 *3 *e *print-dup* *print-length* *print-level* *print-meta* *print-readably* )}]}
			    {:name "Misc"
			     :groups [{:name "General"
				       :symbols '(*clojure-version* clojure-version *command-line-args* time)}]}]}])

#_(spit "/Users/zkim/napplelabs/clojuredocs/cd-site/app/cc_quick_ref.rb" (rubify-spheres spheres))


(def rubify-map nil)
(def rubify-seq nil)

#_ (rubify-seq 0 spheres)

(defn id-for-symbol [symbol]
  (try
   (let [m (meta (resolve symbol))
	 {:keys [ns name]} m
	 ns (str ns)
	 name (str name)]
     (try (Integer. (with-connection db 
		      (transaction (with-query-results rs ["select * from functions where ns=? and name=?" ns name] (:id (first (doall rs)))))))
	  (catch Exception e 0)))))

#_(println (rubify-map 0 {:name "Exceptional"
			:symbols '(try catch finally throw assert)}))

#_(println (rubify-symbols 0 '(try catch finally throw assert)))

(defn rubify-symbols [indent syms]
  (rubify-seq indent
	      (doall (map #(if (string? %) 
			     {:name %
			      :ns ""
			      :link ""
			      :id 0}
			     (let [m (meta (resolve %))
				   {:keys [ns name]} m
				   ns (if (nil? ns) "" ns)
				   name (if (nil? name) (str %) name)
				   id (id-for-symbol %)] 
			       {:name (str name)
				:ns (str ns)
				:link (if (empty? (str ns)) "" (str "http://clojuredocs.org/v/" id))
				:id id})) 
			  syms))))

(defn rubify-map [indent m]
  (let [tabs (apply str (take indent (repeat "\t")))
	tabs-inc (apply str (take (inc indent) (repeat "\t")))]
    (str "{\n"
	 (apply str
		(interpose ",\n"
			   (map
			    #(cond
			      (= :symbols (first %)) (str tabs-inc (first %) " => " (rubify-symbols (inc indent) (second %)))
			      (map? (second %)) (str tabs-inc (first %) " => " (rubify-map (inc indent) (second %)))
			      (vector? (second %)) (str tabs-inc (first %) " => " (rubify-seq (inc indent) (second %)))
			      (number? (second %)) (str tabs-inc (first %) " => " (second %))
			      :else (str tabs-inc (first %) " => \"" (second %) "\""))
			    m)))
	 "\n" tabs "}")))

#_ (spit "/Users/zkim/napplelabs/clojuredocs/cd-site/app/cc_quick_ref.rb" (rubify-spheres spheres))

(defn rubify-seq [indent s]
  (let [tabs (apply str (take indent (repeat "\t")))
	tabs-inc (apply str (take (inc indent) (repeat "\t")))]
    (str "[\n"
	 (apply str
		(interpose ",\n"
			   (map
			    #(cond
			      (map? %) (str tabs-inc (rubify-map (inc indent) %))
			      (vector? %) (str tabs-inc (rubify-seq (inc indent) %))
			      :else (str tabs-inc (first %) " => \"" (second %) "\""))
			    s)))
	 "\n" tabs "]\n")))

(defn rubify-spheres [spheres]
  (str "class CCQuickRef\n"
       "\tdef self.spheres\n"
       "\t\t"
       (rubify-seq 2 spheres)
       "\tend\n"
       "end"))

(defn cat-toc [cat]
  (html
   [:ul {:class "toc_cat"}
    [:li [:a {:name (str (:name cat) "_toc") :href (str "#" (:name cat ))} (:name cat)]
     [:ul {:class "toc_group"}
      #_ (map
	  #(html [:li [:a {:href (str "#" (:name cat) (:name %))} (:name %)]])
	  (:groups cat))]]]))

(defn sphere-toc [sphere]
  (html 
   [:fieldset {:class "toc_sphere"}
    [:legend (:name sphere)]
    [:ul (map cat-toc (:categories sphere))]]))

#_ (do (spit "/Users/zkim/napplelabs/clojuredocs/cd-site/app/views/main/clojure_core.html.erb" (spheres-to-html spheres)))

(defn spheres-toc [spheres]
  (html [:div {:class "toc"}
	 [:h3 "Table of Contents"]
	 (map sphere-toc spheres)]))

(defn group-to-html [sphere cat]
  (fn [group]
    (html [:div {:class "group"}
	   [:table
	    [:tr
	     [:td {:class "var"}
	      [:span [:a {:name (str (:name cat) (:name group))} (str (:name group) ":")]]]
	     [:td
	      (map #(html [:span {:class "var"} (str %)]) (:symbols group))]]]])))

(defn group-with-desc-to-html [sphere cat]
  (fn [group]
    (html [:div {:class "group"}
	   [:div {:class "group_header"}
	    [:h5 [:a {:name (str (:name cat) (:name group))} (str (:name group) ":")]]
	    [:div {:class "signpost"}
	     (:name sphere)
	     ", "
	     (:name cat)]
	    [:div {:class "clear"}]]
	   [:div {:class "clear"}]
	   [:table
	    (with-connection db
	      (doall (map 
		      #(let [m (try (meta (resolve %)) (catch Exception e {:name % :doc ""}))
			     ns (str (:ns m))
			     name (str (:name m))
			     id 0 #_(try (transaction (with-query-results rs ["select * from functions where ns=? and name=?" ns name] (:id (first (doall rs))))))] 
			 (html [:tr [:td {:class "var"} [:a {:href (str "http://clojuredocs.org/v/" id)} (:name m)]] [:td {:class "desc"} (apply str (take 70 (:doc m)))]])) 
		      (:symbols group))))]])))

#_ (do (spit "/Users/zkim/napplelabs/clojuredocs/cd-site/app/views/main/clojure_core.html.erb" (spheres-to-html spheres)))

(defn cat-to-html [sphere]
  (fn [category]
    (html [:div {:class "cat"}
	   [:div {:class "cat_header"}
	    [:h4 [:a {:href (str "#" (:name category) "_toc") :name (:name category)} (:name category)]]]
	   [:div {:class "clear"}]
	   (apply str (map (group-to-html sphere category) (:groups category)))])))

(defn sphere-to-html [sphere]
  (html [:div {:class "sphere"}
	 [:div {:class "sphere_header"}
	  [:h3 [:a {:href (str "#" (:name sphere) "_toc") :name (:name sphere)} (:name sphere)]]
	  [:span [:a {:class "top" :href "#top"} "top"]]
	  [:div {:class "clear"}]]
	 [:div {:class "categories"} (map (cat-to-html sphere) (:categories sphere))]]))

(defn spheres-to-html [categories]
  (html
   [:div {:class "container_16"}
    [:div {:class "grid_3"}
     "<%= render :partial => 'lib_nav', :locals => {:lib => @library} %>"
     (spheres-toc spheres) 
     "<%= render :partial => '/lib_namespaces' %>"
     "&nbsp;"]
    [:div {:class "grid_10"}
     [:div {:class "clear"}]
     [:div {:class "quick_ref"}
      (apply str (map sphere-to-html spheres))]]
    [:div {:class "grid_3"} 
     "&nbsp;"]]))


#_ (do (spit "/Users/zkim/napplelabs/clojuredocs/cd-site/app/views/main/clojure_core.html.erb" (spheres-to-html spheres)))

