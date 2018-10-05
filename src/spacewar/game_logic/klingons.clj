(ns spacewar.game-logic.klingons
  (:require [clojure.spec.alpha :as s]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.explosions :as explosions]))

(s/def ::x int?)
(s/def ::y int?)
(s/def ::shields number?)

(s/def ::weapon #{:phaser :torpedo :kinetic})
(s/def ::damage (s/or :damage-amount number?
                      :phaser-ranges (s/coll-of number?)))

(s/def ::hit (s/keys :req-un [::weapon ::damage]))
(s/def ::klingon (s/keys :req-un [::x ::y ::shields]
                         :opt-un [::hit]))
(s/def ::klingons (s/coll-of ::klingon))

(defn make-random-klingon []
  {:x (int (rand known-space-x))
   :y (int (rand known-space-y))
   :shields klingon-shields})

(defn initialize []
  (repeatedly number-of-klingons make-random-klingon))

(defn damage-by-phasers [hit]
  (let [ranges (:damage hit)]
    (reduce +
            (map #(* phaser-damage (- 1 (/ % phaser-range)))
                 ranges))))

(defn- hit-damage [hit]
  (condp = (:weapon hit)
    :torpedo (:damage hit)
    :kinetic (:damage hit)
    :phaser (damage-by-phasers hit)))

(defn hit-klingon [klingon]
  (let [hit (:hit klingon)
        shields (:shields klingon)
        klingon (dissoc klingon :hit)
        shields (if (some? hit) (- shields (hit-damage hit)) shields)]
    (assoc klingon :shields shields)))

(defn- klingon-destruction [klingons]
  (if (empty? klingons)
    []
    (map #(explosions/->explosion :klingon %) klingons)))

(defn update-klingons [_ms world]
  (let [klingons (:klingons world)
        klingons (map hit-klingon klingons)
        dead-klingons (filter #(> 0 (:shields %)) klingons)
        klingons (filter #(<= 0 (:shields %)) klingons)]
    (assoc world :klingons klingons
                 :explosions (concat (:explosions world) (klingon-destruction dead-klingons)))))
