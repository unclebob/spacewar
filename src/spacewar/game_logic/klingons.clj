(ns spacewar.game-logic.klingons
  (:require [clojure.spec.alpha :as s]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.explosions :as explosions]
            [spacewar.geometry :refer :all]))

(s/def ::x int?)
(s/def ::y int?)
(s/def ::shields number?)
(s/def ::antimatter number?)

(s/def ::weapon #{:phaser :torpedo :kinetic})
(s/def ::damage (s/or :damage-amount number?
                      :phaser-ranges (s/coll-of number?)))
(s/def ::hit (s/keys :req-un [::weapon ::damage]))
(s/def ::kinetics number?)
(s/def ::kinetic-charge number?)
(s/def ::klingon (s/keys :req-un [::x ::y ::shields ::antimatter
                                  ::kinetics ::kinetic-charge]
                         :opt-un [::hit]))
(s/def ::klingons (s/coll-of ::klingon))

(defn make-random-klingon []
  {:x (int (rand known-space-x))
   :y (int (rand known-space-y))
   :shields klingon-shields
   :antimatter klingon-antimatter
   :kinetics klingon-kinetics
   :kinetic-charge 0})

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

(defn recharge-shield [ms klingon]
  (let [{:keys [antimatter shields]} klingon
        shield-deficit (- klingon-shields shields)
        max-charge (* ms klingon-shield-recharge-rate)
        real-charge (min shield-deficit max-charge)
        antimatter (- antimatter real-charge)
        shields (+ shields real-charge)]
    (assoc klingon :antimatter antimatter
                   :shields shields)))

(defn- recharge-shields [ms klingons]
  (map #(recharge-shield ms %) klingons))

(defn klingon-defense [ms world]
  (let [klingons (:klingons world)
        klingons (map hit-klingon klingons)
        dead-klingons (filter #(> 0 (:shields %)) klingons)
        klingons (filter #(<= 0 (:shields %)) klingons)
        klingons (recharge-shields ms klingons)]
    (assoc world :klingons klingons
                 :explosions (concat (:explosions world) (klingon-destruction dead-klingons)))))

(defn- in-range [distance ship klingons]
  (filter #(> distance
              (distance [(:x ship) (:y ship)]
                        [(:x %) (:y%)])) klingons))

(defn klingon-offense [ms world]
  (let [{:keys [klingons ship]} world
        klingons (for [klingon klingons]
                   (if (> klingon-kinetic-range
                          (distance [(:x ship) (:y ship)]
                                    [(:x klingon) (:y klingon)]))
                     (update klingon :kinetic-charge #(+ ms %))
                     klingon))]
    (assoc world :klingons klingons)))

(defn update-klingons [ms world]
  (let [world (klingon-defense ms world)
        world (klingon-offense ms world)]
    world))
