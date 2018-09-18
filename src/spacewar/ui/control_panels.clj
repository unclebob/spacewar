(ns spacewar.ui.control-panels
  (:require [quil.core :as q]
            [spacewar.ui.protocols :as p]
            [spacewar.ui.widgets :as w]))

(def banner-width 40)
(def stringer-width 15)
(def button-gap 10)
(def button-h 40)
(def slider-width 50)

(defn lcars-points [state]
  (let [{:keys [x y w h]} state
        inverted (:inverted state false)]
    (if inverted
      {:a [(+ x w) y]
       :b [(+ x banner-width) y]
       :c [x (+ y banner-width)]
       :d [x (+ y h)]
       :e [(+ x stringer-width) (+ y h)]
       :f [(+ x stringer-width) (+ y banner-width stringer-width)]
       :g [(+ x stringer-width stringer-width) (+ y banner-width)]
       :h [(+ x w) (+ y banner-width)]
       :c1 [x y]
       :c2 [(+ x stringer-width) (+ y banner-width)]
       :label-position [(+ x w -10) (+ y 10)]}
      {:a [x y]
       :b [(+ x w (- banner-width)) y]
       :c [(+ x w) (+ y banner-width)]
       :d [(+ x w) (+ y h)]
       :e [(+ x w (- stringer-width)) (+ y h)]
       :f [(+ x w (- stringer-width)) (+ y banner-width stringer-width)]
       :g [(+ x w (- stringer-width) (- stringer-width)) (+ y banner-width)]
       :h [x (+ y banner-width)]
       :c1 [(+ x w) y]
       :c2 [(+ x w (- stringer-width)) (+ y banner-width)]
       :label-position [(+ x 10) (+ y 10)]})))

(defn draw-lcars [state]
  (let [inverted (:inverted state false)
        color (:color state)
        {:keys [a b c d e f g h c1 c2 label-position]} (lcars-points state)]
    (q/no-stroke)
    (apply q/fill color)
    (q/begin-shape)
    (apply q/vertex a)
    (apply q/vertex b)
    (apply q/quadratic-vertex (concat c1 c))
    (apply q/vertex d)
    (apply q/vertex e)
    (apply q/vertex f)
    (apply q/quadratic-vertex (concat c2 g))
    (apply q/vertex h)
    (apply q/vertex a)
    (q/end-shape)
    (q/fill 0 0 0)
    (q/text-size 24)
    (q/text-font (:lcars (q/state :fonts)))
    (q/text-align (if inverted :right :left) :top)
    (apply q/text (:name state) label-position)))

(defn draw-bottom-lcars [state]
  (let [{:keys [x y w h name color]} state]
    (q/no-stroke)
    (apply q/fill color)
    (q/rect-mode :corner)
    (q/rect x (+ y h (- banner-width)) w banner-width)
    (q/fill 0 0 0)
    (q/text-size 24)
    (q/text-font (:lcars (q/state :fonts)))
    (q/text-align :center :center)
    (q/text name (+ x (/ w 2)) (+ y h (/ banner-width -2)))
    )
  )

(deftype scan-panel [state]
  p/Drawable
  (draw [_]
    (draw-lcars state)
    (p/draw-elements state))

  (setup [_]
    (let [{:keys [x y w button-color]} state
          button-w (- w stringer-width 10)
          strategic-y (+ y banner-width button-gap)
          tactical-y (+ strategic-y button-h button-gap)
          front-view-y (+ tactical-y button-h button-gap)]
      (scan-panel.
        (assoc state
          :strategic (p/setup
                       (w/->button
                         {:x x
                          :y strategic-y
                          :w button-w
                          :h button-h
                          :name "STRAT"
                          :color button-color
                          :left-up-event {:event :strategic-scan}}))
          :tactical (p/setup
                      (w/->button
                        {:x x
                         :y tactical-y
                         :w button-w
                         :h button-h
                         :name "TACT"
                         :color button-color
                         :left-up-event {:event :tactical-scan}}))

          :front-view (p/setup
                        (w/->button
                          {:x x
                           :y front-view-y
                           :w button-w
                           :h button-h
                           :name "FRONT"
                           :color button-color
                           :left-up-event {:event :front-view}}))

          :elements [:strategic :tactical :front-view]))))

  (update-state [_ commands]
    (let [[new-state events] (p/update-elements state commands)]
      (p/pack-update
        (scan-panel. new-state)
        events))))

