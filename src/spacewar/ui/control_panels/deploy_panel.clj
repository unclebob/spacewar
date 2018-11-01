(ns spacewar.ui.control-panels.deploy-panel
  (:require (spacewar.ui [protocols :as p]
                         [config :refer :all])
            [spacewar.ui.widgets.lcars :refer :all]
            [spacewar.ui.widgets.button :refer :all]
            ))

(deftype deploy-panel [state]
  p/Drawable
  (get-state [_] state)

  (draw [_]
    (draw-lcars state)
    (p/draw-elements state))

  (setup [_]
    (let [{:keys [x y w button-color]} state
          button-w (- w stringer-width 10)
          am-x (+ x stringer-width 10)
          am-y (+ y banner-width button-gap)
          dl-x am-x
          dl-y (+ am-y button-h button-gap)
          wpn-x am-x
          wpn-y (+ dl-y button-h button-gap)
          height (+ banner-width button-gap button-h button-gap button-h button-gap button-h)]
      (deploy-panel.
        (assoc state
          :h height

          :antimatter-factory (p/setup
                                (->button
                                  {:x am-x
                                   :y am-y
                                   :w button-w
                                   :h button-h
                                   :name "AM BASE"
                                   :color button-color
                                   :left-up-event {:event :antimatter-factory}}))

          :dilithium-factory (p/setup
                               (->button
                                 {:x dl-x
                                  :y dl-y
                                  :w button-w
                                  :h button-h
                                  :name "DL BASE"
                                  :color button-color
                                  :left-up-event {:event :dilithium-factory}}))

          :weapon-factory (p/setup
                            (->button
                              {:x wpn-x
                               :y wpn-y
                               :w button-w
                               :h button-h
                               :name "WPN BASE"
                               :color button-color
                               :left-up-event {:event :weapon-factory}}))

          :elements [:antimatter-factory :dilithium-factory :weapon-factory]))))

  (update-state [_ world]
    (let [[state events] (p/update-elements state world)]
      (p/pack-update
        (deploy-panel. state)
        events))))
