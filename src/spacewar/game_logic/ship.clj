(ns spacewar.game-logic.ship
  (:require
    [spacewar.geometry :refer :all]
    [spacewar.vector :as vector]
    [spacewar.util :refer :all]
    [spacewar.game-logic.config :refer :all]
    [spacewar.game-logic.shots :as shots]))

(defn initialize []
  {:x (int (rand known-space-x))
   :y (int (rand known-space-y))
   :warp 0
   :warp-charge 0
   :impulse 0
   :heading 0
   :velocity [0 0]
   :selected-view :front-view
   :selected-weapon :none
   :selected-engine :none
   :target-bearing 0
   :engine-power-setting 0
   :weapon-number-setting 1
   :weapon-spread-setting 1
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

(defn- warp-ship [ms ship]
  (let [{:keys [x y warp warp-charge heading]} ship
        warp (or warp 0)
        warp-charge (or warp-charge 0)
        warp-charge-increment (* ms warp warp-charge-rate)
        warp-charge (+ warp-charge-increment warp-charge)
        warp-trigger (> warp-charge warp-threshold)
        warp-charge (if warp-trigger 0 warp-charge)
        radians (->radians heading)
        warp-vector (vector/from-angular warp-leap radians)
        [wx wy] (if warp-trigger
                  (vector/add [x y] warp-vector)
                  [x y])]
    (assoc ship :x wx :y wy :warp-charge warp-charge)))

(defn- impulse-ship [ms ship]
  (let [{:keys [velocity heading impulse x y]} ship
        drag (drag velocity)
        accelerated-v (apply-impulse ms velocity heading impulse)
        velocity (apply-drag drag accelerated-v)
        [px py] (vector/add [x y] velocity)]
    (assoc ship :x px :y py :velocity velocity)))

(defn update-ship [ms ship]
  (let [ship (warp-ship ms ship)
        ship (impulse-ship ms ship)
        {:keys [heading heading-setting]} ship
        new-heading (rotate-ship ms heading heading-setting)]
    (assoc ship :heading new-heading)))

(defn- set-heading-handler [{:keys [angle]} ship]
  (assoc ship :heading-setting angle))

(defn- set-target-bearing-handler [{:keys [angle]} ship]
  (assoc ship :target-bearing angle))

(defn- set-engine-power-handler [{:keys [value]} ship]
  (assoc ship :engine-power-setting value))

(defn- set-weapon-number-handler [{:keys [value]} ship]
  (assoc ship :weapon-number-setting value
              :weapon-spread-setting (if (> value 1) 1 0)))

(defn- set-weapon-spread-handler [{:keys [value]} ship]
  (assoc ship :weapon-spread-setting value))

(defn- engage-engine-handler [_ ship]
  (let [{:keys [selected-engine engine-power-setting]} ship]
    (if (= selected-engine :none)
      ship
      (assoc ship selected-engine engine-power-setting
                  :engine-power-setting 0))))

(defn weapon-fire-handler [_ ship]
  (let [{:keys [x y selected-weapon weapon-spread-setting
                weapon-number-setting target-bearing
                phaser-shots torpedo-shots kinetic-shots]} ship
        shots (shots/fire-weapon [x y]
                           target-bearing
                           weapon-number-setting
                           weapon-spread-setting)]
    (condp = selected-weapon
      :phaser
      (assoc ship :phaser-shots (concat phaser-shots shots))

      :torpedo
      (assoc ship :torpedo-shots (concat torpedo-shots shots))

      :kinetic
      (assoc ship :kinetic-shots (concat kinetic-shots shots))))
  )

(defn- select-impulse [_ ship]
  (let [selected-engine (:selected-engine ship)]
    (assoc ship :selected-engine
                (if (= selected-engine :impulse)
                  :none
                  :impulse))))

(defn- select-warp [_ ship]
  (let [selected-engine (:selected-engine ship)]
    (assoc ship :selected-engine
                (if (= selected-engine :warp)
                  :none
                  :warp))))

(defn- select-front-view [_ ship]
  (assoc ship :selected-view :front-view))

(defn- select-strat-view [_ ship]
  (assoc ship :selected-view :strat-view))

(defn- select-tact-view [_ ship]
  (assoc ship :selected-view :tact-view))

(defn- select-phaser [_ {:keys [selected-weapon] :as ship}]
  (assoc ship :selected-weapon (if (= selected-weapon :phaser) :none :phaser)))

(defn- select-torpedo [_ {:keys [selected-weapon] :as ship}]
  (assoc ship :selected-weapon (if (= selected-weapon :torpedo) :none :torpedo)))

(defn- select-kinetic [_ {:keys [selected-weapon] :as ship}]
  (assoc ship :selected-weapon (if (= selected-weapon :kinetic) :none :kinetic)))

(defn process-events [events ship]
  (let [[_ ship] (->> [events ship]
                      (handle-event :front-view select-front-view)
                      (handle-event :strategic-scan select-strat-view)
                      (handle-event :tactical-scan select-tact-view)
                      (handle-event :engine-direction set-heading-handler)
                      (handle-event :engine-power set-engine-power-handler)
                      (handle-event :weapon-direction set-target-bearing-handler)
                      (handle-event :weapon-number set-weapon-number-handler)
                      (handle-event :weapon-spread set-weapon-spread-handler)
                      (handle-event :engine-engage engage-engine-handler)
                      (handle-event :select-impulse select-impulse)
                      (handle-event :select-warp select-warp)
                      (handle-event :select-phaser select-phaser)
                      (handle-event :select-torpedo select-torpedo)
                      (handle-event :select-kinetic select-kinetic))]
    ship))