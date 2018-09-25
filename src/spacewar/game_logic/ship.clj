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

(defn drag [[x y :as v]]
  (if (and (zero? x) (zero? y))
    [0 0]
    (let [mag (vector/magnitude v)
          mag-sqr (* mag mag)
          uv (vector/unit v)]
      (vector/scale (* -1 mag-sqr drag-factor) uv))))

(defn apply-drag [drag velocity]
  (let [new-velocity (vector/add velocity drag)]
    (if (= (sign (first new-velocity))
           (sign (first velocity)))
      new-velocity
      [0 0])))

(defn apply-impulse [ms velocity heading impulse]
  (let [acceleration (* ms impulse-thrust impulse)
        radians (->radians heading)
        av (vector/from-angular acceleration radians)
        new-velocity (vector/add velocity av)]
    new-velocity))


(defn rotation-direction [current-heading desired-heading]
  (let [diff (mod (- desired-heading current-heading) 360)]
    (cond (> diff 180) (- diff 360)
          :else diff)))

(defn rotate-ship [milliseconds heading desired-heading]
  (let [total-rotation (rotation-direction heading desired-heading)
        rotation-step (* rotation-rate
                         milliseconds
                         (sign total-rotation))
        rotation-step (if (< (abs total-rotation) (abs rotation-step))
                        total-rotation
                        rotation-step)
        new-heading (+ heading rotation-step)]
    new-heading))

(defn- echo-event [event [events state commands :as input]]
  (if (get-event event events)
    [events state (conj commands {:command event})]
    input))

(defn- handle-event [event handler [events state commands :as input]]
  (if-let [e (get-event event events)]
    (let [[new-commands new-state] (handler e state)]
      [events new-state (concat commands new-commands)])
    input))

(defn- update-ship [ms [events ship commands]]
  (let [{:keys [velocity x y heading heading-setting impulse warp warp-charge]} ship
        warp (or warp 0)
        impulse (or impulse 0)
        warp-charge (or warp-charge 0)
        warp-charge-increment (* ms warp warp-charge-rate)
        warp-charge (+ warp-charge-increment warp-charge)
        warp-trigger (> warp-charge warp-threshold)
        warp-charge (if warp-trigger 0 warp-charge)
        radians (->radians heading)
        warp-vector (vector/from-angular warp-leap radians)
        position (if warp-trigger
                   (vector/add [x y] warp-vector)
                   [x y])

        drag (drag velocity)
        accelerated-v (apply-impulse ms
                                     velocity
                                     heading
                                     impulse)
        new-velocity (apply-drag drag accelerated-v)
        [px py] (vector/add position new-velocity)
        new-heading (rotate-ship ms heading heading-setting)
        new-ship (assoc ship :x px :y py
                             :velocity new-velocity
                             :heading new-heading
                             :warp-charge warp-charge)]
    [events new-ship commands]
    )
  )

(defn- set-heading-handler [{:keys [angle]} ship]
  [[] (assoc ship :heading-setting angle)])

(defn- set-target-bearing-handler [{:keys [angle]} ship]
  [[{:command :set-weapon-direction :angle angle}]
   (assoc ship :target-bearing angle)])

(defn- set-engine-power-handler [{:keys [value]} ship]
  [[] (assoc ship :engine-power-setting value)])

(defn- set-weapon-number-handler [{:keys [value]} ship]
  [[{:command :set-weapon-number :number value}]
   (assoc ship :weapon-number-setting value)])

(defn- set-weapon-spread-handler [{:keys [value]} ship]
  [[{:command :set-weapon-spread :spread value}]
   (assoc ship :weapon-spread-setting value)])

(defn- engage-engine-handler [_ ship]
  (let [{:keys [selected-engine engine-power-setting]} ship]
    [[]
     (if (= selected-engine :none)
       ship
       (assoc ship selected-engine engine-power-setting
                   :engine-power-setting 0))]))

(defn- select-impulse [_ ship]
  (let [selected-engine (:selected-engine ship)]
    [[]
     (assoc ship :selected-engine
                 (if (= selected-engine :impulse)
                   :none
                   :impulse))]))

(defn- select-warp [_ ship]
  (let [selected-engine (:selected-engine ship)]
    [[]
     (assoc ship :selected-engine
                 (if (= selected-engine :warp)
                   :none
                   :warp))]))

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
                                (handle-event :select-impulse select-impulse)
                                (handle-event :select-warp select-warp)
                                (update-ship since-last-update))]
    [commands state]))