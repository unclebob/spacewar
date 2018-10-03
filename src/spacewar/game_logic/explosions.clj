(ns spacewar.game-logic.explosions
  (:require
    [spacewar.geometry :refer :all]
    [spacewar.game-logic.config :refer :all]
    [spacewar.ui.config :refer :all]
    [spacewar.util :refer :all]))

(defn make-fragments [n explosion velocity]
  (let [{:keys [x y]} explosion]
    (repeatedly n
                #(identity {:x x :y y
                            :velocity (* (+ 0.8 (rand 0.2)) velocity)
                            :direction (rand 360)}))))

(defn- active-explosion [explosion]
  (let [{:keys [age type]} explosion
        profile (type explosion-profiles)
        duration (:duration profile)]
    (> duration age)))

(defn update-explosions [ms world]
  (let [explosions (:explosions world)
        explosions (map #(update % :age + ms) explosions)
        explosions (filter active-explosion explosions)]
    (assoc world :explosions (doall explosions))))

(defn shot-to-explosion [weapon-tag {:keys [x y] :as explosion}]
  (let [weapon (condp = weapon-tag
                 :phaser-shots :phaser
                 :torpedo-shots :torpedo
                 :kinetic-shots :kinetic
                 weapon-tag)
        profile (weapon explosion-profiles)]
    {:x x :y y
     :age 0 :type weapon
     :fragments (make-fragments (:fragments profile) explosion (:fragment-velocity profile))})
  )