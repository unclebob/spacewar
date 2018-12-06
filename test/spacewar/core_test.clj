(ns spacewar.core-test
  (:require [midje.sweet :refer [facts fact tabular => roughly]]
            [spacewar.core :refer [make-initial-world
                                   process-events
                                   update-world
                                   add-frame-time
                                   frames-per-second]]
            [spacewar.vector-test :as vt]
            [spacewar.game-logic.config :refer [warp-threshold
                                                warp-leap]]
            [spacewar.game-logic.test-mother :as mom]
            [spacewar.game-logic.test-mother :as mom]))

(fact
  "initial world is created correctly"
  (make-initial-world) => mom/valid-world?)

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
  (let [world (mom/make-world)
        ship (mom/make-ship)
        world (assoc world :ship ship)]
    (tabular
      (fact
        "about engine selection events"
        (let [ship (assoc ship :selected-engine ?old-engine)
              world (assoc world :ship ship)
              new-world (process-events [{:event ?event}] world)]
          (->> new-world :ship :selected-engine) => ?new-engine))
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
                 ..engine.. ..power..}})))


(facts
  "about weapons panel events"
  (let [world (mom/make-world)
        ship (mom/make-ship)
        world (assoc world :ship ship)]
    (tabular
      (fact
        "about weapon selection events"
        (let [ship (assoc ship :selected-weapon ?old-weapon)
              world (assoc world :ship ship)
              new-world (process-events [{:event ?event}] world)]
          (->> new-world :ship :selected-weapon) => ?new-weapon))
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
      (let [new-world (process-events
                        [{:event :weapon-direction :angle ..angle..}]
                        world)]
        (->> new-world :ship :target-bearing) => ..angle..))

    (tabular
      (fact
        "weapon number event"
        (let [new-world (process-events
                          [{:event :weapon-number :value ?number}] world)
              number (->> new-world :ship :weapon-number-setting)
              spread (->> new-world :ship :weapon-spread-setting)]
          number => ?number
          spread => ?spread))
      ?number ?spread
      1 0
      2 1
      3 1)

    (fact
      "weapon spread event"
      (let [new-world (process-events
                        [{:event :weapon-spread :value ..spread..}] world)]
        (->> new-world :ship :weapon-spread-setting) => ..spread..))

    (fact
      "fire one phaser event"
      (let [ship (assoc ship :x ..x.. :y ..y..
                             :selected-weapon :phaser
                             :weapon-number-setting 1
                             :target-bearing 90
                             :weapon-spread-setting 0)
            world (assoc world :ship ship)
            new-world (process-events [{:event :weapon-fire}] world)]
        (:shots new-world) => [{:x ..x.. :y ..y.. :bearing 90 :range 0 :type :phaser}]))

    (fact
      "fire two torpedoes event"
      (let [ship (assoc ship :x ..x.. :y ..y.. :selected-weapon :torpedo
                             :weapon-number-setting 2
                             :target-bearing 90
                             :weapon-spread-setting 10)
            world (assoc world :ship ship)
            new-world (process-events [{:event :weapon-fire}] world)]
        (:shots new-world) => [{:x ..x.. :y ..y.. :bearing 85 :range 0 :type :torpedo}
                               {:x ..x.. :y ..y.. :bearing 95 :range 0 :type :torpedo}]))))

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
            pos => (vt/roughly-v [warp-leap 0])))))

(facts
  "About frame rate"
  (let [context {:frame-times []}]
    (fact
      "adding to empty frame time list"
      (->> context (add-frame-time 10) :frame-times) => [10]
      )

    (fact
      "can add two times to time list"
      (let [context (reduce #(add-frame-time %2 %1) context [1 2])
            frame-times (:frame-times context)]
        frame-times => [1 2]))

    (fact
      "can only accumulates to 10"
      (let [context (reduce #(add-frame-time %2 %1)
                            context
                            [1 2 3 4 5 6 7 8 9 10 11 12])
            frame-times (:frame-times context)]
        frame-times => [3 4 5 6 7 8 9 10 11 12]))

    (fact
      "frame-time averages"
      (frames-per-second []) => 0
      (frames-per-second [1000]) => 1
      (frames-per-second [30 40 50 60]) => 1000/45)))
