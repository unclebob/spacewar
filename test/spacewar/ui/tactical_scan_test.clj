(ns spacewar.ui.tactical-scan-test
  (:require [midje.sweet :refer [tabular facts fact => roughly]]
            [spacewar.ui.tactical-scan :refer [explosion-radius
                                               age-color
                                               target-arc]]
            [spacewar.ui.config :refer [phaser-target torpedo-target]]))

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
    (age-color ?age ?profile) => ?color)
  ?age ?profile ?color
  0 [{:until 100 :colors [[0 0 0] [100 100 100]]}] [0 0 0]
  1 [{:until 100 :colors [[0 0 0] [100 100 100]]}] [1 1 1]
  1 [{:until 100 :colors [[0 0 0] [100 100 100]]}
     {:until 200 :colors [[100 100 100] [255 100 20]]}] [1 1 1]
  101 [{:until 100 :colors [[0 0 0] [100 100 100]]}
       {:until 200 :colors [[100 100 100] [200 100 0]]}] [101 100 99])

(tabular
  (fact
    "target arc"
    (let [[r
           start
           stop] (target-arc {:selected-weapon ?weapon
                              :target-bearing ?bearing
                              :weapon-spread-setting ?spread})]
      r => ?radius
      start => (roughly ?start)
      stop => (roughly ?stop)))
  ?weapon ?bearing ?spread ?radius ?start ?stop
  :phaser 0 0 phaser-target -3 3
  :phaser 0 10 phaser-target -5 5
  :phaser 90 10 phaser-target 85 95
  :torpedo 0 0 torpedo-target -3 3
  :none 0 0 0 -3 3
  )
