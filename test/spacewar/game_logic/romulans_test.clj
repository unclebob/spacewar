(ns spacewar.game-logic.romulans-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :as gc]
            [spacewar.game-logic.romulans :as r]
            [spacewar.game-logic.test-mother :as mom]
            [clojure.spec.alpha :as s]
            [spacewar.vector-test :as vt]))

(fact
  "can make romulan"
  (r/make-romulan 12 12) => mom/valid-romulan?)

(facts
  "romulans"
  (let [ms 10
        world (mom/make-world)
        romulan (mom/make-romulan)
        world (assoc world :romulans [romulan])]

    (fact
      "romulans age"
      (let [world (r/update-romulans-age ms world)
            romulan (first (:romulans world))]
        (:age romulan) => ms))

    (tabular
      (fact
      "romulans change state properly"
      (prerequisite (r/romulan-state-transition ms 99 ?current-state) => true)
      (let [romulan (assoc romulan :state ?current-state :age 99)
            world (assoc world :romulans [romulan])
            world (r/update-romulans-state ms world)
            romulan (first (:romulans world))]
        (:state romulan) => ?next-state
        (:age romulan) => 0))
        ?current-state ?next-state
        :invisible :appearing
        :appearing :visible
        :visible :firing
        :firing :fading
        :fading :disappeared
        )

    (fact
      "disappeared romulans are deleted"
      (let [romulan1 (assoc (mom/make-romulan) :state :disappeared)
            romulan2 (assoc (mom/make-romulan) :state :invisible)
            world (assoc world :romulans [romulan1 romulan2])
            world (r/remove-disappeared-romulans world)
            romulans (:romulans world)]
        romulans => [romulan2]))

    (fact
      "update-romulans calls all intermediaries"
      (r/update-romulans ms world) => world
      (provided (r/update-romulans-age ms world) => world
                (r/update-romulans-state ms world) => world
                (r/remove-disappeared-romulans world) => world
                )))

  )
