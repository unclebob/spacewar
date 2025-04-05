(ns spacewar.game-logic.klingons
  (:require [clojure.spec.alpha :as s]
            [quil.core :as q]
            [spacewar.game-logic.clouds :as clouds]
            [spacewar.game-logic.config :as glc]
            [spacewar.game-logic.explosions :as explosions]
            [spacewar.game-logic.hit :as hit]
            [spacewar.game-logic.shots :as shots]
            [spacewar.geometry :as geo]
            [spacewar.util :as util]
            [spacewar.vector :as vector]))

;The Klingon state machine has two super-states: {:battle :cruise}.
;In the :battle super-state a klingon will use the :battle-state FSM.
;In the :cruise super-state a klingon will use the :cruise-state FSM.

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
(s/def ::battle-state #{:no-battle :flank-right :flank-left :retreating :advancing :kamikazee})
(s/def ::cruise-state #{:patrol :guard :refuel :mission})
(s/def ::mission #{:blockade :seek-and-destroy :escape-corbomite})

(s/def ::klingon (s/keys :req-un [::x ::y ::shields ::antimatter
                                  ::kinetics ::torpedos ::weapon-charge
                                  ::velocity ::thrust
                                  ::battle-state-age ::battle-state
                                  ::cruise-state
                                  ::mission]
                         :opt-un [::hit/hit]))
(s/def ::klingons (s/coll-of ::klingon))

(def cruise-fsm {:patrol {:low-antimatter :refuel
                          :low-torpedo :patrol
                          :capable :guard
                          :well-supplied :mission}
                 :refuel {:low-antimatter :refuel
                          :low-torpedo :patrol
                          :capable :guard
                          :well-supplied :mission}
                 :guard {:low-antimatter :refuel
                         :low-torpedo :guard
                         :capable :guard
                         :well-supplied :mission}
                 :mission {:low-antimatter :refuel
                           :low-torpedo :guard
                           :capable :refuel
                           :well-supplied :mission}
                 })

(defn super-state [klingon ship]
  (let [distance (geo/distance [(:x ship) (:y ship)]
                               [(:x klingon) (:y klingon)])]
    (if (< distance glc/klingon-tactical-range)
      :battle
      :cruise)))

(defn- random-mission []
  (if (< 0.5 (rand))
    :seek-and-destroy
    :blockade))

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
   :battle-state :no-battle
   :cruise-state :patrol
   :mission (random-mission)})

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
   :battle-state :no-battle
   :cruise-state :patrol
   :mission (random-mission)})

(defn new-klingon-from-praxis [world]
  (let [klingons (:klingons world)
        klingon (make-klingon (rand-int glc/known-space-x) 0)
        klingon (assoc klingon :mission (random-mission)
                               :cruise-state :mission
                               :thrust [0 glc/klingon-cruise-thrust]
                               :velocity [0 -1]
                               :antimatter glc/klingon-antimatter
                               :torpedos glc/klingon-torpedos
                               :kinetics glc/klingon-kinetics)]
    (assoc world :klingons (conj klingons klingon))))

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

(defn update-kamikazee [ms klingon]
  (if (= :kamikazee (:battle-state klingon))
    (update klingon :shields - (* ms glc/klingon-kamikazee-shield-depletion))
    klingon))

(defn destroyed-klingon? [klingon]
  (neg? (:shields klingon)))

