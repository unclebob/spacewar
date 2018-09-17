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

(facts
  "angle"
  (fact
    "z: coincident points"
    (angle [0 0] [0 0]) => :bad-angle)
  (fact
    "o: unit circle"
    (angle [0 0] [1 0]) => (roughly 0 1e-5)
    (angle [0 0] [0 1]) => (roughly 90 1e-5)
    (angle [0 0] [-1 0]) => (roughly 180 1e-5)
    (angle [0 0] [0 -1]) => (roughly 270 1e-5)
    (angle [0 0] [1 1]) => (roughly 45 1e-5)
    (angle [0 0] [-1 1]) => (roughly 135 1e-5)
    (angle [0 0] [-1 -1]) => (roughly 225 1e-5))
  (fact
    "m: 30 degrees"
    (angle [0 0] [(Math/sqrt 3) 1]) => (roughly 30 1e-5)
    (angle [0 0] [1 (Math/sqrt 3)]) => (roughly 60 1e-5)
    (angle [0 0] [(-(Math/sqrt 3)) 1]) => (roughly 150 1e-5)
    (angle [0 0] [-1 (-(Math/sqrt 3))]) => (roughly 240 1e-5)
    (angle [0 0] [(Math/sqrt 3) -1]) => (roughly 330 1e-5)))