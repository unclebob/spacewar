(ns spacewar.game-logic.shots
  (:require
    [spacewar.geometry :refer :all]
    [spacewar.vector :as vector]
    [spacewar.game-logic.config :refer :all]
    [spacewar.ui.config :refer :all]
    [spacewar.util :refer :all]
    [clojure.set :as set]
    [spacewar.game-logic.explosions :as explosions]
    [clojure.spec.alpha :as s]))


(s/def ::x number?)
(s/def ::y number?)
(s/def ::bearing (s/and number? #(<= 0 % 360)))
(s/def ::range number?)
(s/def ::type #{:phaser :torpedo :kinetic :klingon-kinetic})
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
                      :bearing bearing
                      :range 0})
               (+ bearing bearing-inc)
               (dec number))))))

(defn weapon-fire-handler [_ world]
  (let [{:keys [ship]} world
        {:keys [x y selected-weapon weapon-spread-setting
                weapon-number-setting target-bearing]} ship
        shots (fire-weapon [x y]
                           target-bearing
                           weapon-number-setting
                           weapon-spread-setting)
        shots (map #(assoc % :type selected-weapon) shots)]
    (assoc world :shots (concat (:shots world) shots))))

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
       :klingon-kinetic klingon-kinetic-velocity})

(defn- shot-distance [ms shot]
  (* ms ((:type shot) shot-velocity)))

(defn- shot-range-limit [shot]
  (condp = (:type shot)
    :kinetic kinetic-range
    :torpedo torpedo-range
    :phaser phaser-range
    :klingon-kinetic klingon-kinetic-range))


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

(defn update-ship-hits [world]
  world)

(defn update-shots [ms world]
  (let [world (update-shot-positions ms world)
        world (update-klingon-hits world)
        world (update-ship-hits world)
        ]
    world))


