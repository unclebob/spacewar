(ns spacewar.ui.widgets-test
    (:require [midje.sweet :refer :all]
              [spacewar.ui.widgets :refer :all]))

(facts
  "direction-selector"
  (fact
    "cardinal ticks length 10"
    (degree-tick 100 0) => [0 100 0 90]
    (degree-tick 100 90) => [100 0 90 0]
    (degree-tick 100 180) => [0 -100 0 -90]
    (degree-tick 100 270) => [-100 0 -90 0])

  (fact
    "non-cardinal ticks length 5"
    (degree-tick 100 45) => [71 71 67 67]))