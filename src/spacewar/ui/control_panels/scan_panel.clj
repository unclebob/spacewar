(ns spacewar.ui.control-panels.scan-panel
  (:require [spacewar.ui.protocols :as p]
            [spacewar.ui.config :refer :all]
            [spacewar.ui.widgets.button :refer :all]
            [spacewar.ui.widgets.lcars :refer :all]))

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
                       (->button
                         {:x x
                          :y strategic-y
                          :w button-w
                          :h button-h
                          :name "STRAT"
                          :color button-color
                          :left-up-event {:event :strategic-scan}}))
          :tactical (p/setup
                      (->button
                        {:x x
                         :y tactical-y
                         :w button-w
                         :h button-h
                         :name "TACT"
                         :color button-color
                         :left-up-event {:event :tactical-scan}}))

          :front-view (p/setup
                        (->button
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