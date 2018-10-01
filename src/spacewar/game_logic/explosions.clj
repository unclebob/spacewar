(ns spacewar.game-logic.explosions
  (:require
    [spacewar.geometry :refer :all]
    [spacewar.game-logic.config :refer :all]
    [spacewar.ui.config :refer :all]
    [spacewar.util :refer :all]))

(defn make-fragments [n explosion velocity]
  (let [{:keys [x y]} explosion]
    (repeatedly n
                #(identity {:x x :y y :velocity (* (+ 0.8 (rand 0.2)) velocity) :direction (rand 360)}))))

(defn update-explosions [ms world]
  (let [explosions (:explosions world)
        explosions (map #(update % :age + ms) explosions)
        explosions (filter #(> explosion-duration (:age %)) explosions)]
    (assoc world :explosions explosions)))

(defn shot-to-explosion [weapon-tag {:keys [x y] :as explosion}]
  (let [weapon (condp = weapon-tag
                 :phaser-shots :phaser
                 :torpedo-shots :torpedo
                 :kinetic-shots :kinetic
                 :else :none)]
    {:x x :y y :age 0 :type weapon :fragments (make-fragments 20 explosion 0.5)})
  )