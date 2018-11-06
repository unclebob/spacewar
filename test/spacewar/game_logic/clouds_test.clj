(ns spacewar.game-logic.clouds-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.clouds :refer :all]
            [spacewar.game-logic.test-mother :as mom]))

(fact
  "constructor"
  (make-cloud) => valid-cloud?)

(fact
  "clouds age"
  (let [world (mom/make-world)
        cloud (make-cloud 0 0 100)
        world (assoc world :clouds [cloud])
        world (update-clouds 2 world)
        cloud (first (:clouds world))]
    (:concentration cloud) => (* 100 cloud-decay-rate cloud-decay-rate)))

(fact
  "Clouds disappear when concentration goes below 1"
  (let [world (mom/make-world)
        cloud (make-cloud 0 0 1)
        world (assoc world :clouds [cloud])
        world (update-clouds 1 world)
        clouds (:clouds world)]
    clouds => empty?))
