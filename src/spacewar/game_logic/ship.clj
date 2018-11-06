(ns spacewar.game-logic.ship
  (:require
    [spacewar.geometry :refer :all]
    [spacewar.vector :as vector]
    [spacewar.util :refer :all]
    [spacewar.game-logic.config :refer :all]
    [spacewar.game-logic.world :refer :all]
    [spacewar.game-logic.bases :as bases]
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
(s/def ::percent (s/and number? #(<= 0 % 100)))
(s/def ::life-support-damage ::percent)
(s/def ::hull-damage ::percent)
(s/def ::sensor-damage ::percent)
(s/def ::impulse-damage ::percent)
(s/def ::warp-damage ::percent)
(s/def ::weapons-damage ::percent)
(s/def ::destroyed boolean?)

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
                               ::kinetics ::torpedos
                               ::life-support-damage ::hull-damage
                               ::sensor-damage ::impulse-damage
                               ::warp-damage ::weapons-damage
                               ::strat-scale
                               ::destroyed]))

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
   :dilithium ship-dilithium
   :shields ship-shields
   :kinetics ship-kinetics
   :torpedos ship-torpedos
   :life-support-damage 0
   :hull-damage 0
   :sensor-damage 0
   :warp-damage 0
   :impulse-damage 0
   :weapons-damage 0
   :strat-scale 1
   :destroyed false}
  )

(defn heat-core [antimatter ship]
  (let [heat (* antimatter antimatter-to-heat)]
    (update ship :core-temp + heat)))

(defn dissipate-core-heat [ms ship]
  (let [dilithium (:dilithium ship)
        efficiency (Math/sqrt (/ dilithium ship-dilithium))
        dissipation-factor (- 1 (* efficiency dilithium-heat-dissipation))]
    (update ship :core-temp * (Math/pow dissipation-factor ms))))

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

(defn- calc-warp-charge [warp]
  (Math/pow warp 1.3))

(defn calc-dilithium-consumed [warp ms]
  (* warp ms ship-dilithium-consumption))

(defn- consume-dilithium [dilithium warp ms]
  (if (zero? warp)
    dilithium
    (max 0 (- dilithium (calc-dilithium-consumed warp ms)))))

(defn- warp-ship [ms ship]
  (if (zero? (:warp ship))
    ship
    (let [{:keys [x y warp warp-charge
                  heading antimatter
                  dilithium]} ship
          dilithium (consume-dilithium dilithium warp ms)
          power-required (* ms (warp-factor warp) warp-power)
          power-used (min power-required antimatter)
          antimatter (- antimatter power-used)
          actual-warp (* warp (/ power-used power-required))
          warp-charge-increment (* ms (calc-warp-charge actual-warp) warp-charge-rate)
          warp-efficiency (/ (- 100 (:warp-damage ship)) 100)
          warp-charge-increment (* warp-charge-increment warp-efficiency)
          warp-charge (+ warp-charge-increment warp-charge)
          warp-trigger (> warp-charge warp-threshold)
          warp-charge (if warp-trigger 0 warp-charge)
          radians (->radians heading)
          warp-vector (vector/from-angular warp-leap radians)
          [wx wy] (if warp-trigger
                    (vector/add [x y] warp-vector)
                    [x y])
          ship (heat-core power-used ship)]
      (assoc ship :x wx :y wy :warp-charge warp-charge
                  :antimatter antimatter
                  :dilithium dilithium))))

(defn- impulse-ship [ms ship]
  (let [{:keys [antimatter velocity heading impulse x y]} ship
        power-required (* ms impulse-power impulse)
        power-used (min power-required antimatter)
        actual-impulse (if (zero? power-required)
                         0
                         (* impulse (/ power-used power-required)))
        impulse-efficiency (/ (- 100 (:impulse-damage ship)) 100)
        actual-impulse (* actual-impulse impulse-efficiency)
        antimatter (- antimatter power-used)
        drag (drag velocity)
        accelerated-v (apply-impulse ms velocity heading actual-impulse)
        velocity (apply-drag drag accelerated-v)
        [px py] (vector/add [x y] (vector/scale ms velocity))
        ship (heat-core power-used ship)]
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
        new-heading (mod (+ heading rotation-step) 360)]
    (assoc ship :heading new-heading)))

(defn charge-shields [ms ship]
  (let [antimatter (:antimatter ship)
        shields (:shields ship)
        difference (- ship-shields shields)
        charge (min difference antimatter (* ms ship-shield-recharge-rate))
        ship (update ship :shields + charge)
        antimatter (* charge ship-shield-recharge-cost)
        ship (update ship :antimatter - antimatter)
        ship (heat-core antimatter ship)]
    ship))

(defn repair-capacity [ms ship]
  (let [{:keys [life-support-damage]} ship]
    (* ms ship-repair-capacity (- 100 life-support-damage) 0.01)))

(defn repair-ship [ms ship]
  (loop [systems [:life-support-damage
                  :hull-damage
                  :warp-damage
                  :sensor-damage
                  :weapons-damage
                  :impulse-damage
                  ]
         capacity (repair-capacity ms ship)
         ship ship]
    (if (or (zero? capacity)
            (empty? systems))
      ship
      (let [system (first systems)
            repair (min capacity (system ship))
            ship (update ship system - repair)]
        (recur (rest systems) (- capacity repair) ship)))))

