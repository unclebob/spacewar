(ns spacewar.game-logic.shots
  (:require
      [spacewar.geometry :refer :all]
      [spacewar.vector :as vector]
      [spacewar.game-logic.config :refer :all]))

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

