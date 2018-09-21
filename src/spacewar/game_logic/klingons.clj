(ns spacewar.game-logic.klingons
  (:require [clojure.spec.alpha :as s]
            [spacewar.game-logic.config :refer :all]))

(s/def ::x int?)
(s/def ::y int?)
(s/def ::klingon (s/keys :req-un [::x ::y]))
(s/def ::klingons (s/coll-of ::klingon))

(defn make-random-klingon []
  {:x (int (rand known-space-x))
   :y (int (rand known-space-y))})

(defn initialize []
  (repeatedly number-of-klingons make-random-klingon))