(defn update-destruction [ship]
  (let [{:keys [life-support-damage
                hull-damage
                core-temp]} ship
        destroyed (or (>= life-support-damage 100)
                      (>= hull-damage 100)
                      (>= core-temp 100))]
    (if destroyed
      (assoc ship :destroyed true)
      ship)))

(defn update-ship [ms world]
  (let [ship (:ship world)
        ship (update-destruction ship)
        ship (if (:destroyed ship)
               ship
               (->> ship
                    (warp-ship ms)
                    (impulse-ship ms)
                    (rotate-ship ms)
                    (charge-shields ms)
                    (repair-ship ms)
                    (dissipate-core-heat ms)))]
    (assoc world :ship ship)))

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
    (assoc ship :selected-engine (if (= selected-engine :impulse) :none :impulse))))

(defn- select-warp [_ ship]
  (let [selected-engine (:selected-engine ship)]
    (assoc ship :selected-engine (if (= selected-engine :warp) :none :warp))))

(defn- select-front-view [_ ship]
  (assoc ship :selected-view :front-view))

(defn- select-strat-view [_ ship]
  (assoc ship :selected-view :strat-view))

(defn- select-tact-view [_ ship]
  (assoc ship :selected-view :tact-view))

(defn- select-phaser [_ {:keys [selected-weapon] :as ship}]
  (assoc ship
    :weapon-number-setting 1
    :selected-weapon (if (= selected-weapon :phaser) :none :phaser)))

(defn- select-torpedo [_ {:keys [selected-weapon] :as ship}]
  (assoc ship
    :weapon-number-setting 1
    :selected-weapon (if (= selected-weapon :torpedo) :none :torpedo)))

(defn- select-kinetic [_ {:keys [selected-weapon] :as ship}]
  (assoc ship
    :weapon-number-setting 1
    :selected-weapon (if (= selected-weapon :kinetic) :none :kinetic)))

(defn- set-strat-scale [{:keys [value]} ship]
  (assoc ship :strat-scale value))

(defn- in-range-of-base [ship base]
  (< (distance [(:x base) (:y base)]
               [(:x ship) (:y ship)])
     ship-docking-distance))

(defn- resupply-ship [ship base commodity maximum]
  (let [need (- maximum (ship commodity))
        supplied (int (min need (base commodity)))
        ship (update ship commodity + supplied)
        base (update base commodity - supplied)]
    [ship base]))

(defn- resupply-ship-from-bases [ship bases]
  (loop [ship ship bases bases processed-bases []]
    (if (empty? bases)
      [ship processed-bases]
      (let [base (first bases)
            [ship base] (resupply-ship ship base :antimatter ship-antimatter)
            [ship base] (resupply-ship ship base :dilithium ship-dilithium)
            [ship base] (resupply-ship ship base :torpedos ship-torpedos)
            [ship base] (resupply-ship ship base :kinetics ship-kinetics)]
        (recur ship (rest bases) (conj processed-bases base))))))

(defn dock-ship [_ world]
  (let [ship (:ship world)
        bases (:bases world)
        grouped-bases (group-by #(in-range-of-base ship %) bases)
        docked-bases (grouped-bases true)
        distant-bases (grouped-bases false)
        [ship docked-bases] (resupply-ship-from-bases ship docked-bases)
        ship (assoc ship
               :velocity [0 0]
               :warp 0
               :impulse 0)]
    (assoc world :ship ship :bases (concat distant-bases docked-bases))))

(defn- deploy-base [type world]
  (let [{:keys [x y antimatter dilithium] :as ship} (:ship world)]
    (if (and (> antimatter base-deployment-antimatter)
             (> dilithium base-deployment-dilithium))
      (let [bases (:bases world)
            base (bases/make-base [x y] type)
            bases (conj bases base)
            ship (-> ship
                     (update :antimatter - base-deployment-antimatter)
                     (update :dilithium - base-deployment-dilithium))]
        (assoc world :bases bases :ship ship))
      (add-message world "Insufficient resources sir." 2000))))

(defn- deploy-antimatter-factory [_ world]
  (deploy-base :antimatter-factory world))

(defn- deploy-dilithium-factory [_ world]
  (deploy-base :dilithium-factory world))

(defn- deploy-weapon-factory [_ world]
  (deploy-base :weapon-factory world))

(defn process-events [events world]
  (let [ship (:ship world)
        [_ ship] (->> [events ship]
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
                      )
        world (assoc world :ship ship)
        [_ world] (->> [events world]
                       (handle-event :select-dock dock-ship)
                       (handle-event :antimatter-factory deploy-antimatter-factory)
                       (handle-event :dilithium-factory deploy-dilithium-factory)
                       (handle-event :weapon-factory deploy-weapon-factory))]
    world))

(defn dockable? [ship bases]
  (let [distances (map #(distance [(:x ship) (:y ship)]
                                  [(:x %) (:y %)]) bases)
        closest (apply min distances)]
    (< closest ship-docking-distance)))

(defn deployment-classes [factory]
  (condp = factory
    :antimatter-factory #{:o :b :a}
    :dilithium-factory #{:k :m}
    :weapon-factory #{:f :g}))

(defn deployable? [factory ship stars]
  (let [{:keys [x y]} ship
        deployment-classes-set (deployment-classes factory)
        deployable-stars (filter #(deployment-classes-set (:class %)) stars)
        distances (map #(distance [x y] [(:x %) (:y %)]) deployable-stars)
        closest (if (empty? distances)
                  nil
                  (apply min distances))]
    (if closest
      (< closest ship-deploy-distance)
      false)))
