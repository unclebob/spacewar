(ns spacewar.game-logic.klingons
  (:require [clojure.spec.alpha :as s]
            [spacewar.util :as util]
            [spacewar.game-logic.config :as glc]
            [spacewar.game-logic.explosions :as explosions]
            [spacewar.game-logic.shots :as shots]
            [spacewar.game-logic.hit :as hit]
            [spacewar.geometry :as geo]
            [quil.core :as q]
            [spacewar.vector :as vector]
            [spacewar.game-logic.clouds :as clouds]))

(s/def ::x number?)
(s/def ::y number?)
(s/def ::shields number?)
(s/def ::antimatter number?)

(s/def ::kinetics number?)
(s/def ::torpedos number?)
(s/def ::weapon-charge number?)
(s/def ::velocity (s/tuple number? number?))
(s/def ::thrust (s/tuple number? number?))
(s/def ::battle-state-age number?)
(s/def ::battle-state #{:no-battle :flank-right :flank-left :retreating :advancing})

(s/def ::klingon (s/keys :req-un [::x ::y ::shields ::antimatter
                                  ::kinetics ::torpedos ::weapon-charge
                                  ::velocity ::thrust ::battle-state-age ::battle-state]
                         :opt-un [::hit/hit]))
(s/def ::klingons (s/coll-of ::klingon))

(defn make-random-klingon []
  {:x (int (rand glc/known-space-x))
   :y (int (rand glc/known-space-y))
   :shields glc/klingon-shields
   :antimatter (rand glc/klingon-antimatter)
   :kinetics (rand glc/klingon-kinetics)
   :torpedos (rand glc/klingon-torpedos)
   :weapon-charge 0
   :velocity [(- 2 (rand 4)) (- 2 (rand 4))]
   :thrust [0 0]
   :battle-state-age 0
   :battle-state :no-battle})

(defn make-klingon [x y]
  {:x x
   :y y
   :shields glc/klingon-shields
   :antimatter glc/klingon-antimatter
   :kinetics glc/klingon-kinetics
   :torpedos glc/klingon-torpedos
   :weapon-charge 0
   :velocity [0 0]
   :thrust [0 0]
   :battle-state-age 0
   :battle-state :no-battle})

(defn initialize []
  (repeatedly glc/number-of-klingons make-random-klingon))

(defn damage-by-phasers [hit]
  (let [ranges (:damage hit)]
    (reduce +
            (map #(* glc/phaser-damage (- 1 (/ % glc/phaser-range)))
                 ranges))))

(defn- hit-damage [hit]
  (condp = (:weapon hit)
    :torpedo (:damage hit)
    :kinetic (:damage hit)
    :phaser (damage-by-phasers hit)))

(defn hit-klingon [klingon]
  (let [{:keys [shields hit battle-state-age]} klingon
        klingon (dissoc klingon :hit)
        shields (if (some? hit) (- shields (hit-damage hit)) shields)
        battle-state-age (if (some? hit)
                           (inc glc/klingon-battle-state-transition-age)
                           battle-state-age)]
    (assoc klingon :shields shields :battle-state-age battle-state-age)))

(defn- klingon-destruction [klingons]
  (if (empty? klingons)
    []
    (map #(explosions/->explosion :klingon %) klingons)))

(defn recharge-shield [ms klingon]
  (let [{:keys [antimatter shields]} klingon
        shield-deficit (- glc/klingon-shields shields)
        max-charge (* ms glc/klingon-shield-recharge-rate)
        real-charge (min shield-deficit
                         max-charge
                         (/ antimatter glc/klingon-shield-recharge-cost))
        antimatter (- antimatter (* glc/klingon-shield-recharge-cost real-charge))
        shields (+ shields real-charge)]
    (assoc klingon :antimatter antimatter
                   :shields shields)))

(defn- recharge-shields [ms klingons]
  (map #(recharge-shield ms %) klingons))

(defn- klingon-debris-cloud [klingon]
  (clouds/make-cloud (:x klingon) (:y klingon) (* (rand 1) glc/klingon-debris)))

(defn- klingon-debris-clouds [dead-klingons]
  (map klingon-debris-cloud dead-klingons))

(defn update-klingon-defense [ms world]
  (let [klingons (:klingons world)
        klingons (map hit-klingon klingons)
        dead-klingons (filter #(> 0 (:shields %)) klingons)
        klingons (filter #(<= 0 (:shields %)) klingons)
        klingons (recharge-shields ms klingons)]
    (assoc world :klingons klingons
                 :explosions (concat (:explosions world) (klingon-destruction dead-klingons))
                 :clouds (concat (:clouds world) (klingon-debris-clouds dead-klingons)))))

(defn- charge-weapons [ms klingons ship]
  (for [klingon klingons]
    (if (> glc/klingon-kinetic-firing-distance
           (geo/distance [(:x ship) (:y ship)]
                     [(:x klingon) (:y klingon)]))
      (let [efficiency (/ (:shields klingon) glc/klingon-shields)
            charge-increment (* ms efficiency)]
        (update klingon :weapon-charge + charge-increment))
      klingon)))

; Firing solution from
;http://danikgames.com/blog/how-to-intersect-a-moving-target-in-2d/
(defn- firing-solution [klingon ship shot-velocity]
  (let [ax (:x klingon)
        ay (:y klingon)
        bx (:x ship)
        by (:y ship)
        [ux uy] (:velocity ship)
        vmag shot-velocity
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
    (geo/angle-degrees [0 0]
                   [vx vy])))

(defn- ready-to-fire-kinetic? [klingon]
  (and
    (> (klingon :antimatter) glc/klingon-kinetic-power)
    (> (:kinetics klingon) 0)
    (<= glc/klingon-kinetic-threshold
        (:weapon-charge klingon))))

(defn- ready-to-fire-phaser? [klingon ship]
  (let [ship-pos [(:x ship) (:y ship)]
        klingon-pos [(:x klingon) (:y klingon)]
        dist (geo/distance ship-pos klingon-pos)]
    (and
      (< dist glc/klingon-phaser-firing-distance)
      (> (:antimatter klingon) glc/klingon-phaser-power)
      (<= glc/klingon-phaser-threshold
          (:weapon-charge klingon)))))

(defn- turning? [ship]
  (let [heading (:heading ship)
        heading-setting (:heading-setting ship)
        diff (q/abs (- heading heading-setting))]
    (> diff 0.5)))

(defn- ready-to-fire-torpedo? [klingon ship]
  (let [ship-pos [(:x ship) (:y ship)]
        klingon-pos [(:x klingon) (:y klingon)]
        dist (geo/distance ship-pos klingon-pos)]
    (and
      (not (turning? ship))
      (> (:torpedos klingon) 0)
      (< dist glc/klingon-torpedo-firing-distance)
      (> (:antimatter klingon) glc/klingon-torpedo-power)
      (<= glc/klingon-torpedo-threshold
          (:weapon-charge klingon)))))

(defn- weapon-velocity [weapon]
  (condp = weapon
    :klingon-kinetic glc/klingon-kinetic-velocity
    :klingon-phaser glc/klingon-phaser-velocity
    :klingon-torpedo glc/klingon-torpedo-velocity))

(defn- weapon-threshold [weapon]
  (condp = weapon
    :klingon-kinetic glc/klingon-kinetic-threshold
    :klingon-phaser glc/klingon-phaser-threshold
    :klingon-torpedo glc/klingon-torpedo-threshold))

(defn- weapon-power [weapon]
  (condp = weapon
    :klingon-kinetic glc/klingon-kinetic-power
    :klingon-phaser glc/klingon-phaser-power
    :klingon-torpedo glc/klingon-torpedo-power))

(defn- weapon-inventory [weapon]
  (condp = weapon
    :klingon-kinetic :kinetics
    :klingon-phaser nil
    :klingon-torpedo :torpedos))

(defn- fire-shot [klingon ship weapon]
  (shots/->shot
    (:x klingon) (:y klingon)
    (firing-solution klingon ship (weapon-velocity weapon))
    weapon)
  )

(defn- apply-weapon-costs [klingon weapon]
  (let [klingon (-> klingon
                    (update :weapon-charge - (weapon-threshold weapon))
                    (update :antimatter - (weapon-power weapon)))
        inventory (weapon-inventory weapon)]
    (if inventory
      (update klingon inventory dec)
      klingon)))

(defn- fire-charged-weapons [klingons ship]
  (loop [klingons klingons shots [] fired-klingons []]
    (if (empty? klingons)
      [fired-klingons shots]
      (let [klingon (first klingons)
            weapon (cond
                     (ready-to-fire-torpedo? klingon ship) :klingon-torpedo
                     (ready-to-fire-phaser? klingon ship) :klingon-phaser
                     (ready-to-fire-kinetic? klingon) :klingon-kinetic
                     :else nil)]
        (if weapon
          (recur (rest klingons)
                 (conj shots (fire-shot klingon ship weapon))
                 (conj fired-klingons (apply-weapon-costs klingon weapon)))
          (recur (rest klingons) shots (conj fired-klingons klingon)))))))

(defn delay-shooting? []
  (> 95 (rand 100)))

(defn update-klingon-offense [ms world]
  (if (:game-over world)
    world
    (let [{:keys [klingons ship shots]} world
          klingons (charge-weapons ms klingons ship)
          [klingons new-shots] (if (delay-shooting?)
                                 [klingons []]
                                 (fire-charged-weapons klingons ship))]
      (assoc world :klingons klingons
                   :shots (concat shots new-shots)))))

(defn- thrust-if-battle [ship klingon]
  (let [battle-state (:battle-state klingon)
        ship-pos (util/pos ship)
        klingon-pos (util/pos klingon)
        dist (geo/distance ship-pos klingon-pos)
        degrees (if (= ship-pos klingon-pos)
                  0
                  (geo/angle-degrees klingon-pos ship-pos))
        degrees (+ degrees (battle-state glc/klingon-evasion-trajectories))
        radians (geo/->radians degrees)
        efficiency (/ (:shields klingon) glc/klingon-shields)
        effective-thrust (min (klingon :antimatter)
                              (* glc/klingon-thrust efficiency))
        thrust (vector/from-angular effective-thrust radians)]
    (if (< glc/klingon-tactical-range dist)
      klingon
      (assoc klingon :thrust thrust))))

(defn- accelerate-klingon [ms klingon]
  (let [{:keys [thrust velocity]} klingon
        acc (vector/scale ms thrust)
        velocity (vector/add acc velocity)]
    (assoc klingon :velocity velocity)))

(defn- move-klingon [ms klingon]
  (let [{:keys [x y velocity]} klingon
        delta-v (vector/scale ms velocity)
        pos (vector/add delta-v [x y])]
    (assoc klingon :x (first pos) :y (second pos)))
  )

(defn calc-drag [ms]
  (Math/pow glc/klingon-drag ms))

(defn- drag-klingon [ms klingon]
  (let [drag-factor (calc-drag ms)]
    (update klingon :velocity #(vector/scale drag-factor %))))

(defn update-klingon-motion [ms world]
  (let [ship (:ship world)
        klingons (map #(thrust-if-battle ship %) (:klingons world))
        klingons (map #(accelerate-klingon ms %) klingons)
        klingons (map #(move-klingon ms %) klingons)
        klingons (map #(drag-klingon ms %) klingons)]
    (assoc world :klingons klingons)))

(defn- thrust-to-nearest [klingon bases]
  (if (= (:battle-state klingon) :no-battle)
    (let [distance-map (apply hash-map
                              (flatten
                                (map #(list (geo/distance (util/pos klingon) (util/pos %)) %) bases)))
          nearest-base (distance-map (apply min (keys distance-map)))
          angle-to-base (geo/angle-degrees (util/pos klingon) (util/pos nearest-base))
          thrust (vector/from-angular glc/klingon-thrust (geo/->radians angle-to-base))]
      (assoc klingon :thrust thrust))
    klingon))

(defn update-thrust-towards-nearest-base [world]
  (let [{:keys [klingons bases]} world
        klingons (if (empty? bases)
                   klingons
                   (map #(thrust-to-nearest % bases) klingons))]
    (assoc world :klingons klingons)))

(defn- find-thefts [klingons bases]
  (for [klingon klingons
        base bases
        :when (< (geo/distance (util/pos klingon) (util/pos base))
                 glc/ship-docking-distance)]
    [klingon base]))

(defn- steal-antimatter [[thief victim]]
  (let [amount-needed (- glc/klingon-antimatter (:antimatter thief))
        amount-available (:antimatter victim)
        amount-stolen (min amount-needed amount-available)
        thief (update thief :antimatter + amount-stolen)
        victim (update victim :antimatter - amount-stolen)]
    [thief victim]))

(defn klingons-steal-antimatter [world]
  (let [{:keys [klingons bases]} world
        thefts (find-thefts klingons bases)
        thieves (map first thefts)
        victims (map second thefts)
        innocents (vec (clojure.set/difference (set klingons) (set thieves)))
        unmolested (vec (clojure.set/difference (set bases) (set victims)))
        thefts (map steal-antimatter thefts)
        thieves (map first thefts)
        victims (map second thefts)
        klingons (concat (set thieves) innocents)
        bases (concat (set victims) unmolested)]
    (assoc world :klingons klingons :bases bases)))

(defn random-battle-state []
  (let [selected-index (rand-int (count glc/klingon-battle-states))]
    (nth glc/klingon-battle-states selected-index)))

(defn change-expired-battle-state [klingon]
  (let [{:keys [battle-state-age battle-state]} klingon]
    (if (>= battle-state-age glc/klingon-battle-state-transition-age)
      (random-battle-state)
      battle-state)))

(defn- update-klingon-state [ms ship klingon]
  (let [{:keys [antimatter battle-state-age]} klingon
        dist (geo/distance (util/pos klingon) (util/pos ship))
        new-battle-state (condp <= dist
                           glc/klingon-tactical-range :no-battle
                           glc/klingon-evasion-limit :advancing
                           (change-expired-battle-state klingon))
        new-battle-state (if (and
                               (<= antimatter glc/klingon-antimatter-runaway-threshold)
                               (<= dist glc/klingon-tactical-range))
                           :retreating
                           new-battle-state)
        age (if (>= battle-state-age glc/klingon-battle-state-transition-age)
              0
              (+ battle-state-age ms))]
    (assoc klingon :battle-state new-battle-state
                   :battle-state-age age)))

(defn update-klingons-state [ms world]
  (let [{:keys [ship klingons]} world
        klingons (map #(update-klingon-state ms ship %) klingons)]
    (assoc world :klingons klingons)))

(defn update-klingons [ms world]
  (->> world
       (update-klingons-state ms)
       (update-klingon-defense ms)
       (update-klingon-offense ms)
       (update-klingon-motion ms)
       ))

(defn update-klingons-per-second [world]
  (-> world
      (update-thrust-towards-nearest-base)
      (klingons-steal-antimatter)))