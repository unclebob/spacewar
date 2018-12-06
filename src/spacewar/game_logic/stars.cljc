(ns spacewar.game-logic.stars
  (:require [clojure.spec.alpha :as s]
            [spacewar.game-logic.config :as glc]))

(s/def ::x number?)
(s/def ::y number?)
(s/def ::class (set glc/spectral-classes))
(s/def ::star (s/keys :req-un [::x ::y ::class]))
(s/def ::stars (s/coll-of ::star))

(defn- random-class []
    (condp < (rand)
      0.95 :o
      0.9 :b
      0.8 :a
      0.6 :f
      0.4 :g
      0.2 :k
      :m))

(defn make-random-star []
  {:x (int (rand glc/known-space-x))
   :y (int (rand glc/known-space-y))
   :class (random-class)})

(defn initialize []
  (repeatedly glc/number-of-stars make-random-star))
