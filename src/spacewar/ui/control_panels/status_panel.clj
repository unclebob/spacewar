(ns spacewar.ui.control-panels.status-panel
  (:require [spacewar.ui.protocols :as p]
            [spacewar.ui.config :refer :all]
            [spacewar.game-logic.config :refer :all]
            (spacewar.ui.widgets [lcars :refer :all]
                                 [horizontal-scale :refer :all])))

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
                           :max ship-antimatter
                           :value ship-antimatter
                           :name "ANTIMATTER"
                           :color color
                           :mercury-color mercury-color}))
          :dilithium (p/setup
                       (->h-scale
                         {:x scale-x
                          :y dilithium-y
                          :w scale-w
                          :h scale-h
                          :min 0
                          :max ship-dilithium
                          :value ship-dilithium
                          :name "DILITHIUM"
                          :color color
                          :mercury-color mercury-color}))
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
                          :mercury-color mercury-color}))
          :shields (p/setup
                     (->h-scale
                       {:x scale-x
                        :y shields-y
                        :w scale-w
                        :h scale-h
                        :min 0
                        :max ship-shields
                        :value ship-shields
                        :name "SHIELDS"
                        :color color
                        :mercury-color mercury-color}))
          :elements [:antimatter :dilithium :core-temp :shields]))))

  (update-state [_ world]
    (let [state (p/change-elements
                  state
                  [[:core-temp :value (:ms world)]
                   [:shields :value (->> world :ship :shields)]
                   [:antimatter :value (->> world :ship :antimatter)]
                   [:dilithium :value (->> world :ship :dilithium)]])
          [state events] (p/update-elements state world)]
      (p/pack-update
        (status-panel. state)
        events)))
  )
