(ns spacewar.ui.widgets-test
    (:require [midje.sweet :refer [facts fact => tabular]]
              [spacewar.ui.widgets.direction-selector :refer [degree-tick]]
              [spacewar.ui.widgets.horizontal-scale :as scale]
              [spacewar.ui.config :refer [white yellow red]]))

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

(facts
  "scale"
  (tabular
    (fact
    "scale colors"
    (scale/mercury-color ?value [[10 white] [20 yellow] [30 red]]) => ?color)
    ?value ?color
    0 white
    10 white
    11 yellow
    20 yellow
    30 red
    100 red))