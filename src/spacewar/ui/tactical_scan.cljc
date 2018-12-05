(ns spacewar.ui.tactical-scan
  (:require [quil.core :as q #?@(:cljs [:include-macros true])]
            [spacewar.util :as util]
            [spacewar.ui.config :as uic]
            [spacewar.ui.icons :as icons]
            [spacewar.game-logic.config :as glc]
            [spacewar.ui.protocols :as p]
            [spacewar.geometry :as geo]
            [spacewar.vector :as vector]))

(defn- background-color [world]
  (let [time (:update-time world)
        ship (:ship world)
        game-over (:game-over world)
        {:keys [shields antimatter
                life-support-damage
                hull-damage warp-damage
                impulse-damage sensor-damage
                weapons-damage]} ship
        max-damage (max life-support-damage
                        hull-damage
                        warp-damage
                        impulse-damage
                        sensor-damage
                        weapons-damage)]
    (cond game-over uic/black
          (< (mod time 1000) 500) uic/black
          (> max-damage 0) uic/dark-red
          (< (/ antimatter glc/ship-antimatter) 0.1) uic/dark-red
          (< (/ shields glc/ship-shields) 0.6) uic/dark-yellow
          :else uic/black)))

(defn- draw-background [state]
  (let [{:keys [w h]} state]
    (apply q/fill (background-color (:world state)))
    (q/rect-mode :corner)
    (q/rect 0 0 w h)))

(defn- in-range [x y ship]
  (< (geo/distance [x y] [(:x ship) (:y ship)]) (/ glc/tactical-range 2)))

(defn- click->pos [tactical-scan click]
  (let [{:keys [x y w h world]} tactical-scan
        ship (:ship world)
        center (vector/add [(/ w 2) (/ h 2)] [x y])
        scale (/ glc/tactical-range w)
        click-delta (vector/subtract click center)
        tactical-click-delta (vector/scale scale click-delta)]
    (vector/add tactical-click-delta [(:x ship) (:y ship)])))

(defn- click->bearing [tactical-scan click]
  (let [tactical-loc (click->pos tactical-scan click)
        ship (-> tactical-scan :world :ship)
        ship-loc [(:x ship) (:y ship)]
        bearing (geo/angle-degrees ship-loc tactical-loc)
        ] bearing))

(defn- present-objects [state objects]
  (if (empty? objects)
    []
    (let [{:keys [w world]} state
          ship (:ship world)
          scale (/ w glc/tactical-range)
          presentables (->> objects
                            (filter #(in-range (:x %) (:y %) ship))
                            (map #(assoc % :x (- (:x %) (:x ship))
                                           :y (- (:y %) (:y ship))))
                            (map #(assoc % :x (* (:x %) scale)
                                           :y (* (:y %) scale))))]
      presentables)))

(defn- draw-objects-in [state objects draw]
  (let [{:keys [w h]} state
        presentable-objects (present-objects state objects)]
    (doseq [{:keys [x y] :as object} presentable-objects]
      (q/with-translation
        [(+ x (/ w 2)) (+ y (/ h 2))]
        (draw object)))))

(defn- draw-objects [state key draw]
  (draw-objects-in state (-> state :world key) draw))

(defn- draw-bases [state]
  (draw-objects state :bases icons/draw-base-icon))

(defn- draw-transports [state]
  (draw-objects state :transports icons/draw-transport-icon))

(defn- draw-stars [state]
  (draw-objects state :stars icons/draw-star-icon))

(defn- draw-klingon-and-shield [klingon]
  (icons/draw-klingon-shields (:shields klingon))
  (icons/draw-klingon-icon))

(defn- draw-klingons [state]
  (draw-objects state :klingons draw-klingon-and-shield))

(defn- draw-romulans [state]
  (draw-objects state :romulans icons/draw-romulan))

(defn target-arc [ship]
  (let [{:keys [selected-weapon
                target-bearing
                weapon-spread-setting]} ship
        range (condp = selected-weapon
                :phaser uic/phaser-target
                :torpedo uic/torpedo-target
                :kinetic uic/kinetic-target
                0)
        half-spread (max 3 (/ weapon-spread-setting 2))]
    [range
     (- target-bearing half-spread)
     (+ target-bearing half-spread)]))

