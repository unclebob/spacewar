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

(tabular
  (fact
    "explosion color"
    (explosion-color ?age ?profile) => ?color)
  ?age ?profile ?color
  0 [{:until 100 :colors [[0 0 0] [100 100 100]]}] [0 0 0]
  1 [{:until 100 :colors [[0 0 0] [100 100 100]]}] [1 1 1]
  1 [{:until 100 :colors [[0 0 0] [100 100 100]]}
     {:until 200 :colors [[100 100 100] [255 100 20]]}] [1 1 1]
  101 [{:until 100 :colors [[0 0 0] [100 100 100]]}
       {:until 200 :colors [[100 100 100] [200 100 0]]}] [101 100 99]

  )