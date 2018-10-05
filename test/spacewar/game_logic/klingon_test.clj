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
      20
      {:klingons [{:anti-matter 1000
                   :shields klingon-shields}]}) =>
    {:klingons [{:shields klingon-shields :anti-matter 1000}]
     :explosions []})

  (fact
    "simple kinetic hit"
    (let [world (k/update-klingons
      0
      {:klingons
       [{:anti-matter 1000
         :shields 200
         :hit {:weapon :kinetic
               :damage 20}}]})
          klingons (:klingons world)
          explosions (:explosions world)]
      (count klingons) => 1
      (:hit (first klingons)) => nil
      (:shields (first klingons)) => (roughly 180)
      explosions => []))


  (fact
    "klingon destroyed"
    (let [world (k/update-klingons
                  20
                  {:klingons
                   [{:x 50 :y 50 :shields 10
                     :hit {:weapon :kinetic
                           :damage 20}}]})]
      (:klingons world) => []
      (count (:explosions world)) => 1
      (dissoc (first (:explosions world)) :fragments) => {:age 0 :x 50 :y 50 :type :klingon}))

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

  (tabular
    (fact
      "recharge-shield"
      (k/recharge-shield
        ?ms
        {:anti-matter ?am-in
         :shields ?shields-in}) => {:anti-matter ?am-out
                                    :shields ?shields-out})
    ?ms ?am-in ?shields-in ?am-out ?shields-out
    20 1000 klingon-shields 1000 klingon-shields
    20 1000
    (- klingon-shields 50)
    (- ?am-in (* klingon-shield-recharge-rate ?ms))
    (+ ?shields-in (* klingon-shield-recharge-rate ?ms))
    )
  )
