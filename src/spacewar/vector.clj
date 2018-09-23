(ns spacewar.vector
  (:require [clojure.spec.alpha :as s]))

(s/def ::number (s/or :number number? :rational rational?))
(s/def ::vector (s/coll-of ::number :count 2))

(defn add [[x1 y1] [x2 y2]]
  [(+ x1 x2) (+ y1 y2)])

(defn from-angular [length radians]
  [(* length (Math/cos radians))
   (* length (Math/sin radians))])