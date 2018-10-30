(ns spacewar.game-logic.shots
  (:require
    [spacewar.geometry :refer :all]
    [spacewar.vector :as vector]
    [spacewar.game-logic.config :refer :all]
    [spacewar.game-logic.ship :as the_ship]
    [spacewar.ui.config :refer :all]
    [spacewar.util :refer :all]
    [clojure.set :as set]
    [spacewar.game-logic.explosions :as explosions]
    [clojure.spec.alpha :as s]
    [quil.core :as q]))


(s/def ::x number?)
(s/def ::y number?)
(s/def ::bearing (s/and number? #(<= 0 % 360)))
(s/def ::range number?)
(s/def ::type #{:phaser :torpedo :kinetic :klingon-kinetic :klingon-phaser})
(s/def ::shot (s/keys :req-un [::x ::y ::bearing ::range ::type]))
(s/def ::shots (s/coll-of ::shot))

(defn ->shot [x y bearing type]
  {:x x :y y :bearing bearing :range 0 :type type})

(defn fire-weapon [pos bearing number spread]
  (let [start-bearing (if (= number 1)
                        bearing
                        (- bearing (/ spread 2)))
        bearing-inc (if (= number 1)
                      0
                      (/ spread (dec number)))]
    (loop [shots []
           bearing start-bearing
           number number]
      (if (zero? number)
        shots
        (recur (conj shots
                     {:x (first pos)
                      :y (second pos)
                      :bearing (mod bearing 360)
                      :range 0})
               (+ bearing bearing-inc)
               (dec number))))))

(defn- power-required [weapon]
  (condp = weapon
    :phaser phaser-power
    :torpedo torpedo-power
    :kinetic kinetic-power))

(defn- decrement-inventory [ship]
  (let [{:keys [weapon-number-setting
                selected-weapon
                kinetics torpedos]} ship
        kinetics (if (= selected-weapon :kinetic)
                   (- kinetics weapon-number-setting)
                   kinetics)
        torpedos (if (= selected-weapon :torpedo)
                   (- torpedos weapon-number-setting)
                   torpedos)]
    (assoc ship :kinetics kinetics :torpedos torpedos)))

(defn- sufficient-inventory [ship]
  (let [{:keys [weapon-number-setting
                selected-weapon
                kinetics torpedos]} ship]
    (condp = selected-weapon
      :torpedo (>= torpedos weapon-number-setting)
      :kinetic (>= kinetics weapon-number-setting)
      true)))

(defn weapon-failure-dice [n damage]
  (repeatedly n #(< (rand 100) damage)))

(defn weapon-bearing-deviation [n damage]
  (repeatedly n #(if (> (rand 100) damage)
                   0
                   (- 10 (rand 20)))))

(defn- corrupt-shot [shot deviation]
  (let [bearing (:bearing shot)
        deviated-bearing (mod (+ deviation bearing) 360)]
    (assoc shot :bearing deviated-bearing)))

(defn corrupt-shots-by-damage [damage shots]
  (let [shots-and-failure (partition
                            2
                            (interleave
                              (weapon-failure-dice (count shots) damage)
                              shots))
        final-shots (map second (remove first shots-and-failure))]
    (map #(corrupt-shot %1 %2)
         final-shots
         (weapon-bearing-deviation (count final-shots) damage))))

(defn weapon-fire-handler [_ world]
  (let [{:keys [ship]} world
        {:keys [x y selected-weapon weapon-spread-setting
                weapon-number-setting target-bearing
                antimatter]} ship
        required-power (* weapon-number-setting (power-required selected-weapon))
        can-shoot? (and (< required-power antimatter)
                        (sufficient-inventory ship))
        antimatter (if can-shoot?
                     (- antimatter required-power)
                     antimatter)
        shots (if can-shoot?
                (fire-weapon [x y]
                             target-bearing
                             weapon-number-setting
                             weapon-spread-setting)
                [])
        ship (if can-shoot?
               (->> ship
                   (decrement-inventory)
                   (the_ship/heat-core required-power))
               ship)
        shots (map #(assoc % :type selected-weapon) shots)
        shots (corrupt-shots-by-damage (:weapons-damage ship) shots)
        ship (assoc ship :antimatter antimatter)]
    (assoc world :shots (concat (:shots world) shots)
                 :ship ship)))

(defn process-events [events world]
  (let [[_ world] (->> [events world]
                       (handle-event :weapon-fire weapon-fire-handler)
                       )]
    world))

(defn update-shot [shot distance range-limit]
  (let [{:keys [x y bearing range]} shot
        radians (->radians bearing)
        delta (vector/from-angular distance radians)
        [sx sy] (vector/add [x y] delta)
        range (+ range distance)]
    (if (> range range-limit)
      nil
      (assoc shot :x sx :y sy :range range))))

(def shot-velocity
  {:phaser phaser-velocity
   :torpedo torpedo-velocity
   :kinetic kinetic-velocity
   :klingon-kinetic klingon-kinetic-velocity
   :klingon-phaser klingon-phaser-velocity})

(defn- shot-distance [ms shot]
  (* ms ((:type shot) shot-velocity)))

(defn- shot-range-limit [shot]
  (condp = (:type shot)
    :kinetic kinetic-range
    :torpedo torpedo-range
    :phaser phaser-range
    :klingon-kinetic klingon-kinetic-range
    :klingon-phaser klingon-phaser-range))


(defn update-shot-positions [ms world]
  (let [{:keys [shots]} world]
    (->> shots
         (map #(update-shot % (shot-distance ms %) (shot-range-limit %)))
         (filter some?)
         (doall)
         (assoc world :shots))))

(def hit-proximity
  {:phaser phaser-proximity
   :torpedo torpedo-proximity
   :kinetic kinetic-proximity
   })

(defn shot-proximity [shot]
  (let [type (:type shot)]
    (type hit-proximity)))

(defn- hit-by-kinetic [hit-pairs target]
  (let [hit-shots (map :shot (filter #(= target (:target %)) hit-pairs))]
    (assoc target :hit {:weapon :kinetic :damage (* kinetic-damage (count hit-shots))}))
  )

(defn- hit-by-phaser [hit-pairs target]
  (let [hit-shots (map :shot (filter #(= target (:target %)) hit-pairs))
        ranges (map :range hit-shots)]
    (assoc target :hit {:weapon :phaser :damage ranges}))
  )

(defn- hit-by-torpedo [hit-pairs target]
  (let [hit-shots (map :shot (filter #(= target (:target %)) hit-pairs))]
    (assoc target :hit {:weapon :torpedo :damage (* torpedo-damage (count hit-shots))}))
  )

(def hit-processors
  {:phaser hit-by-phaser
   :torpedo hit-by-torpedo
   :kinetic hit-by-kinetic})

;kluge:  assumes all hits are of same type.
(defn- process-hit [hits target]
  (let [shot (:shot (first hits))
        type (:type shot)
        hit-by (type hit-processors)]
    (hit-by hits target)))

(defn- update-hits [world target-tag]
  (let [shots (:shots world)
        relevant-shots (filter #(#{:kinetic :torpedo :phaser} (:type %)) shots)
        targets (target-tag world)
        explosions (or (:explosions world) [])
        pairs (for [t targets s relevant-shots]
                {:target t
                 :shot s
                 :distance (distance [(:x s) (:y s)]
                                     [(:x t) (:y t)])})
        hits (filter #(>= (shot-proximity (:shot %)) (:distance %)) pairs)
        hit-targets (set (map :target hits))
        hit-shots (set (map :shot hits))
        targets (set/difference (set targets) hit-targets)
        shots (set/difference (set shots) hit-shots)
        hit-targets (map #(process-hit hits %) hit-targets)
        explosions (concat explosions (map #(explosions/shot->explosion %) hit-shots))]
    (assoc world target-tag (doall (concat targets hit-targets))
                 :shots (doall (concat shots))
                 :explosions (doall explosions))))

(defn update-klingon-hits [world]
  world (update-hits world :klingons)
  )

(defn- friend-or-foe [shot]
  (if (some? (#{:klingon-kinetic :klingon-phaser} (:type shot)))
    :foe
    :friend))

(defn- foe-weapon-proximity [type]
  (condp = type
    :klingon-kinetic klingon-kinetic-proximity
    :klingon-phaser klingon-phaser-proximity))

(defn- ship-hit-damage [shot]
  (condp = (:type shot)
    :klingon-kinetic klingon-kinetic-damage
    :klingon-phaser klingon-phaser-damage))

(defn- hit-miss-ship [ship shot]
  (let [dist (distance [(:x ship) (:y ship)]
                       [(:x shot) (:y shot)])
        proximity (foe-weapon-proximity (:type shot))]
    (if (<= dist proximity) :hit :miss)))

(defn calc-damage [shields damage]
  (let [shield-threshold (/ ship-shields 2)]
    (if (< shields shield-threshold)
      (* damage (- 1 (/ shields shield-threshold)))
      0)))

(defn up-to-max-damage [system damage]
  (min 100 (+ system damage)))

(defn incur-damage [damage system systems]
  (if (contains? systems system)
    (update systems system up-to-max-damage damage)
    systems))

(def damage-keys [:life-support-damage
                  :hull-damage
                  :sensor-damage
                  :impulse-damage
                  :warp-damage
                  :weapons-damage])

(defn- select-damaged-system []
  (nth damage-keys (q/round (rand 5))))

(defn update-ship-hits [world]
  (let [ship (:ship world)
        shot-groups (group-by friend-or-foe (:shots world))
        foe-shots (:foe shot-groups)
        friend-shots (:friend shot-groups)
        hit-miss-groups (group-by #(hit-miss-ship ship %) foe-shots)
        hits (:hit hit-miss-groups)
        misses (:miss hit-miss-groups)
        old-explosions (:explosions world)
        new-explosions (map explosions/shot->explosion hits)
        potential-damage (reduce + (map ship-hit-damage hits))
        shields (:shields ship)
        damage (calc-damage shields potential-damage)
        system (select-damaged-system)
        ship (incur-damage damage system ship)
        ship (assoc ship :shields (max 0 (- shields potential-damage)))]
    (assoc world :shots (concat friend-shots misses)
                 :ship ship
                 :explosions (concat old-explosions new-explosions)))
  )

(defn update-shots [ms world]
  (let [world (update-shot-positions ms world)
        world (update-klingon-hits world)
        world (update-ship-hits world)
        ]
    world))


