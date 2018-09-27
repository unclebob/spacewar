(ns spacewar.game-logic.shots
  (:require
    [spacewar.geometry :refer :all]
    [spacewar.vector :as vector]
    [spacewar.game-logic.config :refer :all]
    [spacewar.util :refer :all]))

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


(defn update-shots [ms world]
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


