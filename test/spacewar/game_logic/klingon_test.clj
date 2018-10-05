(ns spacewar.game-logic.klingon-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.klingons :as k]
            [clojure.spec.alpha :as s]))

(facts
  "about klingons"
  (fact
    "make-random-klingon"
    (s/explain-data ::k/klingon (k/make-random-klingon)) => nil)
  (fact
    "initialize"
    (let [klingons (k/initialize)]
      (s/explain-data ::k/klingons klingons) => nil
      (count klingons) => number-of-klingons))

  (fact
    "no hit"
    (k/update-klingons
      ..ms..
      {:klingons [{:shields 100}]}) => {:klingons [{:shields 100}]})

  (fact
    "simple kinetic hit"
    (k/update-klingons
      ..ms..
      {:klingons
       [{:shields 200
         :hit {:weapon :kinetic
               :damage 20}}]}) => {:klingons [{:shields 180}]})

  (fact
    "klingon destroyed"
    (k/update-klingons
      ..ms..
      {:klingons
       [{:shields 10
         :hit {:weapon :kinetic
               :damage 20}}]}) => {:klingons []})

  (tabular
    (fact
      "phaser damage"
      (k/damage-by-phasers {:damage ?ranges}) => ?damage)
    ?ranges ?damage
    [phaser-range] 0
    [0] phaser-damage
    [(/ phaser-range 2)] (/ phaser-damage 2)
    [0 0] (* 2 phaser-damage)
    )

  )
