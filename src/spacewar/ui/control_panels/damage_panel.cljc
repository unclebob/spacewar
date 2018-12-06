(ns spacewar.ui.control-panels.damage-panel
  (:require [spacewar.ui.protocols :as p]
            [spacewar.ui.config :as uic]
            [spacewar.ui.widgets.lcars :as lcars]
            [spacewar.ui.widgets.lights :as lights]
            [spacewar.ui.widgets.named-indicator :refer [->named-indicator]]
            ))

(defn- damage-level [damage]
  (condp >= damage
    0 0
    33 1
    66 2
    99 3
    4))

(deftype damage-panel [state]
  p/Drawable
  (get-state [_] state)

  (draw [_]
    (lcars/draw-banner state)
    (p/draw-elements state))

  (setup [_]
    (let [{:keys [x y w]} state
          indicator-w 20
          indicator-h 15
          indicator-gap 10
          col1 (+ x uic/stringer-width uic/button-gap)
          col2 (+ x (/ w 2) uic/button-gap (/ uic/stringer-width 2))
          warp-y (+ y uic/banner-width uic/button-gap)
          impulse-y (+ warp-y indicator-h indicator-gap)
          life-support-y (+ impulse-y indicator-h indicator-gap)
          hull-y warp-y
          sensor-y impulse-y
          weapons-y life-support-y
          height (+ uic/banner-width uic/button-gap indicator-h indicator-gap indicator-h indicator-gap indicator-h)
          colors [[0 255 0] [255 255 0] [255 150 0] [255 0 0] [0 0 0]]
          ]
      (damage-panel.
        (assoc state
          :h height
          :warp (p/setup
                  (->named-indicator
                    {:x col1
                     :y warp-y
                     :w indicator-w
                     :h indicator-h
                     :name "WRP"
                     :colors colors
                     :level 0
                     :draw-func lights/round-rectangle-light}))
          :impulse (p/setup
                     (->named-indicator
                       {:x col1
                        :y impulse-y
                        :w indicator-w
                        :h indicator-h
                        :name "IMP"
                        :colors colors
                        :level 0
                        :draw-func lights/round-rectangle-light}))
          :life-support (p/setup
                          (->named-indicator
                            {:x col1
                             :y life-support-y
                             :w indicator-w
                             :h indicator-h
                             :name "LIF"
                             :colors colors
                             :level 0
                             :draw-func lights/round-rectangle-light}))
          :hull (p/setup
                  (->named-indicator
                    {:x col2
                     :y hull-y
                     :w indicator-w
                     :h indicator-h
                     :name "HUL"
                     :colors colors
                     :level 0
                     :draw-func lights/round-rectangle-light}))
          :sensors (p/setup
                     (->named-indicator
                       {:x col2
                        :y sensor-y
                        :w indicator-w
                        :h indicator-h
                        :name "SEN"
                        :colors colors
                        :level 0
                        :draw-func lights/round-rectangle-light}))
          :weapons (p/setup
                     (->named-indicator
                       {:x col2
                        :y weapons-y
                        :w indicator-w
                        :h indicator-h
                        :name "WPN"
                        :colors colors
                        :level 0
                        :draw-func lights/round-rectangle-light}))
          :elements [:warp :impulse :life-support :hull :sensors :weapons]))))

  (update-state [_ world]
    (let [ship (:ship world)
          state (p/change-elements state [[:warp :level (damage-level (:warp-damage ship))]
                                          [:impulse :level (damage-level (:impulse-damage ship))]
                                          [:life-support :level (damage-level (:life-support-damage ship))]
                                          [:hull :level (damage-level (:hull-damage ship))]
                                          [:sensors :level (damage-level (:sensor-damage ship))]
                                          [:weapons :level (damage-level (:weapons-damage ship))]
                                          ])
          [state events] (p/update-elements state world)]
      (p/pack-update
        (damage-panel. state)
        events))))