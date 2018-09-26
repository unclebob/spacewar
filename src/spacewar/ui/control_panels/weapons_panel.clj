(ns spacewar.ui.control-panels.weapons-panel
  (:require [spacewar.ui.protocols :as p]
            [spacewar.ui.config :refer :all]
            [spacewar.ui.widgets.lcars :refer :all]
            [spacewar.ui.widgets.button :refer :all]
            [spacewar.ui.widgets.direction-selector :refer :all]
            [spacewar.ui.widgets.slider :refer :all]
            [spacewar.ui.widgets.engage :refer :all]
            [quil.core :as q]))
(defn- button-color [selected button]
  (if (= selected button)
    weapons-panel-selection-color
    weapons-panel-button-color))

(deftype weapons-panel [state]
  p/Drawable
  (draw [_]
    (let [{:keys [bearing-label-x number-label-x spread-label-x label-y]} state]
      (draw-lcars state)
      (p/draw-elements state)
      (apply q/fill black)
      (q/text-align :center :bottom)
      (q/text-font (:lcars (q/state :fonts)) 14)
      (q/text "BEARING" bearing-label-x label-y)
      (q/text "SPREAD" spread-label-x label-y)
      (q/text "NUM" number-label-x label-y)))

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
          fire-h number-h
          bearing-label-x (+ direction-x (/ direction-diameter 2))
          number-label-x (+ number-x (/ number-w 2))
          spread-label-x (+ spread-x (/ spread-w 2))
          label-y (+ y banner-width)]
      (weapons-panel.
        (assoc state
          :bearing-label-x bearing-label-x
          :number-label-x number-label-x
          :spread-label-x spread-label-x
          :label-y label-y
          :torpedo (p/setup
                     (->button
                       {:x button-x
                        :y torpedo-y
                        :w button-w
                        :h button-h
                        :name "TORPEDO"
                        :color button-color
                        :left-up-event {:event :select-torpedo}}))
          :kinetic (p/setup
                     (->button
                       {:x button-x
                        :y kinetic-y
                        :w button-w
                        :h button-h
                        :name "KINETIC"
                        :color button-color
                        :left-up-event {:event :select-kinetic}}))
          :phaser (p/setup
                    (->button
                      {:x button-x
                       :y phaser-y
                       :w button-w
                       :h button-h
                       :name "PHASER"
                       :color button-color
                       :left-up-event {:event :select-phaser}}))

          :direction-selector (p/setup
                                (->direction-selector
                                  {:x direction-x
                                   :y direction-y
                                   :diameter direction-diameter
                                   :direction 0
                                   :color color
                                   :left-up-event {:event :weapon-direction}}))

          :number-slider (p/setup
                           (->slider
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
                           (->slider
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
                  (->engage
                    {:x fire-x
                     :y fire-y
                     :w fire-w
                     :h fire-h
                     :color color
                     :activation-color button-color
                     :name "FIRE"
                     :disabled true
                     :left-up-event {:event :weapon-fire}}))

          :elements [:torpedo :kinetic :phaser :direction-selector :number-slider :spread-slider :fire]))))

  (update-state [_ world]
    (let [ship (:ship world)
          {:keys [target-bearing selected-weapon
                  weapon-number-setting weapon-spread-setting]} ship
          phaser-button-color (button-color selected-weapon :phaser)
          torpedo-button-color (button-color selected-weapon :torpedo)
          kinetic-button-color (button-color selected-weapon :kinetic)
          weapon-disabled (= selected-weapon :none)

          state (p/change-elements state [[:direction-selector :direction target-bearing]
                                          [:number-slider :value weapon-number-setting]
                                          [:spread-slider :value weapon-spread-setting]
                                          [:phaser :color phaser-button-color]
                                          [:torpedo :color torpedo-button-color]
                                          [:kinetic :color kinetic-button-color]
                                          [:fire :disabled weapon-disabled]])
          [state events] (p/update-elements state world)]
      (p/pack-update
        (weapons-panel. state)
        events))))