(defn- draw-ship [state]
  (let [{:keys [w h]} state
        ship (->> state :world :ship)
        heading (or (:heading ship) 0)
        velocity (or (:velocity ship) [0 0])
        [vx vy] (vector/scale uic/velocity-vector-scale velocity)
        radians (q/radians heading)
        [tgt-radius start stop] (target-arc ship)
        start (q/radians start)
        stop (q/radians stop)
        draw-arc (not= (:selected-weapon ship) :none)]
    (q/with-translation
      [(/ w 2) (/ h 2)]
      (when draw-arc
        (q/no-stroke)
        (q/fill 255 255 255 50)
        (q/ellipse-mode :center)
        (q/arc 0 0 tgt-radius tgt-radius start stop #?(:clj :pie)))
      (icons/draw-ship-icon [vx vy] radians)
      )))

(defn- draw-torpedo-segment []
  (let [angle (rand 360)
        color (repeatedly 3 #(+ 128 (rand 127)))
        length (+ 5 (rand 5))
        radians (geo/->radians angle)
        [tx ty] (vector/from-angular length radians)]
    (apply q/stroke color)
    (q/line 0 0 tx ty)))

(defn- draw-torpedo [color _]
  (doseq [_ (range 3)]
    (draw-torpedo-segment))
  (apply q/fill color)
  (q/ellipse-mode :center)
  (q/ellipse 0 0 4 4))

(defn- draw-torpedo-shots [state]
  (draw-objects-in state
                   (filter #(= :torpedo (:type %)) (:shots (:world state)))
                   (partial draw-torpedo uic/white)))

(defn- draw-klingon-torpedo-shots [state]
  (draw-objects-in state
                   (filter #(= :klingon-torpedo (:type %)) (:shots (:world state)))
                   (partial draw-torpedo uic/green)))

(defn- draw-romulan-blast-shots [state]
  (draw-objects-in state
                   (filter #(= :romulan-blast (:type %)) (:shots (:world state)))
                   (partial icons/draw-romulan-shot (/ (:w state) glc/tactical-range))))

(defn- draw-kinetic-shot [color _]
  (q/ellipse-mode :center)
  (q/no-stroke)
  (apply q/fill color)
  (q/ellipse 0 0 3 3))

(defn- draw-kinetic-shots [state]
  (draw-objects-in state
                   (filter #(= :kinetic (:type %)) (:shots (:world state)))
                   (partial draw-kinetic-shot uic/kinetic-color)))

(defn- draw-klingon-kinetic-shots [state]
  (draw-objects-in state
                   (filter #(= :klingon-kinetic (:type %)) (:shots (:world state)))
                   (partial draw-kinetic-shot uic/klingon-kinetic-color)))

(defn- phaser-intensity [range]
  (let [intensity (* 255 (- 1 (/ range glc/phaser-range)))]
    [intensity intensity intensity]))

(defn- phaser-color [shot]
  (phaser-intensity (:range shot)))

(defn- klingon-phaser-color [_]
  uic/green)

(defn- draw-phaser-shot [color-function shot]
  (let [{:keys [bearing]} shot
        radians (geo/->radians bearing)
        [sx sy] (vector/from-angular uic/phaser-length radians)
        beam-color (color-function shot)]
    (apply q/stroke beam-color)
    (q/stroke-weight 3)
    (q/line 0 0 sx sy)))

(defn- draw-phaser-shots [state]
  (draw-objects-in state
                   (filter #(= :phaser (:type %)) (:shots (:world state)))
                   (partial draw-phaser-shot phaser-color)))

(defn- draw-klingon-phaser-shots [state]
  (draw-objects-in state
                   (filter #(= :klingon-phaser (:type %)) (:shots (:world state)))
                   (partial draw-phaser-shot klingon-phaser-color)))

(defn explosion-radius [age profile]
  (loop [profile profile radius 0 last-time 0]
    (let [{:keys [velocity until]} (first profile)]
      (cond (empty? profile)
            nil

            (> age until)
            (recur (rest profile)
                   (+ radius (* (- until last-time) velocity))
                   until)

            :else
            (+ radius (* velocity (- age last-time)))))))

(defn age-color [age profile]
  (loop [profile profile last-age 0 last-color [0 0 0]]
    (if (empty? profile)
      last-color
      (if (<= age (:until (first profile)))
        (let [profile-entry (first profile)
              {:keys [until colors]} profile-entry
              [c1 c2] colors
              diff (util/color-diff c2 c1)
              span (- until last-age)
              increment (util/color-scale diff (/ (- age last-age) span))]
          (util/color-add increment c1))
        (let [profile-entry (first profile)
              {:keys [until colors]} profile-entry]
          (recur (rest profile) until (last colors)))))))

(defn draw-fragment [fragment age fragment-color]
  (let [{:keys [velocity direction]} fragment
        radians (geo/->radians direction)
        velocity-vector (vector/from-angular velocity radians)
        [hx hy] (vector/scale age velocity-vector)
        [tx ty] (vector/scale (* age 0.9) velocity-vector)
        ]
    (q/stroke-weight 1)
    (apply q/stroke fragment-color)
    (q/line hx hy tx ty)))

(defn draw-explosion [state explosion]
  (let [{:keys [age type]} explosion
        {:keys [explosion-profile
                explosion-color-profile
                fragment-color-profile]} (type uic/explosion-profiles)]
    (let [fragments (present-objects state (:fragments explosion))
          radius (explosion-radius age explosion-profile)
          explosion-color (age-color age explosion-color-profile)
          fragment-color (age-color age fragment-color-profile)
          ex (- (rand 6) 3)
          ey (- (rand 6) 3)]
      (apply q/fill explosion-color)
      (q/ellipse-mode :center)
      (q/no-stroke)
      (q/ellipse ex ey radius radius)
      (doseq [fragment fragments]
        (draw-fragment fragment age fragment-color)))))

(defn- draw-explosions [state]
  (draw-objects state :explosions (partial draw-explosion state)))

(defn- draw-clouds [state]
  (draw-objects state :clouds icons/draw-cloud-icon))

(defn- draw-shots [state]
  (draw-phaser-shots state)
  (draw-torpedo-shots state)
  (draw-kinetic-shots state)
  (draw-klingon-kinetic-shots state)
  (draw-klingon-phaser-shots state)
  (draw-klingon-torpedo-shots state)
  (draw-romulan-blast-shots state))

(deftype tactical-scan [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y]} state]
      (q/with-translation
        [x y]
        (draw-background state)
        (draw-stars state)
        (draw-shots state)
        (draw-klingons state)
        (when (not (-> state :world :game-over))
          (draw-ship state))
        (draw-bases state)
        (draw-transports state)
        (draw-explosions state)
        (draw-clouds state)
        (draw-romulans state)
        )))

  (setup [_]
    (tactical-scan. state))

  (update-state [_ world]
    (let [{:keys [x y w h]} state
          last-left-down (:left-down state)
          mx (q/mouse-x)
          my (q/mouse-y)
          mouse-in (geo/inside-rect [x y w h] [mx my])
          left-down (and mouse-in (q/mouse-pressed?) (= :left (q/mouse-button)))
          state (assoc state :mouse-in mouse-in :left-down left-down)
          left-up (and (not left-down) last-left-down mouse-in)
          pressed? (q/key-pressed?)
          the-key (q/key-as-keyword)
          key (and pressed? the-key)
          event (if left-up
                  (condp = key
                    :p {:event :debug-position-ship :pos (click->pos state [mx my])}
                    :c {:event :debug-dilithium-cloud :pos (click->pos state [mx my])}
                    :r {:event :debug-resupply-ship}
                    :k {:event :debug-add-klingon :pos (click->pos state [mx my])}
                    :R {:event :debug-add-romulan :pos (click->pos state [mx my])}
                    {:event :weapon-direction :angle (click->bearing state [mx my])})
                  nil)]
      (p/pack-update (tactical-scan. (assoc state :world world)) event)))

  )