(defn update-klingon-defense [ms {:keys [klingons klingons-killed explosions clouds] :as world}]
  (let [klingons (map hit-klingon klingons)
        klingons (map #(update-kamikazee ms %) klingons)
        dead (filter destroyed-klingon? klingons)
        alive (remove destroyed-klingon? klingons)
        klingons-killed (+ klingons-killed (count dead))
        klingons (recharge-shields ms alive)]
    (assoc world :klingons klingons :klingons-killed klingons-killed
                 :explosions (concat explosions (klingon-destruction dead))
                 :clouds (concat clouds (klingon-debris-clouds dead)))))

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

(defn- warping? [ship]
  (> (:warp ship) 0))

(defn- ready-to-fire-torpedo? [klingon ship]
  (let [ship-pos [(:x ship) (:y ship)]
        klingon-pos [(:x klingon) (:y klingon)]
        dist (geo/distance ship-pos klingon-pos)]
    (and
      (not (warping? ship))
      (not (turning? ship))
      (> (int (:torpedos klingon)) 0)
      (< dist glc/klingon-torpedo-firing-distance)
      (> (:antimatter klingon) glc/klingon-torpedo-power)
      (<= glc/klingon-torpedo-threshold
          (:weapon-charge klingon)))))

(defprotocol klingon-weapon
  (weapon-velocity [this])
  (weapon-threshold [this])
  (weapon-power [this])
  (weapon-inventory [this])
  (weapon-type [this]))

(deftype klingon-kinetic []
  klingon-weapon
  (weapon-velocity [_] glc/klingon-kinetic-velocity)
  (weapon-threshold [_] glc/klingon-kinetic-threshold)
  (weapon-power [_] glc/klingon-kinetic-power)
  (weapon-inventory [_] :kinetics)
  (weapon-type [_] :klingon-kinetic)
  )

(deftype klingon-phaser []
  klingon-weapon
  (weapon-velocity [_] glc/klingon-phaser-velocity)
  (weapon-threshold [_] glc/klingon-phaser-threshold)
  (weapon-power [_] glc/klingon-phaser-power)
  (weapon-inventory [_] nil)
  (weapon-type [_] :klingon-phaser)
  )

(deftype klingon-torpedo []
  klingon-weapon
  (weapon-velocity [_] glc/klingon-torpedo-velocity)
  (weapon-threshold [_] glc/klingon-torpedo-threshold)
  (weapon-power [_] glc/klingon-torpedo-power)
  (weapon-inventory [_] :torpedos)
  (weapon-type [_] :klingon-torpedo)
  )

(defn- fire-shot [klingon ship weapon]
  (let [shot-velocity (weapon-velocity weapon)
        kamikazee? (= :kamikazee (:battle-state klingon))
        shot-velocity (if kamikazee?
                        (* glc/kamikazee-shot-velocity-factor shot-velocity)
                        shot-velocity)
        shot (shots/->shot
               (:x klingon) (:y klingon)
               (firing-solution klingon ship shot-velocity)
               (weapon-type weapon))]
    (assoc shot :kamikazee kamikazee?)))

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
                     (ready-to-fire-torpedo? klingon ship) (->klingon-torpedo)
                     (ready-to-fire-phaser? klingon ship) (->klingon-phaser)
                     (ready-to-fire-kinetic? klingon) (->klingon-kinetic)
                     :else nil)]
        (if weapon
          (recur (rest klingons)
                 (conj shots (fire-shot klingon ship weapon))
                 (conj fired-klingons (apply-weapon-costs klingon weapon)))
          (recur (rest klingons) shots (conj fired-klingons klingon)))))))

(defn delay-shooting? []
  (> 95 (rand 100)))

(defn update-klingon-offense [ms world]
  (if (pos? (:game-over-timer world))
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
        efficiency (+ (/ 2 3) (/ efficiency 3))
        effective-thrust (min (klingon :antimatter)
                              (* glc/klingon-tactical-thrust efficiency))
        thrust (vector/from-angular effective-thrust radians)
        thrust (if (= :kamikazee (:battle-state klingon))
                 (vector/scale glc/klingon-kamikazee-thrust-factor thrust)
                 thrust)]
    (if (< glc/klingon-tactical-range dist)
      klingon
      (assoc klingon :thrust thrust))))

(defn- accelerate-klingon [ms klingon]
  (let [{:keys [thrust velocity antimatter]} klingon]
    (if (= thrust [0 0])
      klingon
      (let [available-antimatter (min glc/klingon-antimatter antimatter)
            full-thrust-antimatter (* ms glc/klingon-thrust-antimatter)
            thrusting-antimatter (min available-antimatter full-thrust-antimatter)
            efficiency (/ thrusting-antimatter full-thrust-antimatter)
            thrust (vector/scale efficiency thrust)
            acc (vector/scale ms thrust)
            velocity (vector/add acc velocity)
            antimatter (- antimatter thrusting-antimatter)]
        (assoc klingon :velocity velocity :antimatter antimatter)))))

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

(defn- full-retreat [klingon]
  (assoc klingon :thrust [0 (- glc/klingon-cruise-thrust)]))

