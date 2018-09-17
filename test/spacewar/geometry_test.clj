(ns spacewar.geometry-test
    (:require [midje.sweet :refer :all]
              [spacewar.geometry :refer :all]))

(facts
  "inside-rect"
  (fact
    "z: degenerate rectangle"
    (inside-rect [0 0 0 0] [0 0]) => false)
  (fact
    "o: top left is the only pixel inside"
    (inside-rect [0 0 1 1] [0 0]) => true
    (inside-rect [0 0 1 1] [1 1]) => false
    (inside-rect [0 0 1 1] [0 1]) => false
    (inside-rect [0 0 1 1] [1 0]) => false
    )
  )

(facts
  "distance"
  (fact
    "z: no distance"
    (distance [1 1] [1 1]) => (roughly 0 1e-10))
  (fact
    "o: distance 1"
    (distance [0 0] [0 1]) => (roughly 1 1e-10)
    (distance [0 0] [1 0]) => (roughly 1 1e-10)
    (distance [1 0] [0 0]) => (roughly 1 1e-10)
    (distance [0 1] [0 0]) => (roughly 1 1e-10)
    (distance [0 0] [0 -1]) => (roughly 1 1e-10)
    (distance [0 0] [-1 0]) => (roughly 1 1e-10)
    (distance [-1 0] [0 0]) => (roughly 1 1e-10)
    (distance [0 -1] [0 0]) => (roughly 1 1e-10)
    )
  )

(facts
  "inside-circle"
  (fact
    "z: degenerate-circle"
    (inside-circle [0 0 0] [0 0]) => false
    )
  (fact
    "o: circle of radius 1"
    (inside-circle [0 0 1] [1 1]) => false
    (inside-circle [0 0 1] [-1 1]) => false
    (inside-circle [0 0 1] [1 -1]) => false
    (inside-circle [0 0 1] [-1 -1]) => false
    (inside-circle [0 0 1] [0.5 0.5]) => true
    )
  )