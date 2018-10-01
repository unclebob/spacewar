(ns spacewar.game-logic.explosions-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.shots :refer :all]))

(fact
  "update-explosions"
  (update-explosions 10 {:explosions [{:age 10}]}) => {:explosions [{:age 20}]}
  )
