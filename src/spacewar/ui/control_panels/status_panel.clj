(ns spacewar.ui.control-panels.status-panel
  (:require [spacewar.ui.protocols :as p]
            [spacewar.ui.config :refer :all]
            (spacewar.ui.widgets [lcars :refer :all]
                                 [horizontal-scale :refer :all])))

(deftype status-panel [state]
  p/Drawable
  (get-state [_] state)
  (clone [_ clone-state] (status-panel. clone-state))
  (draw [_]
    (draw-bottom-lcars state)
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
                         (->h-scale
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
                       (->h-scale
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
          :elements [:anti-matter :dilithium :core-temp]))))

  (update-state [_ world]
    (let [state (p/change-elements state [[:core-temp :value (:ms world)]])
          [state events] (p/update-elements state world)]
      (p/pack-update
        (status-panel. state)
        events)))
  )
