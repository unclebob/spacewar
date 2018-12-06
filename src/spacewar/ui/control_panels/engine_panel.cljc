(ns spacewar.ui.control-panels.engine-panel
  (:require [spacewar.ui.protocols :as p]
            [spacewar.ui.config :as uic]
            [spacewar.ui.widgets.lcars :as lcars]
            [spacewar.ui.widgets.button :refer [->button]]
            [spacewar.ui.widgets.direction-selector :refer [->direction-selector]]
            [spacewar.ui.widgets.slider :refer [->slider]]
            [spacewar.ui.widgets.engage :refer [->engage]]
            [spacewar.geometry :as geo]
            [spacewar.vector :as vector]
            [spacewar.game-logic.ship :as ship]
            [quil.core :as q #?@(:cljs [:include-macros true])]))

(deftype engine-panel [state]
  p/Drawable
  (clone [_ clone-state]
    (engine-panel. clone-state))

  (draw [_]
    (let [{:keys [heading-label-x power-label-x label-y]} state]
      (lcars/draw-banner state)
      (p/draw-elements state)
      (apply q/fill uic/black)
      (q/text-align :center :bottom)
      (q/text-font (:lcars (q/state :fonts)) 14)
      (q/text "HEADING" heading-label-x label-y)
      (q/text "POWER" power-label-x label-y)))

  (setup [_]
    (let [{:keys [x y w h color button-color]} state
          button-w 150
          warp-y (+ y uic/banner-width uic/button-gap)
          impulse-y (+ warp-y uic/button-h uic/button-gap)
          dock-y (+ impulse-y uic/button-h uic/button-gap)
          direction-x (+ x button-w uic/button-gap)
          direction-y (+ y uic/banner-width uic/button-gap)
          direction-diameter (- h uic/banner-width)
          power-x (+ direction-x direction-diameter uic/button-gap)
          power-y direction-y
          power-w uic/slider-width
          power-h (- h uic/banner-width)
          engage-x (- (+ x w) uic/stringer-width uic/button-gap uic/engage-width)
          engage-y direction-y
          engage-w uic/engage-width
          engage-h (- h uic/banner-width)

          heading-label-x (+ direction-x (/ direction-diameter 2))
          power-label-x (+ power-x (/ power-w 2))
          label-y (+ y uic/banner-width)]

      (engine-panel.
        (assoc state
          :heading-label-x heading-label-x
          :power-label-x power-label-x
          :label-y label-y
          :warp (p/setup
                  (->button
                    {:x x
                     :y warp-y
                     :w button-w
                     :h uic/button-h
                     :name "WARP"
                     :color button-color
                     :left-up-event {:event :select-warp}}))
          :impulse (p/setup
                     (->button
                       {:x x
                        :y impulse-y
                        :w button-w
                        :h uic/button-h
                        :name "IMPULSE"
                        :color button-color
                        :left-up-event {:event :select-impulse}}))
          :dock (p/setup
                  (->button
                    {:x x
                     :y dock-y
                     :w button-w
                     :h uic/button-h
                     :name "DOCK"
                     :color button-color
                     :disabled true
                     :left-up-event {:event :select-dock}}))

          :direction-selector (p/setup
                                (->direction-selector
                                  {:x direction-x
                                   :y direction-y
                                   :diameter direction-diameter
                                   :direction 0
                                   :color color
                                   :left-up-event {:event :engine-direction}}))
          :power-slider (p/setup
                          (->slider
                            {:x power-x
                             :y power-y
                             :w power-w
                             :h power-h
                             :color color
                             :thumb-color button-color
                             :min-val 0
                             :max-val 10
                             :value 0
                             :left-up-event {:event :engine-power}}))

          :engage (p/setup
                    (->engage
                      {:x engage-x
                       :y engage-y
                       :w engage-w
                       :h engage-h
                       :name "ENGAGE"
                       :color color
                       :activation-color button-color
                       :left-up-event {:event :engine-engage}
                       :disabled true}))

          :elements [:warp :impulse :dock :direction-selector :power-slider :engage]))))

  (update-state [_ world]
    (let [{:keys [bases ship]} world
          {:keys [heading heading-setting velocity selected-engine engine-power-setting warp]} ship
          selected-engine (or selected-engine :none)
          warp-color (if (= selected-engine :warp)
                       uic/engine-panel-selection-color
                       uic/engine-panel-button-color)
          impulse-color (if (= selected-engine :impulse)
                          uic/engine-panel-selection-color
                          uic/engine-panel-button-color)
          engage-disabled (= selected-engine :none)
          dockable (ship/dockable? ship bases)

          state (p/change-elements
                  state
                  [[:dock :disabled (not dockable)]
                   [:direction-selector :pointer2 heading]
                   [:impulse :status (str (geo/round (* 20 (vector/magnitude velocity))))]
                   [:impulse :color impulse-color]
                   [:warp :color warp-color]
                   [:power-slider :value engine-power-setting]
                   [:direction-selector :direction heading-setting]
                   [:engage :disabled engage-disabled]
                   [:warp :status (str warp)]])

          [state events] (p/update-elements state world)]
      (p/pack-update
        (engine-panel. state)
        events))))
