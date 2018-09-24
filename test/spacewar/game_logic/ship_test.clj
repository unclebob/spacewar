(ns spacewar.game-logic.ship-test
  (:require [midje.sweet :refer :all]
                [spacewar.game-logic.config :refer :all]
                [spacewar.game-logic.ship :refer :all]))

(facts
  "ship"
  (fact
    "rotate direction"
    (rotate-heading-direction 0 0) => 0
    (rotate-heading-direction 0 1) => 1
    (rotate-heading-direction 1 0) => -1
    (rotate-heading-direction 0 180) => 1
    (rotate-heading-direction 0 181) => -1
    (rotate-heading-direction 45 350) => -1
    (rotate-heading-direction 350 45) => 1
    (rotate-heading-direction 180 181) => 1
    )
  )
