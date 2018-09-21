(ns spacewar.game-logic.stars
  (:require [clojure.spec.alpha :as s]
            [spacewar.game-logic.config :refer :all]))

(s/def ::x int?)
(s/def ::y int?)
(s/def ::star (s/keys :req-un [::x ::y]))
(s/def ::stars (s/coll-of ::star))

(defn make-random-star []
  {:x (int (rand known-space-x))
   :y (int (rand known-space-y))})

(defn initialize []
  (repeatedly number-of-stars make-random-star))
