(ns spacewar.game-logic.shots
  (:require
    [spacewar.geometry :refer :all]
    [spacewar.vector :as vector]
    [spacewar.game-logic.config :refer :all]
    [spacewar.ui.config :refer :all]
    [spacewar.util :refer :all]
    [clojure.set :as set]))

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
  (let [{:keys [ship phaser-shots torpedo-shots kinetic-shots]} world
        {:keys [x y selected-weapon weapon-spread-setting
                weapon-number-setting target-bearing]} ship
        shots (fire-weapon [x y]
                           target-bearing
                           weapon-number-setting
                           weapon-spread-setting)]
    (condp = selected-weapon
      :phaser
      (assoc world :phaser-shots (concat phaser-shots shots))

      :torpedo
      (assoc world :torpedo-shots (concat torpedo-shots shots))

      :kinetic
      (assoc world :kinetic-shots (concat kinetic-shots shots))))
  )

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
      {:x sx :y sy :bearing bearing :range range})))

(defn update-phaser-shot [ms shot]
  (update-shot shot (* ms phaser-velocity) phaser-range))

(defn update-kinetic-shot [ms shot]
  (update-shot shot (* ms kinetic-velocity) kinetic-range))

(defn- torpedo-distance [ms shot]
  (max ms
       (* ms
          torpedo-velocity
          (- 1 (/ (:range shot) torpedo-range)))))

(defn update-torpedo-shot [ms shot]
  (update-shot shot (torpedo-distance ms shot) torpedo-range))


(defn update-shot-positions [ms world]
  (let [{:keys [phaser-shots torpedo-shots kinetic-shots]} world
        phaser-shots (filter some?
                             (map #(update-phaser-shot ms %) phaser-shots))
        torpedo-shots (filter some?
                              (map #(update-torpedo-shot ms %) torpedo-shots))
        kinetic-shots (filter some?
                              (map #(update-kinetic-shot ms %) kinetic-shots))]
    (assoc world :phaser-shots phaser-shots
                 :torpedo-shots torpedo-shots
                 :kinetic-shots kinetic-shots))
  )

(defn- shot-to-explosion [weapon-tag {:keys [x y]}]
  {:x x :y y :age 0 :type (condp = weapon-tag
                            :phaser-shots :phaser
                            :torpedo-shots :torpedo
                            :kinetic-shots :kinetic
                            :else :none)}
  )

(defn- update-hits [world weapon-tag target-tag proximity hit-by]
  (let [shots (weapon-tag world)
        targets (target-tag world)
        explosions (or (:explosions world) [])
        pairs (for [t targets s shots]
                {:target t
                 :shot s
                 :distance (distance [(:x s) (:y s)]
                                     [(:x t) (:y t)])})
        hits (filter #(>= proximity (:distance %)) pairs)
        hit-targets (set (map :target hits))
        hit-shots (set (map :shot hits))
        targets (set/difference (set targets) hit-targets)
        shots (set/difference (set shots) hit-shots)
        hit-targets (map #(hit-by hits %) hit-targets)
        explosions (concat explosions (map #(shot-to-explosion weapon-tag %) hit-shots))]

    (assoc world target-tag (concat targets hit-targets)
                 weapon-tag (concat shots)
                 :explosions explosions)))

(defn- hit-by-phaser [hit-pairs target]
  (let [hit-shots (map :shot (filter #(= target (:target %)) hit-pairs))
        ranges (map :range hit-shots)]
    (assoc target :hit {:weapon :phaser :damage ranges}))
  )

(defn update-phaser-klingon-hits [world]
  (update-hits world :phaser-shots :klingons phaser-proximity hit-by-phaser))

(defn- hit-by-torpedo [hit-pairs target]
  (let [hit-shots (map :shot (filter #(= target (:target %)) hit-pairs))]
    (assoc target :hit {:weapon :torpedo :damage (* torpedo-damage (count hit-shots))}))
  )

(defn update-torpedo-klingon-hits [world]
  (update-hits world :torpedo-shots :klingons torpedo-proximity hit-by-torpedo))

(defn- hit-by-kinetic [hit-pairs target]
  (let [hit-shots (map :shot (filter #(= target (:target %)) hit-pairs))]
    (assoc target :hit {:weapon :kinetic :damage (* kinetic-damage (count hit-shots))}))
  )

(defn update-kinetic-klingon-hits [world]
  (update-hits world :kinetic-shots :klingons kinetic-proximity hit-by-kinetic))

(defn update-explosions [ms world]
  (let [explosions (:explosions world)
        explosions (map #(update % :age + ms) explosions)
        explosions (filter #(> explosion-duration (:age %)) explosions)]
    (assoc world :explosions explosions)))


(defn update-shots [ms world]
  (let [world (update-shot-positions ms world)
        world (update-phaser-klingon-hits world)
        world (update-torpedo-klingon-hits world)
        world (update-kinetic-klingon-hits world)
        world (update-explosions ms world)
        ]
    world))


