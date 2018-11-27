(ns spacewar.game-logic.explosions
  (:require
    [spacewar.ui.config :as uic]
    [clojure.spec.alpha :as s]))

(s/def ::x number?)
(s/def ::y number?)
(s/def ::type #{:phaser :torpedo :kinetic
                :klingon :klingon-kinetic
                :klingon-phaser :ship})
(s/def ::age number?)
(s/def ::velocity number?)
(s/def ::direction number?)
(s/def ::fragments (s/coll-of (s/keys :req-un [::x ::y ::velocity ::direction])))
(s/def ::explosion (s/keys :req-un [::x ::y ::type ::age ::fragments]))
(s/def ::explosions (s/coll-of ::explosion))

(defn make-fragments [n explosion velocity]
  (let [{:keys [x y]} explosion]
    (repeatedly n
                #(identity {:x x :y y
                            :velocity (* (+ 0.8 (rand 0.2)) velocity)
                            :direction (rand 360)}))))

(defn- active-explosion [explosion]
  (let [{:keys [age type]} explosion
        profile (type uic/explosion-profiles)
        duration (:duration profile)]
    (> duration age)))

(defn update-explosions [ms world]
  (let [explosions (:explosions world)
        explosions (map #(update % :age + ms) explosions)
        explosions (filter active-explosion explosions)]
    (assoc world :explosions (doall explosions))))

(defn ->explosion [explosion-type {:keys [x y] :as object}]
  (let [
        profile (explosion-type uic/explosion-profiles)]
    {:x x :y y
     :age 0 :type explosion-type
     :fragments (make-fragments (:fragments profile) object (:fragment-velocity profile))})
  )

(defn shot->explosion [shot]
  (->explosion (:type shot) shot))