(deftype engine-panel [state]
  p/Drawable
  (clone [_ clone-state]
    (engine-panel. clone-state))
  (draw [_]
    (draw-lcars state)
    (p/draw-elements state))

  (setup [_]
    (let [{:keys [x y h color button-color]} state
          button-w 150
          warp-y (+ y banner-width button-gap)
          impulse-y (+ warp-y button-h button-gap)
          dock-y (+ impulse-y button-h button-gap)
          direction-x (+ x button-w button-gap)
          direction-y (+ y banner-width button-gap)
          direction-diameter (- h banner-width)
          power-x (+ direction-x direction-diameter button-gap)
          power-y direction-y
          power-w slider-width
          power-h (- h banner-width)]
      (engine-panel.
        (assoc state
          :warp (p/setup
                  (w/->button
                    {:x x
                     :y warp-y
                     :w button-w
                     :h button-h
                     :name "WARP"
                     :color button-color
                     :left-up-event {:event :select-warp}}))
          :impulse (p/setup
                     (w/->button
                       {:x x
                        :y impulse-y
                        :w button-w
                        :h button-h
                        :name "IMPULSE"
                        :color button-color
                        :left-up-event {:event :select-impulse}}))
          :dock (p/setup
                  (w/->button
                    {:x x
                     :y dock-y
                     :w button-w
                     :h button-h
                     :name "DOCK"
                     :color button-color
                     :left-up-event {:event :select-dock}}))

          :direction-selector (p/setup
                                (w/->direction-selector
                                  {:x direction-x
                                   :y direction-y
                                   :diameter direction-diameter
                                   :direction 180
                                   :color color
                                   :left-up-event {:event :engine-direction}}))
          :power-slider (p/setup
                          (w/->slider
                            {:x power-x
                             :y power-y
                             :w power-w
                             :h power-h
                             :color color
                             :thumb-color button-color
                             :min-val 0
                             :max-val 100
                             :value 50
                             :left-up-event {:event :engine-power}}))

          :elements [:warp :impulse :dock :direction-selector :power-slider]))))

  (update-state [_ commands-and-state]
    (let [commands (:commands commands-and-state)
          direction-command (p/get-command :set-engine-direction commands)
          power-command (p/get-command :set-engine-power commands)
          commanded-state (cond
                            (some? direction-command)
                            (p/assoc-element state :direction-selector :direction (:angle direction-command))

                            (some? power-command)
                            (p/assoc-element state :power-slider :value (:power power-command))

                            :else state)
          [new-state events] (p/update-elements commanded-state commands-and-state)]
      (p/pack-update
        (engine-panel. new-state)
        events))))

(deftype weapons-panel [state]
  p/Drawable
  (draw [_]
    (draw-lcars state)
    (p/draw-elements state))

  (setup [_]
    (let [{:keys [x y h color button-color]} state
          button-w 150
          button-x (+ x stringer-width button-gap)
          torpedo-y (+ y banner-width button-gap)
          kinetic-y (+ torpedo-y button-h button-gap)
          phaser-y (+ kinetic-y button-h button-gap)
          direction-x (+ button-x button-w button-gap)
          direction-y (+ y banner-width button-gap)
          direction-diameter (- h banner-width)
          number-x (+ direction-x direction-diameter button-gap)
          number-y direction-y
          number-w slider-width
          number-h (- h banner-width)
          spread-x (+ number-x number-w button-gap)
          spread-y number-y
          spread-w slider-width
          spread-h number-h]
      (weapons-panel.
        (assoc state
          :torpedo (p/setup
                     (w/->button
                       {:x button-x
                        :y torpedo-y
                        :w button-w
                        :h button-h
                        :name "TORPEDO"
                        :color button-color
                        :left-up-event {:event :select-torpedo}}))
          :kinetic (p/setup
                     (w/->button
                       {:x button-x
                        :y kinetic-y
                        :w button-w
                        :h button-h
                        :name "KINETIC"
                        :color button-color
                        :left-up-event {:event :select-kinetic}}))
          :phaser (p/setup
                    (w/->button
                      {:x button-x
                       :y phaser-y
                       :w button-w
                       :h button-h
                       :name "PHASER"
                       :color button-color
                       :left-up-event {:event :select-phaser}}))

          :direction-selector (p/setup
                                (w/->direction-selector
                                  {:x direction-x
                                   :y direction-y
                                   :diameter direction-diameter
                                   :direction 180
                                   :color color
                                   :left-up-event {:event :weapon-direction}}))

          :number-slider (p/setup
                                    (w/->slider
                                      {:x number-x
                                       :y number-y
                                       :w number-w
                                       :h number-h
                                       :color color
                                       :thumb-color button-color
                                       :min-val 1
                                       :max-val 5
                                       :value 1
                                       :left-up-event {:event :weapon-number}}))

          :spread-slider (p/setup
                                    (w/->slider
                                      {:x spread-x
                                       :y spread-y
                                       :w spread-w
                                       :h spread-h
                                       :color color
                                       :thumb-color button-color
                                       :min-val 0
                                       :max-val 20
                                       :value 0
                                       :left-up-event {:event :weapon-spread}}))

          :elements [:torpedo :kinetic :phaser :direction-selector :number-slider :spread-slider]))))

  (update-state [_ commands-and-state]
    (let [commands (:commands commands-and-state)
          direction-command (p/get-command :set-weapon-direction commands)
          number-command (p/get-command :set-weapon-number commands)
          spread-command (p/get-command :set-weapon-spread commands)
          commanded-state (cond
                            (some? direction-command)
                            (p/assoc-element state :direction-selector :direction (:angle direction-command))

                            (some? number-command)
                            (p/assoc-element state :number-slider :value (:number number-command))

                            (some? spread-command)
                            (p/assoc-element state :spread-slider :value (:spread spread-command))

                            :else state)
          [new-state events] (p/update-elements commanded-state commands-and-state)]
      (p/pack-update
        (weapons-panel. new-state)
        events))))

