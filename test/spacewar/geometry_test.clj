(ns spacewar.geometry-test
  (:require [midje.sweet :refer [facts fact => roughly =not=>]]
            [spacewar.geometry :refer [distance
                                       inside-circle
                                       angle-degrees
                                       ->degrees
                                       ->radians
                                       abs
                                       inside-rect
                                       round
                                       sign]]
            [clojure.spec.alpha :as s]
            [midje.experimental :refer [for-all]]))

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
    (angle-degrees [0 0] [0 0]) => :bad-angle)
  (fact
    "o: unit circle"
    (angle-degrees [0 0] [1 0]) => (roughly 0 1e-5)
    (angle-degrees [0 0] [0 1]) => (roughly 90 1e-5)
    (angle-degrees [0 0] [-1 0]) => (roughly 180 1e-5)
    (angle-degrees [0 0] [0 -1]) => (roughly 270 1e-5)
    (angle-degrees [0 0] [1 1]) => (roughly 45 1e-5)
    (angle-degrees [0 0] [-1 1]) => (roughly 135 1e-5)
    (angle-degrees [0 0] [-1 -1]) => (roughly 225 1e-5))
  (fact
    "m: 30 degrees"
    (angle-degrees [0 0] [(Math/sqrt 3) 1]) => (roughly 30 1e-5)
    (angle-degrees [0 0] [1 (Math/sqrt 3)]) => (roughly 60 1e-5)
    (angle-degrees [0 0] [(- (Math/sqrt 3)) 1]) => (roughly 150 1e-5)
    (angle-degrees [0 0] [-1 (- (Math/sqrt 3))]) => (roughly 240 1e-5)
    (angle-degrees [0 0] [(Math/sqrt 3) -1]) => (roughly 330 1e-5)))

(facts
  "about radians and degrees"
  (let [pi Math/PI
        tpi (* 2 pi)
        hpi (/ pi 2)]
    (fact
      "->degrees"
      (->degrees 0) => (roughly 0)
      (->degrees pi) => (roughly 180)
      (->degrees tpi) => (roughly 0)
      (->degrees (- pi)) => (roughly 180)
      (->degrees hpi) => (roughly 90)
      (->degrees (- hpi)) => (roughly 270)
      (->degrees (* 5 pi)) => (roughly 180))

    (fact
      "->radians"
      (->radians 0) => (roughly 0)
      (->radians 45) => (roughly (/ pi 4))
      (->radians 90) => (roughly hpi)
      (->radians 180) => (roughly pi)
      (->radians 270) => (roughly (+ pi hpi))
      (->radians 360) => (roughly 0)
      (->radians 450) => (roughly hpi)
      (->radians -90) => (roughly (+ pi hpi)))))

(s/def ::number (s/or :number number? :rational rational?))

(facts
  "numerical functions"
  (fact
    "abs properties"
    (for-all [x (s/gen ::number)]
             (abs x) =not=> neg?
             (abs x) => #(or (= x %) (= (- x) %)))
    )

  (fact
    "round properties"
    (for-all [x (s/gen ::number)]
             (round x) => int?))
  (fact
    "round rounds properly"
    (round 0) => 0
    (round 1.5) => 2
    (round -1.5) => -1
    (round -1.51) => -2
    (round 1.49) => 1
    (round -1.49) => -1
    )

  (fact
    "sign"
    (sign 0) => 0
    (sign 10) => 1
    (sign -10) => -1
    )
  )
