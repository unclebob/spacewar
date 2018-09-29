(ns spacewar.ui.tactical-scan-test
  (:require [midje.sweet :refer :all]
            [spacewar.ui.tactical-scan :refer :all]))

(tabular
  (fact
    "explosion-radius"
    (explosion-radius ?age ?profile) => ?radius)
  ?age ?profile ?radius
  0 [{:velocity 100 :until 200}] 0
  1 [{:velocity 100 :until 200}] 100
  201 [{:velocity 100 :until 200}] nil
  201 [{:velocity 100 :until 200}
       {:velocity -20 :until 400}] (+ (* 100 200) -20)
  400 [{:velocity 100 :until 200}
       {:velocity -20 :until 400}] (+ (* 100 200)
                                      (* 200 -20))
  401 [{:velocity 100 :until 200}
         {:velocity -20 :until 400}] nil

  )