(deftype damage-panel [state]
  p/Drawable
  (draw [_]
    (draw-lcars state)
    (p/draw-elements state))

  (setup [_]
    (let [{:keys [x y w]} state
          indicator-w 20
          indicator-h 15
          indicator-gap 50
          col1 (+ x stringer-width button-gap)
          col2 (+ x (/ w 2) button-gap (/ stringer-width 2))
          warp-y (+ y banner-width button-gap)
          impulse-y (+ warp-y indicator-h indicator-gap)
          life-support-y (+ impulse-y indicator-h indicator-gap)
          hull-y warp-y
          sensor-y impulse-y
          weapons-y life-support-y
          colors [[0 255 0] [255 255 0] [255 255 100] [255 0 0]]
          ]
      (damage-panel.
        (assoc state
          :warp (p/setup
                  (w/->named-indicator
                    {:x col1
                     :y warp-y
                     :w indicator-w
                     :h indicator-h
                     :name "WRP"
                     :colors colors
                     :level 0
                     :draw-func w/rectangle-light}))
          :impulse (p/setup
                     (w/->named-indicator
                       {:x col1
                        :y impulse-y
                        :w indicator-w
                        :h indicator-h
                        :name "IMP"
                        :colors colors
                        :level 0
                        :draw-func w/rectangle-light}))
          :life-support (p/setup
                          (w/->named-indicator
                            {:x col1
                             :y life-support-y
                             :w indicator-w
                             :h indicator-h
                             :name "LIF"
                             :colors colors
                             :level 0
                             :draw-func w/rectangle-light}))
          :hull (p/setup
                  (w/->named-indicator
                    {:x col2
                     :y hull-y
                     :w indicator-w
                     :h indicator-h
                     :name "HUL"
                     :colors colors
                     :level 0
                     :draw-func w/rectangle-light}))
          :sensors (p/setup
                     (w/->named-indicator
                       {:x col2
                        :y sensor-y
                        :w indicator-w
                        :h indicator-h
                        :name "SEN"
                        :colors colors
                        :level 0
                        :draw-func w/rectangle-light}))
          :weapons (p/setup
                     (w/->named-indicator
                       {:x col2
                        :y weapons-y
                        :w indicator-w
                        :h indicator-h
                        :name "WPN"
                        :colors colors
                        :level 0
                        :draw-func w/rectangle-light}))
          :elements [:warp :impulse :life-support :hull :sensors :weapons]))))

  (update-state [_ commands]
    (let [[new-state events] (p/update-elements state commands)]
      (p/pack-update
        (damage-panel. new-state)
        events))))

(deftype status-panel [state]
  p/Drawable
  (draw [_]
    (draw-bottom-lcars state)
    (p/draw-elements state))

  (setup [_]
    (let [{:keys [x y w]} state
          scale-x x
          scale-h 20
          scale-w w
          scale-gap 10
          antimatter-y y
          dilithium-y (+ antimatter-y scale-h scale-gap)
          core-temp-y (+ dilithium-y scale-h scale-gap)]
      (status-panel.
        (assoc state
          :anti-matter (p/setup
                         (w/->h-scale
                           {:x scale-x
                            :y antimatter-y
                            :w scale-w
                            :h scale-h
                            :min 0
                            :max 100
                            :value 50
                            :name "ANTIMATTER"}))
          :dilithium (p/setup
                       (w/->h-scale
                         {:x scale-x
                          :y dilithium-y
                          :w scale-w
                          :h scale-h
                          :min 0
                          :max 100
                          :value 50
                          :name "DILITHIUM"}))
          :core-temp (p/setup
                       (w/->h-scale
                         {:x scale-x
                          :y core-temp-y
                          :w scale-w
                          :h scale-h
                          :min 0
                          :max 100
                          :value 50
                          :name "CORE TEMP"}))
          :elements [:anti-matter :dilithium :core-temp]))))

  (update-state [_ commands]
    (let [[new-state events] (p/update-elements state commands)]
      (p/pack-update
        (status-panel. new-state)
        events))))

