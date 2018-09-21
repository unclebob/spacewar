(ns spacewar.game-logic.bases
  (:require [clojure.spec.alpha :as s]
            [spacewar.game-logic.config :refer :all]))

(s/def ::x int?)
(s/def ::y int?)
(s/def ::base (s/keys :req-un [::x ::y]))
(s/def ::base (s/coll-of ::base))

(defn make-random-base []
  {:x (int (rand known-space-x))
   :y (int (rand known-space-y))})

(defn initialize []
  (repeatedly number-of-bases make-random-base))
