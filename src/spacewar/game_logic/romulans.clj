(ns spacewar.game-logic.romulans
  (:require [clojure.spec.alpha :as s]
            [spacewar.util :as util]
            [spacewar.game-logic.config :as gc]
            [spacewar.game-logic.explosions :as explosions]
            [spacewar.game-logic.shots :as shots]
            [spacewar.geometry :as geo]
            [quil.core :as q]
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


