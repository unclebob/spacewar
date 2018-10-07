(ns spacewar.game-logic.stars
  (:require [clojure.spec.alpha :as s]
            [spacewar.game-logic.config :refer :all]))

(s/def ::x number?)
(s/def ::y number?)
(s/def ::class (set spectral-classes))
(s/def ::star (s/keys :req-un [::x ::y ::class]))
(s/def ::stars (s/coll-of ::star))

(defn- random-class []
    (condp < (rand)
      0.95 :o
      0.85 :b
      0.7 :a
      0.6 :f
      0.5 :g
      0.4 :k
      :m))

(defn make-random-star []
  {:x (int (rand known-space-x))
   :y (int (rand known-space-y))
   :class (random-class)})

(defn initialize []
  (repeatedly number-of-stars make-random-star))
