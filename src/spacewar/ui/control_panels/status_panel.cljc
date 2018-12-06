(ns spacewar.ui.control-panels.status-panel
  (:require [spacewar.ui.protocols :as p]
            [spacewar.ui.config :as uic]
            [spacewar.game-logic.config :as glc]
            [spacewar.ui.widgets.horizontal-scale :refer [->h-scale]]))

(deftype status-panel [state]
  p/Drawable
  (get-state [_] state)
  (clone [_ clone-state] (status-panel. clone-state))
  (draw [_]
    (p/draw-elements state))

  (setup [_]
    (let [{:keys [x y w color mercury-color]} state
          scale-x x
          scale-h 20
          scale-w w
          scale-gap 10
          antimatter-y y
          dilithium-y (+ antimatter-y scale-h scale-gap)
          core-temp-y (+ dilithium-y scale-h scale-gap)
          shields-y (+ core-temp-y scale-h scale-gap)]
      (status-panel.
        (assoc state
          :antimatter (p/setup
                        (->h-scale
                          {:x scale-x
                           :y antimatter-y
                           :w scale-w
                           :h scale-h
                           :min 0
                           :max glc/ship-antimatter
                           :value glc/ship-antimatter
                           :name "ANTIMATTER"
                           :color color
                           :mercury-colors [[(* 0.1 glc/ship-antimatter) uic/red]
                                            [(* 0.25 glc/ship-antimatter) uic/orange]
                                             [glc/ship-antimatter mercury-color]]}))
          :dilithium (p/setup
                       (->h-scale
                         {:x scale-x
                          :y dilithium-y
                          :w scale-w
                          :h scale-h
                          :min 0
                          :max glc/ship-dilithium
                          :value glc/ship-dilithium
                          :name "DILITHIUM"
                          :color color
                          :mercury-colors [[(* 0.1) uic/red]
                                           [(* 0.2 glc/ship-dilithium) uic/orange]
                                           [glc/ship-dilithium mercury-color]]}))
          :core-temp (p/setup
                       (->h-scale
                         {:x scale-x
                          :y core-temp-y
                          :w scale-w
                          :h scale-h
                          :min 0
                          :max 100
                          :value 50
                          :name "CORE TEMP"
                          :color color
                          :mercury-colors [[70 mercury-color]
                                           [85 uic/orange]
                                           [100 uic/red]]}))
          :shields (p/setup
                     (->h-scale
                       {:x scale-x
                        :y shields-y
                        :w scale-w
                        :h scale-h
                        :min 0
                        :max glc/ship-shields
                        :value glc/ship-shields
                        :name "SHIELDS"
                        :color color
                        :mercury-colors [[(* 0.1 glc/ship-shields) uic/red]
                                         [(* 0.6 glc/ship-shields) uic/orange]
                                         [glc/ship-shields mercury-color]]}))
          :elements [:antimatter :dilithium :core-temp :shields]))))

  (update-state [_ world]
    (let [state (p/change-elements
                  state
                  [[:core-temp :value (->> world :ship :core-temp)]
                   [:shields :value (->> world :ship :shields)]
                   [:antimatter :value (->> world :ship :antimatter)]
                   [:dilithium :value (->> world :ship :dilithium)]])
          [state events] (p/update-elements state world)]
      (p/pack-update
        (status-panel. state)
        events)))
  )