(defn update-klingon-motion [ms world]
  (let [ship (:ship world)
        klingons (:klingons world)
        klingons (map #(thrust-if-battle ship %) klingons)
        klingons (map #(accelerate-klingon ms %) klingons)
        klingons (map #(move-klingon ms %) klingons)
        klingons (map #(drag-klingon ms %) klingons)]
    (assoc world :klingons klingons)))

(defn- find-thefts [klingons bases]
  (for [klingon klingons
        base bases
        :when (< (geo/distance (util/pos klingon) (util/pos base))
                 glc/klingon-docking-distance)]
    [klingon base]))

(defn- klingon-steals-antimatter [[thief victim]]
  (let [guarding? (or
                    (#{:guard :refuel} (:cruise-state thief))
                    (and (= :mission (:cruise-state thief))
                         (= :blockade (:mission thief))))
        amount-needed (- glc/klingon-antimatter (:antimatter thief))
        amount-available (:antimatter victim)
        amount-stolen (min amount-needed amount-available)
        thief (update thief :antimatter + amount-stolen)
        victim (update victim :antimatter - amount-stolen)
        thief (if guarding?
                (assoc thief :velocity [0 0] :thrust [0 0])
                thief)]
    [thief victim]))

(defn- steal-antimatter [thefts]
  (let [thieves (util/pos-map (map first thefts))
        victims (util/pos-map (map second thefts))]
    (loop [thefts thefts
           thieves thieves
           victims victims]
      (if (empty? thefts)
        [(vals thieves) (vals victims)]
        (let [theft (first thefts)
              thief (get thieves (util/pos (first theft)))
              victim (get victims (util/pos (second theft)))
              [thief victim] (klingon-steals-antimatter [thief victim])]
          (recur (rest thefts)
                 (assoc thieves (util/pos thief) thief)
                 (assoc victims (util/pos victim) victim)))))))

(defn klingons-steal-antimatter [world]
  (let [{:keys [klingons bases]} world
        thefts (find-thefts klingons bases)
        thieves (set (map first thefts))
        victims (set (map second thefts))
        innocents (vec (clojure.set/difference (set klingons) thieves))
        unmolested (vec (clojure.set/difference (set bases) victims))
        [thieves victims] (steal-antimatter thefts)
        klingons (concat thieves innocents)
        bases (concat victims unmolested)]
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
  (if (= :kamikazee (:battle-state klingon))
    klingon
    (let [{:keys [antimatter battle-state-age]} klingon
          dist (geo/distance (util/pos klingon) (util/pos ship))
          new-battle-state (condp <= dist
                             glc/klingon-tactical-range :no-battle
                             glc/klingon-evasion-limit :advancing
                             (change-expired-battle-state klingon))
          new-battle-state (if (and
                                 (<= antimatter glc/klingon-antimatter-runaway-threshold)
                                 (<= dist glc/klingon-tactical-range))
                             (if (< glc/klingon-kamikazee-probability (rand))
                               :retreating
                               :kamikazee)
                             new-battle-state)
          age (if (>= battle-state-age glc/klingon-battle-state-transition-age)
                0
                (+ battle-state-age ms))]
      (assoc klingon :battle-state new-battle-state
                     :battle-state-age age))))

(defn update-klingons-state [ms world]
  (let [{:keys [ship klingons]} world
        klingons (map #(update-klingon-state ms ship %) klingons)]
    (assoc world :klingons klingons)))

(defn update-torpedo-and-kinetic-production [ms {:keys [antimatter torpedos kinetics] :as klingon}]
  (let [deficit (- glc/klingon-torpedos torpedos)
        max-production (* ms glc/klingon-torpedo-production-rate)
        can-make-torpedos? (< antimatter glc/klingon-torpedo-antimatter-threshold)
        new-torpedos (if can-make-torpedos?
                       0
                       (min deficit max-production))
        efficiency (/ new-torpedos max-production)
        antimatter-cost (* ms glc/klingon-torpedo-antimatter-cost efficiency)
        torpedos (+ torpedos new-torpedos)
        antimatter (- antimatter antimatter-cost)
        kinetics (min glc/klingon-kinetics (+ kinetics (* ms glc/klingon-kinetic-production-rate)))]
    (assoc klingon :antimatter antimatter :torpedos torpedos :kinetics kinetics)))

(defn update-klingon-torpedo-production [ms world]
  (let [klingons (:klingons world)
        klingons (map #(update-torpedo-and-kinetic-production ms %) klingons)]
    (assoc world :klingons klingons))
  )

(defn remove-klingons-out-of-range [_ms world]
  (let [klingons (:klingons world)
        klingons (remove #(< (:y %) -10000) klingons)]
    (assoc world :klingons klingons))
  )

(defn update-klingons [ms world]
  (->> world
       (update-klingons-state ms)
       (update-klingon-defense ms)
       (update-klingon-offense ms)
       (update-klingon-motion ms)
       (update-klingon-torpedo-production ms)
       (remove-klingons-out-of-range ms)
       ))

(defn- thrust-to-nearest-base [klingon bases]
  (if (= (:battle-state klingon) :no-battle)
    (if (empty? bases)
      (assoc klingon :cruise-state :patrol)
      (let [distance-map (apply hash-map
                                (flatten
                                  (map #(list (geo/distance (util/pos klingon) (util/pos %)) %) bases)))
            nearest-base (distance-map (apply min (keys distance-map)))
            angle-to-base (geo/angle-degrees (util/pos klingon) (util/pos nearest-base))
            thrust (vector/from-angular glc/klingon-cruise-thrust (geo/->radians angle-to-base))]
        (assoc klingon :thrust thrust)))
    klingon))

(defn- thrust-towards-ship [klingon ship]
  (if (= (:battle-state klingon) :no-battle)
    (let [angle-to-ship (geo/angle-degrees (util/pos klingon) (util/pos ship))
          thrust (vector/from-angular glc/klingon-cruise-thrust (geo/->radians angle-to-ship))]
      (assoc klingon :thrust thrust))
    klingon))

(defn- thrust-toward-mission [klingon ship bases]
  (condp = (:mission klingon)
    :seek-and-destroy (thrust-towards-ship klingon ship)
    :blockade (thrust-to-nearest-base klingon bases)
    :escape-corbomite (full-retreat klingon)
    klingon)
  )

(defn- thrust-to-nearest-antimatter-source [klingon stars bases]
  (if (= (:battle-state klingon) :no-battle)
    (let [fraction-fuel-remaining (/ (:antimatter klingon) glc/klingon-antimatter)
          target-classes (condp <= fraction-fuel-remaining
                           0.5 #{:o :b}
                           0.3 #{:o :b :a :f}
                           #{:o :b :a :f :g :k :m})
          antimatter-stars (filter #(target-classes (:class %)) stars)
          antimatter-bases (filter #(= :antimatter-factory (:type %)) bases)
          base-distance-map (apply hash-map
                                   (flatten
                                     (map #(list (geo/distance (util/pos klingon) (util/pos %)) %)
                                          antimatter-bases)))
          star-distance-map (apply hash-map
                                   (flatten
                                     (map #(list (geo/distance (util/pos klingon) (util/pos %)) %)
                                          antimatter-stars)))
          distance-to-nearest-antimatter-star (apply min (keys star-distance-map))
          distance-to-nearest-antimatter-base (if (empty? antimatter-bases)
                                                1000000000  ;very far away.
                                                (apply min (keys base-distance-map)))
          nearest-antimatter-star (star-distance-map distance-to-nearest-antimatter-star)
          nearest-antimatter-base (base-distance-map distance-to-nearest-antimatter-base)
          angle-to-target (if (< distance-to-nearest-antimatter-base
                                 glc/klingon-antimatter-base-in-range)
                            (geo/angle-degrees (util/pos klingon) (util/pos nearest-antimatter-base))
                            (geo/angle-degrees (util/pos klingon) (util/pos nearest-antimatter-star)))
          thrust (vector/from-angular glc/klingon-cruise-thrust (geo/->radians angle-to-target))]
      (assoc klingon :thrust thrust))
    klingon))

(defn- thrust-in-random-direction [klingon]
  (if (= (:battle-state klingon) :no-battle)
    (let [angle (rand (* 2 Math/PI))
          tx (* glc/klingon-cruise-thrust (Math/cos angle))
          ty (* glc/klingon-cruise-thrust (Math/sin angle))]
      (assoc klingon :thrust [tx ty]))
    klingon)
  )

(defn- occupying? [base klingon]
  (let [distance (geo/distance (util/pos klingon) (util/pos base))]
    (<= distance glc/klingon-docking-distance)))

(defn- unoccupied-base? [base klingons]
  (let [occupiers (filter #(occupying? base %) klingons)]
    (<= (count occupiers) 3)))

(defn find-unoccupied-bases [bases klingons]
  (filter #(unoccupied-base? % klingons) bases)
  )

(defn cruise-klingons [{:keys [ship klingons bases stars] :as world}]
  (let [cruise-states (group-by :cruise-state klingons)
        on-mission-klingons (:mission cruise-states)
        patrolling-klingons (:patrol cruise-states)
        guarding-klingons (:guard cruise-states)
        refuelling-klingons (:refuel cruise-states)
        unoccupied-bases (find-unoccupied-bases bases klingons)
        on-mission-klingons (map #(thrust-toward-mission % ship unoccupied-bases) on-mission-klingons)
        guarding-klingons (map #(thrust-to-nearest-base % unoccupied-bases) guarding-klingons)
        refuelling-klingons (map #(thrust-to-nearest-antimatter-source % stars bases) refuelling-klingons)
        klingons (concat on-mission-klingons
                         patrolling-klingons
                         guarding-klingons
                         refuelling-klingons)
        ]
    (assoc world :klingons klingons))
  )

(defn cruise-transition [{:keys [antimatter torpedos]}]
  (let [antimatter (/ antimatter glc/klingon-antimatter 0.01)
        torpedos (/ torpedos glc/klingon-torpedos 0.01)]
    (cond
      (<= antimatter 40) :low-antimatter
      (<= torpedos 40) :low-torpedo
      (and (> antimatter 40) (> torpedos 60)) :well-supplied
      :else :capable
      ))
  )

(defn- change-cruise-state [klingon]
  (let [antimatter (:antimatter klingon)
        transition (cruise-transition klingon)
        cruise-state (:cruise-state klingon)
        new-state (if (and (= :refuel cruise-state)
                           (< antimatter glc/klingon-antimatter))
                    :refuel
                    (-> cruise-fsm cruise-state transition))]
    (assoc klingon :cruise-state new-state)))

(defn- change-all-cruise-states [{:keys [klingons] :as world}]
  (assoc world :klingons (map change-cruise-state klingons)))

(defn produce-antimatter [ms klingon stars]
  (let [antimatter (:antimatter klingon)
        thrust (:thrust klingon)
        velocity (:velocity klingon)
        refueling? (= :refuel (:cruise-state klingon))
        deficit (- glc/klingon-antimatter antimatter)
        distance-map (apply hash-map
                            (flatten
                              (map #(list (geo/distance (util/pos klingon) (util/pos %)) %) stars)))
        distance-to-nearest-star (apply min (keys distance-map))
        in-range? (< distance-to-nearest-star glc/klingon-range-for-antimatter-production)
        nearest-star (distance-map distance-to-nearest-star)
        production (if in-range?
                     (* ms (glc/klingon-antimatter-production-rate (:class nearest-star)))
                     0)
        thrust (if (and in-range? refueling?)
                 [0 0]
                 thrust)
        velocity (if (and in-range? refueling?)
                   [0 0]
                   velocity)]
    (assoc klingon :antimatter (+ antimatter (min deficit production))
                   :thrust thrust
                   :velocity velocity))
  )

(defn klingons-produce-antimatter [{:keys [klingons stars] :as world}]
  (let [klingons (map #(produce-antimatter 1000 % stars) klingons)]
    (assoc world :klingons klingons)))

(defn update-klingons-per-second [world]
  (-> world
      (cruise-klingons)
      (klingons-steal-antimatter)
      (klingons-produce-antimatter)))

(defn change-patrol-direction [{:keys [klingons] :as world}]
  (let [cruise-states (group-by :cruise-state klingons)
        on-mission-klingons (:mission cruise-states)
        patrolling-klingons (:patrol cruise-states)
        guarding-klingons (:guard cruise-states)
        refuelling-klingons (:refuel cruise-states)
        patrolling-klingons (map thrust-in-random-direction patrolling-klingons)
        klingons (concat on-mission-klingons
                         patrolling-klingons
                         guarding-klingons
                         refuelling-klingons)
        ]
    (assoc world :klingons klingons)))

(defn- add-klingons-from-praxis [world]
  (let [minutes (get world :minutes 0)
        probability (/ minutes glc/minutes-till-full-klingon-invasion)]
    (if (< (rand) probability)
      (new-klingon-from-praxis world)
      world)))

(defn- try-change-to-seek-and-destroy [klingon]
  (if (< (rand) glc/klingon-odds-to-become-destroyer)
    (assoc klingon :mission :seek-and-destroy)
    klingon))

(defn change-blockade-to-seek-and-destroy [{:keys [klingons] :as world}]
  (let [klingons (map try-change-to-seek-and-destroy klingons)]
    (assoc world :klingons klingons))
  )

(defn- change-mission-to-escape [klingon]
  (assoc klingon :mission :escape-corbomite :cruise-state :mission))

(defn- check-escape-corbomite [{:keys [klingons ship] :as world}]
  (let [corbomite (:corbomite-device-installed ship)
        klingons (if corbomite
                   (map change-mission-to-escape klingons)
                   klingons)]
    (assoc world :klingons klingons)))

(defn update-klingons-per-minute [world]
  (-> world
      (change-patrol-direction)
      (change-all-cruise-states)
      (change-blockade-to-seek-and-destroy)
      (check-escape-corbomite)
      (add-klingons-from-praxis)))

