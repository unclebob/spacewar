(ns spacewar.ui.control-panels.weapons-panel
  (:require [spacewar.ui.protocols :as p]
            [spacewar.ui.widgets :as w]
            [spacewar.ui.config :refer :all]))

(deftype weapons-panel [state]
  p/Drawable
  (draw [_]
    (w/draw-lcars state)
    (p/draw-elements state))

  (setup [_]
    (let [{:keys [x y h color button-color]} state
          button-w 150
          button-x (+ x stringer-width button-gap)
          torpedo-y (+ y banner-width button-gap)
          kinetic-y (+ torpedo-y button-h button-gap)
          phaser-y (+ kinetic-y button-h button-gap)
          direction-x (+ button-x button-w button-gap)
          direction-y (+ y banner-width button-gap)
          direction-diameter (- h banner-width)
          number-x (+ direction-x direction-diameter button-gap)
          number-y direction-y
          number-w slider-width
          number-h (- h banner-width)
          spread-x (+ number-x number-w button-gap)
          spread-y number-y
          spread-w slider-width
          spread-h number-h
          fire-x (+ spread-x spread-w button-gap)
          fire-y number-y
          fire-w engage-width
          fire-h number-h]
      (weapons-panel.
        (assoc state
          :torpedo (p/setup
                     (w/->button
                       {:x button-x
                        :y torpedo-y
                        :w button-w
                        :h button-h
                        :name "TORPEDO"
                        :color button-color
                        :left-up-event {:event :select-torpedo}}))
          :kinetic (p/setup
                     (w/->button
                       {:x button-x
                        :y kinetic-y
                        :w button-w
                        :h button-h
                        :name "KINETIC"
                        :color button-color
                        :left-up-event {:event :select-kinetic}}))
          :phaser (p/setup
                    (w/->button
                      {:x button-x
                       :y phaser-y
                       :w button-w
                       :h button-h
                       :name "PHASER"
                       :color button-color
                       :left-up-event {:event :select-phaser}}))

          :direction-selector (p/setup
                                (w/->direction-selector
                                  {:x direction-x
                                   :y direction-y
                                   :diameter direction-diameter
                                   :direction 180
                                   :color color
                                   :left-up-event {:event :weapon-direction}}))

          :number-slider (p/setup
                           (w/->slider
                             {:x number-x
                              :y number-y
                              :w number-w
                              :h number-h
                              :color color
                              :thumb-color button-color
                              :min-val 1
                              :max-val 5
                              :value 1
                              :left-up-event {:event :weapon-number}}))

          :spread-slider (p/setup
                           (w/->slider
                             {:x spread-x
                              :y spread-y
                              :w spread-w
                              :h spread-h
                              :color color
                              :thumb-color button-color
                              :min-val 0
                              :max-val 20
                              :value 0
                              :left-up-event {:event :weapon-spread}}))
          :fire (p/setup
                  (w/->engage
                    {:x fire-x
                     :y fire-y
                     :w fire-w
                     :h fire-h
                     :color color
                     :activation-color button-color
                     :name "FIRE"
                     :left-up-event {:event :weapon-fire}}))

          :elements [:torpedo :kinetic :phaser :direction-selector :number-slider :spread-slider :fire]))))

  (update-state [_ commands-and-state]
    (let [commands (:commands commands-and-state)
          direction-command (p/get-command :set-weapon-direction commands)
          number-command (p/get-command :set-weapon-number commands)
          spread-command (p/get-command :set-weapon-spread commands)
          commanded-state (cond
                            (some? direction-command)
                            (p/assoc-element state :direction-selector :direction (:angle direction-command))

                            (some? number-command)
                            (p/assoc-element state :number-slider :value (:number number-command))

                            (some? spread-command)
                            (p/assoc-element state :spread-slider :value (:spread spread-command))

                            :else state)
          [new-state events] (p/update-elements commanded-state commands-and-state)]
      (p/pack-update
        (weapons-panel. new-state)
        events))))
