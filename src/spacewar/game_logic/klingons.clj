(ns spacewar.game-logic.klingons
  (:require [clojure.spec.alpha :as s]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.explosions :as explosions]
            [spacewar.game-logic.shots :as shots]
            [spacewar.geometry :refer :all]
            [spacewar.geometry :refer :all]
            [quil.core :as q]))

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

(defn- charge-weapons [ms klingons ship]
  (for [klingon klingons]
    (if (> klingon-kinetic-firing-distance
           (distance [(:x ship) (:y ship)]
                     [(:x klingon) (:y klingon)]))
      (update klingon :kinetic-charge #(+ ms %))
      klingon)))

; Firing solution from
;http://danikgames.com/blog/how-to-intersect-a-moving-target-in-2d/
(defn- kinetic-firing-solution [klingon ship]
  (let [ax (:x klingon)
        ay (:y klingon)
        bx (:x ship)
        by (:y ship)
        [ux uy] (:velocity ship)
        vmag klingon-kinetic-velocity
        abx (- bx ax)
        aby (- by ay)
        abmag (q/sqrt (+ (* abx abx) (* aby aby)))
        abx (/ abx abmag)
        aby (/ aby abmag)
        udotab (+ (* abx ux) (* aby uy))
        ujx (* udotab abx)
        ujy (* udotab aby)
        uix (- ux ujx)
        uiy (- uy ujy)
        vix uix
        viy uiy
        vimag (q/sqrt (+ (* vix vix) (* viy viy)))
        vjmag (q/sqrt (+ (* vmag vmag) (* vimag vimag)))
        vjx (* abx vjmag)
        vjy (* aby vjmag)
        vx (+ vjx vix)
        vy (+ vjy viy)]
    (angle-degrees [0 0]
                   [vx vy])))

(defn- ready-to-fire-kinetic? [klingon]
  (and
    (> (:kinetics klingon) 0)
    (<= klingon-kinetic-threshold
        (:kinetic-charge klingon))))

(defn- fire-charged-weapons [klingons ship]
  (filter some?
          (for [klingon klingons]
            (if (ready-to-fire-kinetic? klingon)
              (shots/->shot (:x klingon)
                            (:y klingon)
                            (kinetic-firing-solution klingon ship)
                            :klingon-kinetic)
              nil))))

(defn- discharge-fired-weapons [klingons]
  (for [klingon klingons]
    (if (ready-to-fire-kinetic? klingon)
      (assoc klingon :kinetic-charge 0
                     :kinetics (dec (:kinetics klingon)))
      klingon)))

(defn klingon-offense [ms world]
  (let [{:keys [klingons ship shots]} world
        klingons (charge-weapons ms klingons ship)
        new-shots (fire-charged-weapons klingons ship)
        klingons (discharge-fired-weapons klingons)]
    (assoc world :klingons klingons
                 :shots (concat shots new-shots))))

(defn update-klingons [ms world]
  (let [world (klingon-defense ms world)
        world (klingon-offense ms world)]
    world))
