(ns cd-analyzer.runner-test
  (:use [cd-analyzer.runner] :reload-all)
  (:use [cd-analyzer.util])
  (:require [clojure.test :as t]))

(t/deftest test-clone-repo
  (with-temp-dir dir
    (clone-target-repo "git://gist.github.com/509311.git" dir)
    (t/is (.exists (java.io.File. (str dir "/509311"))))))

