(ns spacewar.game-logic.explosions
  (:require
    [spacewar.geometry :refer :all]
    [spacewar.game-logic.config :refer :all]
    [spacewar.ui.config :refer :all]
    [spacewar.util :refer :all]))

(defn update-explosions [ms world]
  (let [explosions (:explosions world)
        explosions (map #(update % :age + ms) explosions)
        explosions (filter #(> explosion-duration (:age %)) explosions)]
    (assoc world :explosions explosions)))

(defn shot-to-explosion [weapon-tag {:keys [x y]}]
  {:x x :y y :age 0 :type (condp = weapon-tag
                            :phaser-shots :phaser
                            :torpedo-shots :torpedo
                            :kinetic-shots :kinetic
                            :else :none)}
  )