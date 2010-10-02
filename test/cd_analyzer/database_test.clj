(ns cd-analyzer.database-test
  (:use [cd-analyzer.database]
        [clojure.contrib.sql]
        [lazytest describe expect]
        [clojure.contrib.pprint]
        [clojure.contrib.string :only (as-str)]))

(defn truncate-tables [& names]
  (with-connection *db*
    (transaction
     (doseq [n names]
       ; Can't figure out the syntax here, moving on
       (do-commands (as-str "truncate table " n))))))

(def test-ns-map {:name "my.ns"
                  :doc "doc for my ns"})

(def test-var-map {:name 'test-var
                   :ns "test.ns"
                   :file "dummydir/test.clj"
                   :line 123
                   :arglists ["[x]" "[x y]"]
                   :added "1.0"
                   :source "(defn test-var ([x y] (+ x y)) ([x y z] (+ x y z))"
                   :doc "the quick brown fox jumps over the lazy dog the quick brown fox jumps over the lazy dog"})

(def test-db {:classname "com.mysql.jdbc.Driver"
              :subprotocol "mysql"
              :subname "//localhost:3306/clojuredocs_test?user=root&password="
              :create true
              :username "root"
              :password ""})

(describe store-ns-map
  (do-it "correctly stores the test ns"
    (binding [*db* test-db]
      (truncate-tables :namespaces)

      (store-ns-map test-ns-map)

      (let [res (query-ns test-ns-map)]
        (expect (= (:name res) (:name test-ns-map)))
        (expect (= (:doc res) (:doc test-ns-map)))))))

(describe store-var-map
  (do-it "correctly stores the var w/ version rel"
    (binding [cd-analyzer.database/*db* test-db]
      (truncate-tables :functions :function_versions)

      ((store-var-map "Clojure Core" "1.0") test-var-map)

      (let [res (query-var "Clojure Core" test-var-map)]
        (expect (= 1 (count (:versions res))))
        (expect (= "1.0" (:version (first (:versions res))))))
      
      ((store-var-map "Clojure Core" "1.0") test-var-map)
      ((store-var-map "Clojure Core" "2.0") test-var-map)
      
      (let [res (query-var "Clojure Core" test-var-map)]
        (expect (= 2 (count (:versions res))))
        (expect (= "2.0" (:version (second (:versions res)))))))))

(comment (ns cd-analyzer.database-test
           (:use [cd-analyzer.database])
           (:use [clojure.contrib.sql])
           (:require [clojure.test :as t]))

         (def test-db {:classname "com.mysql.jdbc.Driver"
                       :subprotocol "mysql"
                       :subname "//localhost:3306/clojuredocs_test?user=root&password="
                       :create true
                       :username "root"
                       :password ""})



         (def test-ns-map {:name "my.ns"
                           :doc "doc for my ns"})

         (defn uuid [] (str (java.util.UUID/randomUUID)))

         (defn random-var-map []
           {:name (uuid)
            :ns (uuid)})

         (defn truncate-functions []
           (with-connection test-db
             (transaction
              (do-commands "truncate table functions;"))))

         (binding [*db* test-db]
           (query-ns "clojure.core"))

         (t/deftest store-ns-map-test
           (binding [*db* test-db]
             (store-ns-map test-ns-map)
             (let [{:keys [name doc]} (query-ns "my.ns")]
               (t/is (= "my.ns" name))
               (t/is (= "doc for my ns" doc)))))

         (t/deftest remove-stale-vars-test
           (binding [*db* test-db]
             (truncate-functions)
             (let [store-var-map (store-var-map "Clojure Core")]
               (dotimes [n 5]
                 (store-var-map (random-var-map))))
             (t/is (= 5 (count (remove-stale-vars "Clojure Core" (+ (System/currentTimeMillis) 2000)))))))

         (t/deftest lookup-var-id-test
           (binding [*db* test-db]
             (truncate-functions)
             ((store-var-map "Clojure Core") test-var-map)
             (with-connection test-db
               (t/is (= 1 (lookup-var-id test-var-map))))))

         (t/deftest query-var-test
           (binding [*db* test-db]
             (truncate-functions)
             ((store-var-map "Clojure Core") test-var-map)
             (t/is (nil? (query-var "" "")))
             (let [{:keys [name ns doc shortdoc created_at updated_at]} (query-var "test.ns" "test-var")]
               (t/is (= "test-var" name))
               (t/is (= "test.ns" ns))
               (t/is (= "the quick brown fox jumps over the lazy dog the quick brown fox jumps over the lazy dog" doc))
               (t/is (= "the quick brown fox jumps over the lazy dog the quick brown fox jumps " shortdoc))
               (t/is created_at)
               (t/is updated_at))))
)