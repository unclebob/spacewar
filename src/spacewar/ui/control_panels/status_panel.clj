(ns spacewar.ui.control-panels.status-panel
  (:require [spacewar.ui.protocols :as p]
            [spacewar.ui.widgets :as w]
            [spacewar.ui.config :refer :all]))

(deftype status-panel [state]
  p/Drawable
  (draw [_]
    (w/draw-bottom-lcars state)
    (p/draw-elements state))

  (setup [_]
    (let [{:keys [x y w color mercury-color]} state
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
                            :name "ANTIMATTER"
                            :color color
                            :mercury-color mercury-color}))
          :dilithium (p/setup
                       (w/->h-scale
                         {:x scale-x
                          :y dilithium-y
                          :w scale-w
                          :h scale-h
                          :min 0
                          :max 100
                          :value 50
                          :name "DILITHIUM"
                          :color color
                          :mercury-color mercury-color}))
          :core-temp (p/setup
                       (w/->h-scale
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
          :elements [:anti-matter :dilithium :core-temp]))))

  (update-state [_ commands]
    (let [[new-state events] (p/update-elements state commands)]
      (p/pack-update
        (status-panel. new-state)
        events))))
