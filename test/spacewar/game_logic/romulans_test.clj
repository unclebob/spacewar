(ns spacewar.game-logic.romulans-test
  (:require [midje.sweet :refer [facts fact tabular => roughly]]
            [spacewar.game-logic.romulans :as r]
            [spacewar.game-logic.test-mother :as mom]))

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
        (:age romulan) => 0
        (:fire-weapon romulan) => ?fire-weapon))
        ?current-state ?next-state ?fire-weapon
        :invisible :appearing false
        :appearing :visible false
        :visible :firing false
        :firing :fading true
        :fading :disappeared false
        )

    (fact
      "romulan does not create shot when firing-weapon is false"
      (let [romulan (assoc romulan :fire-weapon false)
            world (assoc world :romulans [romulan])
            world (r/fire-romulan-weapons world)
            romulan (first (:romulans world))
            shots (:shots world)]
        (:fire-weapon romulan) => false
        (count shots) => 0))

    (fact
      "romulan creates shot when firing-weapon is true"
      (let [ship (assoc (mom/make-ship) :x 0 :y 0)
            romulan (assoc romulan :fire-weapon true :x 100 :y 0)
            world (assoc world :romulans [romulan] :ship ship)
            world (r/fire-romulan-weapons world)
            romulan (first (:romulans world))
            shots (:shots world)]
        (:fire-weapon romulan) => false
        (count shots) => 1
        (:type (first shots)) => :romulan-blast
        (:x (first shots)) => (:x romulan)
        (:y (first shots)) => (:y romulan)
        (:bearing (first shots)) => (roughly 180)))

    (fact
      "disappeared romulans are deleted"
      (let [romulan1 (assoc (mom/make-romulan) :state :disappeared)
            romulan2 (assoc (mom/make-romulan) :state :invisible)
            world (assoc world :romulans [romulan1 romulan2])
            world (r/remove-disappeared-romulans world)
            romulans (:romulans world)]
        romulans => [romulan2]))

    (fact
      "hit romulans are deleted"
      (let [romulan (assoc (mom/make-romulan) :hit :some-hit)
            world (assoc world :romulans [romulan])
            world (r/destroy-hit-romulans world)
            romulans (:romulans world)]
        romulans => empty?))

    (fact
      "update-romulans calls all intermediaries"
      (r/update-romulans ms world) => world
      (provided (r/update-romulans-age ms world) => world
                (r/update-romulans-state ms world) => world
                (r/remove-disappeared-romulans world) => world
                (r/destroy-hit-romulans world) => world
                (r/fire-romulan-weapons world) => world
                )))

  )
