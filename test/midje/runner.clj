(ns midje.runner
  (:require [midje.repl :as repl]))

(defn -main [& _args]
  (let [results (repl/load-facts)]
    (System/exit (if (zero? (:failures results)) 0 1))))
