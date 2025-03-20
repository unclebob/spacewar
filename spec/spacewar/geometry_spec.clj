(ns spacewar.geometry-spec
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [spacewar.geometry :refer [->degrees
                                       ->radians
                                       angle-degrees
                                       distance
                                       inside-circle
                                       inside-rect
                                       round
                                       sign]]
            [speclj.core :refer [context describe it should should-not should=]]))

(s/def ::number (s/or :number number? :rational rational?))

(describe "inside-rect"
  (context "degenerate rectangle"
    (it "returns false"
      (should= false (inside-rect [0 0 0 0] [0 0]))))
  (context "top left is the only pixel inside"
    (it "returns true for [0 0]"
      (should= true (inside-rect [0 0 1 1] [0 0])))
    (it "returns false for other points"
      (should= false (inside-rect [0 0 1 1] [1 1]))
      (should= false (inside-rect [0 0 1 1] [0 1]))
      (should= false (inside-rect [0 0 1 1] [1 0])))))

(describe "distance"
  (context "no distance"
    (it "returns approximately 0"
      (should= 0 (distance [1 1] [1 1]) 1e-10)))
  (context "distance 1"
    (it "returns approximately 1 for various unit distances"
      (should= 1 (distance [0 0] [0 1]) 1e-10)
      (should= 1 (distance [0 0] [1 0]) 1e-10)
      (should= 1 (distance [1 0] [0 0]) 1e-10)
      (should= 1 (distance [0 1] [0 0]) 1e-10)
      (should= 1 (distance [0 0] [0 -1]) 1e-10)
      (should= 1 (distance [0 0] [-1 0]) 1e-10)
      (should= 1 (distance [-1 0] [0 0]) 1e-10)
      (should= 1 (distance [0 -1] [0 0]) 1e-10))))

(describe "inside-circle"
  (context "degenerate circle"
    (it "returns false"
      (should= false (inside-circle [0 0 0] [0 0]))))
  (context "circle of radius 1"
    (it "returns correct results"
      (should= false (inside-circle [0 0 1] [1 1]))
      (should= false (inside-circle [0 0 1] [-1 1]))
      (should= false (inside-circle [0 0 1] [1 -1]))
      (should= false (inside-circle [0 0 1] [-1 -1]))
      (should= true (inside-circle [0 0 1] [0.5 0.5])))))

(describe "angle-degrees"
  (context "coincident points"
    (it "returns :bad-angle"
      (should= :bad-angle (angle-degrees [0 0] [0 0]))))
  (context "unit circle"
    (it "returns correct angles"
      (should= 0 (angle-degrees [0 0] [1 0]) 1e-5)
      (should= 90 (angle-degrees [0 0] [0 1]) 1e-5)
      (should= 180 (angle-degrees [0 0] [-1 0]) 1e-5)
      (should= 270 (angle-degrees [0 0] [0 -1]) 1e-5)
      (should= 45 (angle-degrees [0 0] [1 1]) 1e-5)
      (should= 135 (angle-degrees [0 0] [-1 1]) 1e-5)
      (should= 225 (angle-degrees [0 0] [-1 -1]) 1e-5)))
  (context "30 degrees"
    (it "returns correct angles"
      (should= 30 (angle-degrees [0 0] [(Math/sqrt 3) 1]) 1e-5)
      (should= 60 (angle-degrees [0 0] [1 (Math/sqrt 3)]) 1e-5)
      (should= 150 (angle-degrees [0 0] [(- (Math/sqrt 3)) 1]) 1e-5)
      (should= 240 (angle-degrees [0 0] [-1 (- (Math/sqrt 3))]) 1e-5)
      (should= 330 (angle-degrees [0 0] [(Math/sqrt 3) -1]) 1e-5))))

(describe "radians and degrees"
  (let [pi Math/PI
        tpi (* 2 pi)
        hpi (/ pi 2)]
    (context "->degrees"
      (it "converts correctly"
        (should= 0 (->degrees 0) 1e-5)
        (should= 180 (->degrees pi) 1e-5)
        (should= 0 (->degrees tpi) 1e-5)
        (should= 180 (->degrees (- pi)) 1e-5)
        (should= 90 (->degrees hpi) 1e-5)
        (should= 270 (->degrees (- hpi)) 1e-5)
        (should= 180 (->degrees (* 5 pi)) 1e-5)))
    (context "->radians"
      (it "converts correctly"
        (should= 0 (->radians 0) 1e-5)
        (should= (/ pi 4) (->radians 45) 1e-5)
        (should= hpi (->radians 90) 1e-5)
        (should= pi (->radians 180) 1e-5)
        (should= (+ pi hpi) (->radians 270) 1e-5)
        (should= 0 (->radians 360) 1e-5)
        (should= hpi (->radians 450) 1e-5)
        (should= (+ pi hpi) (->radians -90) 1e-5)))))

(describe "numerical functions"
  (context "abs properties"
    (it "never returns negative"
      (doseq [x (gen/sample (s/gen ::number))]
        (should-not (neg? (abs x)))))
    (it "returns either x or -x"
      (doseq [x (gen/sample (s/gen ::number))]
        (should (or (= (abs x) x) (= (abs x) (- x)))))))
  (context "round properties"
    (it "always returns integer"
      (doseq [x (gen/sample (s/gen ::number))]
        (should (int? (round x))))))
  (context "round rounds properly"
    (it "rounds specific values"
      (should= 0 (round 0))
      (should= 2 (round 1.5))
      (should= -1 (round -1.5))
      (should= -2 (round -1.51))
      (should= 1 (round 1.49))
      (should= -1 (round -1.49))))
  (context "sign"
    (it "returns correct signs"
      (should= 0 (sign 0))
      (should= 1 (sign 10))
      (should= -1 (sign -10)))))