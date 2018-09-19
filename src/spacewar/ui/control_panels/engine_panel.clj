(ns spacewar.ui.control-panels.engine-panel
  (:require [spacewar.ui.protocols :as p]
            [spacewar.ui.widgets :as w]
            [spacewar.ui.config :refer :all]))

(deftype engine-panel [state]
  p/Drawable
  (clone [_ clone-state]
    (engine-panel. clone-state))

  (draw [_]
    (w/draw-lcars state)
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
