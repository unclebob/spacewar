(ns spacewar.game-logic.ship
  (:require
    [spacewar.geometry :refer :all]
    [spacewar.vector :as vector]
    [spacewar.util :refer :all]
    [spacewar.game-logic.config :refer :all]))

(defn initialize []
  {:x (int (rand known-space-x))
   :y (int (rand known-space-y))
   :heading 0
   :velocity [0 0]
   :target-bearing 0
   :engine-power-setting 0
   :weapon-number-setting 0
   :weapon-spread-setting 0
   :heading-setting 0
   :antimatter 100
   :core-temp 0
   :dilithium 100}
  )

(def rotation-rate 0.01) ; degrees per millisecond.

(defn rotate-heading-direction [current-heading desired-heading]
  (let [diff (mod (- desired-heading current-heading) 360)]
    (cond (> diff 180) -1
          (zero? diff) 0
          :else 1))
  )

(defn- echo-event [event [events state commands :as input]]
  (if (get-event event events)
    [events state (conj commands {:command event})]
    input))

(defn- handle-event [event handler [events state commands :as input]]
  (if-let [e (get-event event events)]
    (let [[new-commands new-state] (handler e state)]
      [events new-state (concat commands new-commands)])
    input))

(defn- update-ship [since-last-update [events ship commands]]
  (let [{:keys [velocity x y heading heading-setting]} ship
        [vx vy] velocity
        rotation (* rotation-rate since-last-update (rotate-heading-direction heading heading-setting))
        rotated-heading (+ heading rotation)
        new-heading (if (= (round rotated-heading) heading-setting)
                      heading-setting
                      rotated-heading)
        new-ship (assoc ship :x (+ x vx) :y (+ y vy)
                             :heading new-heading)]
    [events new-ship commands]
    )
  )

(defn- set-heading-handler [{:keys [angle]} ship]
  [[{:command :set-engine-direction :angle angle}]
   (assoc ship :heading-setting angle)])

(defn- set-target-bearing-handler [{:keys [angle]} ship]
  [[{:command :set-weapon-direction :angle angle}]
   (assoc ship :target-bearing angle)])

(defn- set-engine-power-handler [{:keys [value]} ship]
  [[{:command :set-engine-power :power value}]
   (assoc ship :engine-power-setting value)])

(defn- set-weapon-number-handler [{:keys [value]} ship]
  [[{:command :set-weapon-number :number value}]
   (assoc ship :weapon-number-setting value)])

(defn- set-weapon-spread-handler [{:keys [value]} ship]
  [[{:command :set-weapon-spread :spread value}]
   (assoc ship :weapon-spread-setting value)])

(defn- engage-engine-handler [_ ship]
  (let [[vx vy] (:velocity ship)
        radians (->radians (:heading ship))
        power (:engine-power-setting ship)
        [dx dy] (vector/from-angular power radians)]
    [[] (assoc ship :velocity [(+ dx vx) (+ dy vy)])]))

(defn process-events [events global-state]
  (let [{:keys [ship since-last-update]} global-state
        [_ state commands] (->> [events ship []]
                                (echo-event :front-view)
                                (echo-event :strategic-scan)
                                (echo-event :tactical-scan)
                                (handle-event :engine-direction set-heading-handler)
                                (handle-event :engine-power set-engine-power-handler)
                                (handle-event :weapon-direction set-target-bearing-handler)
                                (handle-event :weapon-number set-weapon-number-handler)
                                (handle-event :weapon-spread set-weapon-spread-handler)
                                (handle-event :engine-engage engage-engine-handler)
                                (update-ship since-last-update))]
    [commands state]))