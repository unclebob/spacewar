(ns spacewar.game-logic.klingon-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.klingons :as k]
            [spacewar.game-logic.test-mother :as mom]
            [clojure.spec.alpha :as s]
            [spacewar.game-logic.shots :as shots]))

(let [klingon (assoc (mom/make-klingon) :shields klingon-shields
                                        :antimatter 1000)
      ship (mom/make-ship)
      world (assoc (mom/make-world) :klingons [klingon]
                                    :ship ship)]
  (facts
    "about klingons"
    (fact
      "make-random-klingon"
      (k/make-random-klingon) => mom/valid-klingon?)

    (fact
      "initialize"
      (let [klingons (k/initialize)]
        (s/explain-data ::k/klingons klingons) => nil
        (count klingons) => number-of-klingons))

    (fact
      "no hit"
      (let [new-world (k/update-klingons 20 world)
            new-klingon (->> new-world :klingons first)]
        new-world => mom/valid-world?
        (:shields new-klingon) => klingon-shields
        (:hit new-klingon) => nil?
        (:explosions new-world) => []
        ))


    (fact
      "simple kinetic hit"
      (let [klingon (assoc klingon :hit {:weapon :kinetic :damage 20})
            world (assoc world :klingons [klingon])
            new-world (k/update-klingons 0 world)
            klingons (:klingons new-world)
            explosions (:explosions new-world)]
        (count klingons) => 1
        (:hit (first klingons)) => nil
        (:shields (first klingons)) => (roughly 180)
        explosions => []))


    (fact
      "klingon destroyed"
      (let [world (mom/make-world)
            klingon (mom/make-klingon)
            klingon (mom/set-pos klingon [50 50])
            klingon (assoc klingon :shields 10
                                   :hit {:weapon :kinetic
                                         :damage 20})
            world (assoc world :klingons [klingon])
            world (k/update-klingons 20 world)
            ]
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
          {:antimatter ?am-in
           :shields ?shields-in}) => {:antimatter ?am-out
                                      :shields ?shields-out})
      ?ms ?am-in ?shields-in ?am-out ?shields-out
      20 1000 klingon-shields 1000 klingon-shields
      20 1000
      (- klingon-shields 50)
      (- ?am-in (* klingon-shield-recharge-rate ?ms))
      (+ ?shields-in (* klingon-shield-recharge-rate ?ms))
      )

    (facts
      "klingon-offense"
      (let [ship (-> (mom/make-ship) (mom/set-pos [0 0]))
            klingon (mom/make-klingon)
            world world]
        (fact
          "not in kinetic firing distance"
          (let [out-of-range [(inc klingon-kinetic-firing-distance) 0]
                klingon (mom/set-pos klingon out-of-range)
                world (-> world (mom/set-ship ship) (mom/set-klingons [klingon]))
                offense (k/klingon-offense 20 world)]
            offense => world))

        (fact
          "just entered kinetic firing distance"
          (let [in-range (dec klingon-kinetic-firing-distance)
                klingon (mom/set-pos klingon [in-range 0])
                world (-> world (mom/set-ship ship) (mom/set-klingons [klingon]))
                offense (k/klingon-offense 20 world)]
            offense => mom/valid-world?
            (->> offense :klingons first :kinetic-charge) => 20))

        (fact
          "kinetic-fired, shot added, charge reset, count reduced."
          (let [in-range (dec klingon-kinetic-firing-distance)
                klingon (mom/set-pos klingon [in-range 0])
                klingon (assoc klingon :kinetic-charge klingon-kinetic-threshold
                                       :kinetics 20)
                world (mom/set-klingons world [klingon])
                world (assoc world :shots [(shots/->shot 0 0 0 :phaser)])
                offense (k/klingon-offense 0 world)]
            offense => mom/valid-world?
            (count (:shots offense)) => 2
            (->> offense :shots second :type) => :klingon-kinetic
            (->> offense :shots second :bearing) => (roughly 180)
            (->> offense :klingons first :kinetic-charge) => 0
            (->> offense :klingons first :kinetics) => 19))

        (fact
          "in kinetic firing distance, all charged, but no more kinetics left."
          (let [in-range (dec klingon-kinetic-firing-distance)
                klingon (mom/set-pos klingon [in-range 0])
                klingon (assoc klingon :kinetic-charge klingon-kinetic-threshold
                                       :kinetics 0)
                world (mom/set-klingons world [klingon])
                world (assoc world :shots [])
                offense (k/klingon-offense 0 world)]
            offense => mom/valid-world?
            (count (:shots offense)) => 0
            (->> offense :klingons first :kinetic-charge) => klingon-kinetic-threshold
            (->> offense :klingons first :kinetics) => 0))

        ))
    ))
