(ns spacewar.game-logic.klingons
  (:require [clojure.spec.alpha :as s]
            [spacewar.game-logic.config :refer :all]))

(s/def ::x int?)
(s/def ::y int?)
(s/def ::klingon (s/keys :req-un [::x ::y ::shields]))
(s/def ::klingons (s/coll-of ::klingon))

(defn make-random-klingon []
  {:x (int (rand known-space-x))
   :y (int (rand known-space-y))
   :shields klingon-shields})

(defn initialize []
  (repeatedly number-of-klingons make-random-klingon))

(defn hit-klingon [klingon]
  (let [hit (:hit klingon)
        shields (:shields klingon)
        klingon (dissoc klingon :hit)
        shields (if (some? hit) (- shields (:damage hit)) shields)]
    (assoc klingon :shields shields)))

(defn update-klingons [_ms world]
  (let [klingons (:klingons world)
        klingons (map hit-klingon klingons)
        klingons (filter #(< 0 (:shields %)) klingons)]
    (assoc world :klingons klingons)))
