(ns main
  (:use [cd-analyzer.core])
  (:gen-class))

(defn -main [& args]
  (when (= 0 (count args))
    (throw "Must specify library root path."))
  (if (> (count args) 1)
    (run-update (first args))
    (run-update (first args))))