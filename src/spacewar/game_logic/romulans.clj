(ns spacewar.game-logic.romulans
  (:require [clojure.spec.alpha :as s]
            [spacewar.game-logic.config :as gc]
            [spacewar.geometry :as geo]
            [spacewar.vector :as vector]))

(s/def ::x number?)
(s/def ::y number?)
(s/def ::age number?)
(s/def ::state #{:invisible :appearing :visible :firing :fading :disappeared})

(s/def ::romulan (s/keys :reg-un [::x ::y ::age ::state]))
(s/def ::romulans (s/coll-of ::romulan))

(defn make-romulan [x y]
  {:x x :y y
   :age 0
   :state :invisible})

(defn update-romulans-age [ms world]
  (let [romulans (:romulans world)
        romulans (map #(update % :age + ms) romulans)]
    (assoc world :romulans romulans)))

(defn romulan-state-transition [ms age state]
  (let [min-time (condp = state
                   :invisible gc/romulan-invisible-time
                   :appearing gc/romulan-appearing-time
                   :visible gc/romulan-visible-time
                   :firing gc/romulan-firing-time
                   :fading gc/romulan-fading-time)
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
      (assoc romulan :state next-state :age 0))
    romulan))

(defn update-romulans-state [ms world]
  (let [romulans (:romulans world)
        romulans (map #(update-romulan-state ms %) romulans)]
    (assoc world :romulans romulans)))

(defn remove-disappeared-romulans [world]
  (let [romulans (:romulans world)
        romulans (doall (remove #(= :disappeared (:state %)) romulans))]
    (assoc world :romulans romulans)))

(defn update-romulans [ms world]
  (->> world
       (update-romulans-age ms)
       (update-romulans-state ms)
       (remove-disappeared-romulans)))

(defn- add-occasional-romulan [world]
  (if (< (rand 1) gc/romulan-appear-odds-per-second)
    (let [{:keys [romulans ship]} world
          {:keys [x y]} ship
          dist (* gc/romulan-appear-distance (- 1.5 (rand 1)))
          angle (rand 360)
          pos (vector/from-angular dist (geo/->radians angle))
          [rx ry] (vector/add [x y] pos)
          romulans (conj romulans (make-romulan rx ry))]
      (assoc world :romulans romulans))
    world))


(defn update-romulans-per-second [world]
  (-> world
      (add-occasional-romulan)))


