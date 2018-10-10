(ns spacewar.core-test
  (:require [midje.sweet :refer :all]
            [spacewar.core :refer :all]
            [spacewar.vector-test :as vt]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.test-mother :as mom]))

(tabular
  (fact
    "about view selection events"
    (process-events [{:event ?event}]
                    {:ship {}}) => {:ship {:selected-view ?view}})
  ?event ?view
  :strategic-scan :strat-view
  :tactical-scan :tact-view
  :front-view :front-view
  )

(facts
  "about engine panel events"
  (tabular
    (fact
      "about engine selection events"
      (process-events
        [{:event ?event}]
        {:ship {:selected-engine ?old-engine}})
      => {:ship {:selected-engine ?new-engine}})
    ?event ?old-engine ?new-engine
    :select-warp :none :warp
    :select-warp :impulse :warp
    :select-warp :warp :none
    :select-impulse :none :impulse
    :select-impulse :warp :impulse
    :select-impulse :impulse :none)

  (fact
    "engine direction event"
    (process-events
      [{:event :engine-direction :angle ..angle..}]
      {:ship {}})
    => {:ship {:heading-setting ..angle..}})

  (fact
    "engine power event"
    (process-events
      [{:event :engine-power :value ..power..}]
      {:ship {}})
    => {:ship {:engine-power-setting ..power..}})

  (fact
    "engine engage event"
    (process-events
      [{:event :engine-engage}]
      {:ship {:engine-power-setting ..power..
              :selected-engine ..engine..}})
    => {:ship {:engine-power-setting 0
               :selected-engine ..engine..
               ..engine.. ..power..}}))


(facts
  "about weapons panel events"
  (tabular
    (fact
      "about weapon selection events"
      (process-events
        [{:event ?event}]
        {:ship {:selected-weapon ?old-weapon}})
      => {:ship {:selected-weapon ?new-weapon}})
    ?event ?old-weapon ?new-weapon
    :select-phaser :none :phaser
    :select-phaser :phaser :none
    :select-phaser :torpedo :phaser
    :select-phaser :kinetic :phaser
    :select-torpedo :none :torpedo
    :select-torpedo :torpedo :none
    :select-torpedo :phaser :torpedo
    :select-torpedo :kinetic :torpedo
    :select-kinetic :none :kinetic
    :select-kinetic :kinetic :none
    :select-kinetic :phaser :kinetic
    :select-kinetic :torpedo :kinetic)

  (fact
    "weapon direction event"
    (process-events
      [{:event :weapon-direction :angle ..angle..}]
      {:ship {}})
    => {:ship {:target-bearing ..angle..}})

  (tabular (fact
    "weapon number event"
    (process-events
      [{:event :weapon-number :value ?number}]
      {:ship {}})
    => {:ship {:weapon-number-setting ?number :weapon-spread-setting ?spread}})
           ?number ?spread
           1 0
           2 1
           3 1)

  (fact
    "weapon spread event"
    (process-events
      [{:event :weapon-spread :value ..spread..}]
      {:ship {}})
    => {:ship {:weapon-spread-setting ..spread..}})

  (fact
    "fire one phaser event"
    (let [world (process-events
                  [{:event :weapon-fire}]
                  {:ship {:x ..x.. :y ..y.. :selected-weapon :phaser
                          :weapon-number-setting 1
                          :target-bearing 90
                          :weapon-spread-setting 0}})]
      (:shots world) => [{:x ..x.. :y ..y.. :bearing 90 :range 0 :type :phaser}]))

  (fact
    "fire two torpedoes event"
    (let [world (process-events
                  [{:event :weapon-fire}]
                  {:ship {:x ..x.. :y ..y.. :selected-weapon :torpedo
                          :weapon-number-setting 2
                          :target-bearing 90
                          :weapon-spread-setting 10}})]
      (:shots world) => [{:x ..x.. :y ..y.. :bearing 85 :range 0 :type :torpedo}
                                 {:x ..x.. :y ..y.. :bearing 95 :range 0 :type :torpedo}])))

(facts
  "updating the world"
  (let [world (mom/make-world)
        ship (mom/make-ship)]
    (fact
      "rotate towards heading"
      (let [ship (assoc ship :heading-setting 90
                             :heading 70)
            world (assoc world :ship ship)
            world (update-world 1000 world)]
        (->> world :ship :heading) => (roughly 80)))

    (fact
      "impulse moves ship"
      (let [ship (assoc ship :impulse 1)
            world (assoc world :ship ship)
            world (update-world 1000 world)]
        (->> world :ship :velocity first) => #(> % 0)
        (->> world :ship :velocity second) => (roughly 0)))

    (fact
      "warp charges warp field"
      (let [ship (assoc ship :warp 1)
            world (assoc world :ship ship)
            world (update-world 1000 world)
            ship (:ship world)
            warp-charge (:warp-charge ship)]
        warp-charge => (roughly 1000)))

    (fact
      "warp field threshold moves ship"
      (let [ship (assoc ship :warp 1
                             :warp-charge warp-threshold)
            world (assoc world :ship ship)
            world (update-world 1000 world)
            ship (:ship world)
            pos [(:x ship) (:y ship)]]
        pos => (vt/roughly-v [warp-leap 0])))

    )
  )
