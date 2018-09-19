(ns spacewar.ui.control-panels.damage-panel
  (:require (spacewar.ui [protocols :as p]
                         [config :refer :all])
            [spacewar.ui.widgets.lcars :refer :all]
            [spacewar.ui.widgets.lights :refer :all]))


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
                  (->named-indicator
                    {:x col1
                     :y warp-y
                     :w indicator-w
                     :h indicator-h
                     :name "WRP"
                     :colors colors
                     :level 0
                     :draw-func round-rectangle-light}))
          :impulse (p/setup
                     (->named-indicator
                       {:x col1
                        :y impulse-y
                        :w indicator-w
                        :h indicator-h
                        :name "IMP"
                        :colors colors
                        :level 0
                        :draw-func round-rectangle-light}))
          :life-support (p/setup
                          (->named-indicator
                            {:x col1
                             :y life-support-y
                             :w indicator-w
                             :h indicator-h
                             :name "LIF"
                             :colors colors
                             :level 0
                             :draw-func round-rectangle-light}))
          :hull (p/setup
                  (->named-indicator
                    {:x col2
                     :y hull-y
                     :w indicator-w
                     :h indicator-h
                     :name "HUL"
                     :colors colors
                     :level 0
                     :draw-func round-rectangle-light}))
          :sensors (p/setup
                     (->named-indicator
                       {:x col2
                        :y sensor-y
                        :w indicator-w
                        :h indicator-h
                        :name "SEN"
                        :colors colors
                        :level 0
                        :draw-func round-rectangle-light}))
          :weapons (p/setup
                     (->named-indicator
                       {:x col2
                        :y weapons-y
                        :w indicator-w
                        :h indicator-h
                        :name "WPN"
                        :colors colors
                        :level 0
                        :draw-func round-rectangle-light}))
          :elements [:warp :impulse :life-support :hull :sensors :weapons]))))

  (update-state [_ commands]
    (let [[new-state events] (p/update-elements state commands)]
      (p/pack-update
        (damage-panel. new-state)
        events))))