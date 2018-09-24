(ns spacewar.game-logic.ship-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.ship :refer :all]))

(facts
  "ship"
  (fact
    "rotate direction"
    (rotation-direction 0 0) => 0
    (rotation-direction 0 1) => 1
    (rotation-direction 1 0) => -1
    (rotation-direction 0 180) => 180
    (rotation-direction 0 181) => -179
    (rotation-direction 45 350) => -55
    (rotation-direction 350 45) => 55
    (rotation-direction 180 181) => 1)

  (fact
    "rotation timing"
    (let [dps (* 1000 rotation-rate)]
      (rotate-ship 1000 0 90) => (roughly dps)
      (rotate-ship 1000 90 0) => (roughly (- 90 dps))))

  (fact
    "rotation will not pass desired heading"
    (rotate-ship 1000 89 90) => 90
    (rotate-ship 1000 90 89) => 89)
  )
