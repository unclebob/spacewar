(ns spacewar.game-logic.ship
  (:require
    [spacewar.geometry :refer :all]
    [spacewar.vector :as vector]
    [spacewar.util :refer :all]
    [spacewar.game-logic.config :refer :all]
    [clojure.spec.alpha :as s]))

(s/def ::x number?)
(s/def ::y number?)
(s/def ::warp number?)
(s/def ::warp-charge number?)
(s/def ::impulse number?)
(s/def ::heading (s/and number? #(<= 0 % 360)))
(s/def ::velocity (s/tuple number? number?))
(s/def ::selected-view #{:front-view :strat-view :tact-view})
(s/def ::selected-weapon #{:phaser :kinetic :torpedo :none})
(s/def ::selected-engine #{:warp :impulse :none})
(s/def ::target-bearing (s/and number? #(<= 0 % 360)))
(s/def ::engine-power-setting number?)
(s/def ::weapon-number-setting number?)
(s/def ::weapon-spread-setting number?)
(s/def ::heading-setting (s/and number? #(<= 0 % 360)))
(s/def ::antimatter number?)
(s/def ::core-temp number?)
(s/def ::dilithium number?)
(s/def ::shields number?)
(s/def ::kinetics number?)
(s/def ::torpedos number?)
(s/def ::strat-scale (s/and number? #(<= 1 % 10)))

(s/def ::ship (s/keys :req-un [::x ::y ::warp ::warp-charge
                               ::impulse ::heading ::velocity
                               ::selected-view ::selected-weapon
                               ::selected-engine ::target-bearing
                               ::engine-power-setting
                               ::weapon-number-setting
                               ::weapon-spread-setting
                               ::heading-setting
                               ::antimatter ::core-temp
                               ::dilithium ::shields
                               ::kinetics ::torpedos ::strat-scale]))

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
   :antimatter ship-antimatter
   :core-temp 0
   :dilithium 100
   :shields ship-shields
   :kinetics ship-kinetics
   :torpedos ship-torpedos
   :strat-scale 1}
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
  (let [delta-v (* ms impulse-thrust impulse)
        radians (->radians heading)
        dv (vector/from-angular delta-v radians)
        new-velocity (vector/add velocity dv)]
    new-velocity))


(defn rotation-direction [current-heading desired-heading]
  (let [diff (mod (- desired-heading current-heading) 360)]
    (cond (> diff 180) (- diff 360)
          :else diff)))

(defn- warp-factor [warp]
  (* warp warp 0.5))

(defn- warp-ship [ms ship]
  (if (zero? (:warp ship))
    ship
    (let [{:keys [x y warp warp-charge heading antimatter]} ship
          power-required (* ms (warp-factor warp) warp-power)
          power-used (min power-required antimatter)
          antimatter (- antimatter power-used)
          actual-warp (* warp (/ power-used power-required))
          warp-charge-increment (* ms actual-warp warp-charge-rate)
          warp-charge (+ warp-charge-increment warp-charge)
          warp-trigger (> warp-charge warp-threshold)
          warp-charge (if warp-trigger 0 warp-charge)
          radians (->radians heading)
          warp-vector (vector/from-angular warp-leap radians)
          [wx wy] (if warp-trigger
                    (vector/add [x y] warp-vector)
                    [x y])]
      (assoc ship :x wx :y wy :warp-charge warp-charge
                  :antimatter antimatter))))

(defn- impulse-ship [ms ship]
  (let [{:keys [antimatter velocity heading impulse x y]} ship
        power-required (* ms impulse-power impulse)
        power-used (min power-required antimatter)
        actual-impulse (if (zero? power-used)
                         0
                         (* impulse (/ power-used power-required)))
        antimatter (- antimatter power-used)
        drag (drag velocity)
        accelerated-v (apply-impulse ms velocity heading actual-impulse)
        velocity (apply-drag drag accelerated-v)
        [px py] (vector/add [x y] (vector/scale ms velocity))]
    (assoc ship :x px :y py
                :velocity velocity
                :antimatter antimatter)))

(defn rotate-ship [ms ship]
  (let [{:keys [heading heading-setting]} ship
        total-rotation (rotation-direction heading heading-setting)
        rotation-step (* rotation-rate ms (sign total-rotation))
        rotation-step (if (< (abs total-rotation) (abs rotation-step))
                        total-rotation
                        rotation-step)
        new-heading (+ heading rotation-step)]
    (assoc ship :heading new-heading)))

(defn charge-shields [ms ship]
  (let [antimatter (:antimatter ship)
        shields (:shields ship)
        difference (- ship-shields shields)
        charge (min difference antimatter (* ms ship-shield-recharge-rate))
        ship (update ship :shields + charge)
        ship (update ship :antimatter - charge)]
    ship))

(defn update-ship [ms ship]
  (let [ship (warp-ship ms ship)
        ship (impulse-ship ms ship)
        ship (rotate-ship ms ship)
        ship (charge-shields ms ship)]
    ship))

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

(defn- set-strat-scale [{:keys [value]} ship]
  (assoc ship :strat-scale value))

(defn- dock-ship [_ ship]
  (assoc ship :antimatter ship-antimatter
              :kinetics ship-kinetics
              :torpedos ship-torpedos))

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
                      (handle-event :select-kinetic select-kinetic)
                      (handle-event :strat-scale set-strat-scale)
                      (handle-event :select-dock dock-ship))]
    ship))

(defn dockable? [ship bases]
  (let [distances (map #(distance [(:x ship) (:y ship)]
                                  [(:x %) (:y %)]) bases)
        closest (apply min distances)]
    (< closest ship-docking-distance)))
