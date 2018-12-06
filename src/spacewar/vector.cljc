(ns spacewar.vector
  (:require [clojure.spec.alpha :as s]
            [spacewar.geometry :as geo]))

(s/def ::number (s/or :number number? #?@(:clj [:rational rational?])))
(s/def ::vector (s/coll-of ::number :count 2))

(defn add [[x1 y1] [x2 y2]]
  [(+ x1 x2) (+ y1 y2)])

(defn subtract [[x1 y1] [x2 y2]]
  [(- x1 x2) (- y1 y2)])

(defn multiply [[x1 y1] [x2 y2]]
  [(* x1 x2) (* y1 y2)])

(defn scale [n [x y]]
  [(* n x) (* n y)])

(defn magnitude [v]
  (geo/distance [0 0] v))

(defn from-angular [length radians]
  [(* length (Math/cos radians))
   (* length (Math/sin radians))])

(defn unit [[x y :as v]]
  (if (and (zero? x) (zero? y))
    :no-unit-vector
    (scale (/ 1 (magnitude v)) v)
    ))
