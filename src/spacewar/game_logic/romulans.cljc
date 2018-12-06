(ns spacewar.game-logic.romulans
  (:require [clojure.spec.alpha :as s]
            [spacewar.game-logic.config :as glc]
            [spacewar.game-logic.explosions :as explosions]
            [spacewar.game-logic.shots :as shots]
            [spacewar.geometry :as geo]
            [spacewar.vector :as vector]
            [spacewar.util :as util]))

(s/def ::x number?)
(s/def ::y number?)
(s/def ::age number?)
(s/def ::state #{:invisible :appearing :visible :firing :fading :disappeared})
(s/def ::fire-weapon boolean?)

(s/def ::romulan (s/keys :reg-un [::x ::y ::age ::state ::fire-weapon]))
(s/def ::romulans (s/coll-of ::romulan))

(defn make-romulan [x y]
  {:x x :y y
   :age 0
   :state :invisible
   :fire-weapon false})

(defn update-romulans-age [ms world]
  (let [romulans (:romulans world)
        romulans (map #(update % :age + ms) romulans)]
    (assoc world :romulans romulans)))

(defn romulan-state-transition [ms age state]
  (let [min-time (condp = state
                   :invisible glc/romulan-invisible-time
                   :appearing glc/romulan-appearing-time
                   :visible glc/romulan-visible-time
                   :firing glc/romulan-firing-time
                   :fading glc/romulan-fading-time)
        past-time? (> age min-time)
        next-second? (<= (rem age 1000) ms)
        fifty-fifty? (< 0.5 (rand 1))]
    (and past-time? next-second? fifty-fifty?)))

(defn update-romulan-state [ms romulan]
  (if (romulan-state-transition ms (:age romulan) (:state romulan))
    (let [next-state (condp = (:state romulan)
                       :invisible :appearing
                       :appearing :visible
                       :visible :firing
                       :firing :fading
                       :fading :disappeared)]
      (assoc romulan :state next-state :age 0 :fire-weapon (= next-state :fading)))
    romulan))

(defn update-romulans-state [ms world]
  (let [romulans (:romulans world)
        romulans (map #(update-romulan-state ms %) romulans)]
    (assoc world :romulans romulans)))

(defn remove-disappeared-romulans [world]
  (let [romulans (:romulans world)
        romulans (doall (remove #(= :disappeared (:state %)) romulans))]
    (assoc world :romulans romulans)))

(defn- explode-romulan [romulan]
  (explosions/->explosion :romulan romulan)
  )

(defn destroy-hit-romulans [world]
  (let [{:keys [romulans explosions]} world
        hit-romulans (filter :hit romulans)
        explosions (concat explosions (map explode-romulan hit-romulans))
        romulans (remove :hit romulans)]
    (assoc world :romulans romulans :explosions explosions)))

(defn- romulan-shots [ship romulan]
  (if (:fire-weapon romulan)
    (let [bearing (geo/angle-degrees (util/pos romulan) (util/pos ship))
          shot (shots/->shot (:x romulan) (:y romulan) bearing :romulan-blast)]
      shot)
    nil))

(defn fire-romulan-weapons [world]
  (let [{:keys [shots romulans ship]} world
        new-shots (filter some? (map #(romulan-shots ship %) romulans))
        romulans (map #(assoc % :fire-weapon false) romulans)]
    (assoc world :romulans romulans :shots (concat shots new-shots)))
  )

(defn update-romulans [ms world]
  (->> world
       (update-romulans-age ms)
       (update-romulans-state ms)
       (fire-romulan-weapons)
       (remove-disappeared-romulans)
       (destroy-hit-romulans)))

(defn- add-occasional-romulan [world]
  (if (< (rand 1) glc/romulan-appear-odds-per-second)
    (let [{:keys [romulans ship]} world
          {:keys [x y]} ship
          dist (* glc/romulan-appear-distance (- 1.5 (rand 1)))
          angle (rand 360)
          pos (vector/from-angular dist (geo/->radians angle))
          [rx ry] (vector/add [x y] pos)
          romulans (conj romulans (make-romulan rx ry))]
      (assoc world :romulans romulans))
    world))


(defn update-romulans-per-second [world]
  (-> world
      (add-occasional-romulan)))


