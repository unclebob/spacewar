(ns spacewar.ui.control-panels.scan-panel
  (:require [spacewar.ui.protocols :as p]
            [spacewar.ui.config :as uic]
            [spacewar.ui.widgets.button :refer [->button]]
            [spacewar.ui.widgets.slider :refer [->slider]]
            [spacewar.ui.widgets.lcars :as lcars]))

(defn- button-color [selected button]
  (if (= selected button)
    uic/scan-panel-selection-color
    uic/scan-panel-button-color))

(deftype scan-panel [state]
  p/Drawable
  (draw [_]
    (lcars/draw-banner state)
    (p/draw-elements state))

  (setup [_]
    (let [{:keys [x y w h button-color color]} state
          bottom (+ y h)
          button-w (- w uic/stringer-width 10)
          strategic-y (+ y uic/banner-width uic/button-gap)
          tactical-y (+ strategic-y uic/button-h uic/button-gap)
          front-view-y (+ tactical-y uic/button-h uic/button-gap)
          strat-scale-slider-x (- (+ x w) uic/stringer-width uic/slider-width uic/button-gap)
          strat-scale-slider-y (+ front-view-y uic/button-h uic/button-gap)
          strat-scale-slider-h (- bottom front-view-y uic/button-h uic/button-gap)]
      (scan-panel.
        (assoc state
          :strategic (p/setup
                       (->button
                         {:x x
                          :y strategic-y
                          :w button-w
                          :h uic/button-h
                          :name "STRAT"
                          :color button-color
                          :left-up-event {:event :strategic-scan}}))
          :tactical (p/setup
                      (->button
                        {:x x
                         :y tactical-y
                         :w button-w
                         :h uic/button-h
                         :name "TACT"
                         :color button-color
                         :left-up-event {:event :tactical-scan}}))

          :front-view (p/setup
                        (->button
                          {:x x
                           :y front-view-y
                           :w button-w
                           :h uic/button-h
                           :name "FRONT"
                           :color button-color
                           :left-up-event {:event :front-view}}))
          :strat-scale (p/setup
                         (->slider
                           {:x strat-scale-slider-x
                            :y strat-scale-slider-y
                            :w uic/slider-width
                            :h strat-scale-slider-h
                            :color color
                            :thumb-color button-color
                            :min-val 1
                            :max-val 10
                            :value 1
                            :disabled :true
                            :left-up-event {:event :strat-scale}}))

          :elements [:strategic :tactical :front-view :strat-scale]))))

  (update-state [_ world]
    (let [ship (:ship world)
          {:keys [selected-view strat-scale]} ship
          tact-color (button-color selected-view :tact-view)
          strat-color (button-color selected-view :strat-view)
          front-color (button-color selected-view :front-view)
          scale-disabled (not= selected-view :strat-view)
          state (p/change-elements state [[:front-view :color front-color]
                                          [:tactical :color tact-color]
                                          [:strategic :color strat-color]
                                          [:strat-scale :disabled scale-disabled]
                                          [:strat-scale :value strat-scale]])
          [state events] (p/update-elements state world)]
      (p/pack-update
        (scan-panel. state)
        events))))