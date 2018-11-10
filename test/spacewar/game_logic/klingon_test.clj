(ns spacewar.game-logic.klingon-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.klingons :as k]
            [spacewar.game-logic.test-mother :as mom]
            [clojure.spec.alpha :as s]
            [spacewar.game-logic.shots :as shots]
            [spacewar.game-logic.bases :as bases]
            [spacewar.vector-test :as vt]))

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
      (- ?am-in (* klingon-shield-recharge-rate klingon-shield-recharge-cost ?ms))
      (+ ?shields-in (* klingon-shield-recharge-rate ?ms))
      )

    (facts
      "klingon-offense"
      (prerequisite (k/delay-shooting?) => false)

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
            (->> offense :klingons first :weapon-charge) => 20))

        (fact
          "kinetic not fired fired if insufficient antimatter"
          (let [in-range (dec klingon-kinetic-firing-distance)
                klingon (mom/set-pos klingon [in-range 0])
                klingon (assoc klingon :antimatter (dec klingon-kinetic-power)
                                       :weapon-charge klingon-kinetic-threshold
                                       :kinetics 20)
                world (mom/set-klingons world [klingon])
                world (assoc world :shots [(shots/->shot 0 0 0 :phaser)])
                offense (k/klingon-offense 0 world)]
            offense => mom/valid-world?
            (count (:shots offense)) => 1
            (let [klingon (->> offense :klingons first)]
              (klingon :weapon-charge) => klingon-kinetic-threshold
              (klingon :kinetics) => 20
              (klingon :antimatter) => (dec klingon-kinetic-power))))

        (fact
          "kinetic-fired, shot added, charge reset, count reduced, power used."
          (let [in-range (dec klingon-kinetic-firing-distance)
                klingon (mom/set-pos klingon [in-range 0])
                klingon (assoc klingon :antimatter 1000
                                       :weapon-charge klingon-kinetic-threshold
                                       :kinetics 20)
                world (mom/set-klingons world [klingon])
                world (assoc world :shots [(shots/->shot 0 0 0 :phaser)])
                offense (k/klingon-offense 0 world)]
            offense => mom/valid-world?
            (count (:shots offense)) => 2
            (let [shot (->> offense :shots second)
                  klingon (->> offense :klingons first)]
              (shot :type) => :klingon-kinetic
              (shot :bearing) => (roughly 180)
              (klingon :weapon-charge) => 0
              (klingon :kinetics) => 19
              (klingon :antimatter) => (- 1000 klingon-kinetic-power))))

        (fact
          "phaser not fired if insufficient antimatter."
          (let [in-range (dec klingon-phaser-firing-distance)
                klingon (mom/set-pos klingon [in-range 0])
                klingon (assoc klingon :antimatter (dec klingon-phaser-power)
                                       :weapon-charge klingon-phaser-threshold)
                world (mom/set-klingons world [klingon])
                world (assoc world :shots [(shots/->shot 0 0 0 :phaser)])
                offense (k/klingon-offense 0 world)]
            offense => mom/valid-world?
            (count (:shots offense)) => 1
            (let [klingon (->> offense :klingons first)]
              (klingon :weapon-charge) => klingon-phaser-threshold
              (klingon :antimatter) => (dec klingon-phaser-power))))

        (fact
          "in phaser rage, phaser charged, shot added, charge reset, count reduced, power used."
          (let [in-range (dec klingon-phaser-firing-distance)
                klingon (mom/set-pos klingon [in-range 0])
                klingon (assoc klingon :antimatter 1000 :weapon-charge klingon-phaser-threshold)
                world (mom/set-klingons world [klingon])
                world (assoc world :shots [(shots/->shot 0 0 0 :phaser)])
                offense (k/klingon-offense 0 world)]
            offense => mom/valid-world?
            (count (:shots offense)) => 2
            (let [shot (->> offense :shots second)
                  klingon (->> offense :klingons first)]
              (shot :type) => :klingon-phaser
              (shot :bearing) => (roughly 180)
              (klingon :weapon-charge) => 0
              (klingon :antimatter) => (- 1000 klingon-phaser-power))))

        (fact
          "in torpedo rage, torpedo charged, ship-not-turning, shot added, charge reset, count reduced, power used."
          (let [in-range (dec klingon-torpedo-firing-distance)
                klingon (mom/set-pos klingon [in-range 0])
                klingon (assoc klingon :antimatter 1000 :weapon-charge klingon-torpedo-threshold
                                       :torpedos 1)
                world (mom/set-klingons world [klingon])
                world (assoc world :shots [])
                offense (k/klingon-offense 0 world)]
            offense => mom/valid-world?
            (count (:shots offense)) => 1
            (let [shot (->> offense :shots first)
                  klingon (->> offense :klingons first)]
              (shot :type) => :klingon-torpedo
              (shot :bearing) => (roughly 180)
              (klingon :weapon-charge) => 0
              (klingon :antimatter) => (- 1000 klingon-torpedo-power))))

        (fact
          "in torpedo rage, torpedo charged, no-torpedos, ship-not-turning, shot not added.  No costs."
          (let [in-range (dec klingon-torpedo-firing-distance)
                klingon (mom/set-pos klingon [in-range 0])
                klingon (assoc klingon :antimatter 1000 :weapon-charge klingon-torpedo-threshold
                                       :torpedos 0)
                world (mom/set-klingons world [klingon])
                world (assoc world :shots [])
                offense (k/klingon-offense 0 world)]
            offense => mom/valid-world?
            (count (:shots offense)) => 0))

        (fact
          "in torpedo rage, torpedo charged, ship turning, shot not added.  No costs."
          (let [in-range (dec klingon-torpedo-firing-distance)
                klingon (mom/set-pos klingon [in-range 0])
                klingon (assoc klingon :antimatter 1000 :weapon-charge klingon-torpedo-threshold
                                       :torpedos 1)
                ship (assoc ship :heading 90 :heading-setting 0)
                world (mom/set-klingons world [klingon])
                world (assoc world :shots []
                                   :ship ship)
                offense (k/klingon-offense 0 world)]
            offense => mom/valid-world?
            (count (:shots offense)) => 0))

        (fact
          "in kinetic firing distance, all charged, but no more kinetics left."
          (let [in-range (dec klingon-kinetic-firing-distance)
                klingon (mom/set-pos klingon [in-range 0])
                klingon (assoc klingon :weapon-charge klingon-kinetic-threshold
                                       :kinetics 0)
                world (mom/set-klingons world [klingon])
                world (assoc world :shots [])
                offense (k/klingon-offense 0 world)]
            offense => mom/valid-world?
            (count (:shots offense)) => 0
            (let [klingon (->> offense :klingons first)]
              (klingon :weapon-charge) => klingon-kinetic-threshold
              (klingon :kinetics) => 0))))))

  (fact
    "klingon far away from ship does not thrust"
    (let [ship (mom/set-pos ship [0 0])
          klingon (mom/set-pos klingon [(inc klingon-tactical-range) 0])
          world (assoc world :ship ship :klingons [klingon])
          new-world (k/klingon-motion 1000 world)
          new-klingon (->> new-world :klingons first)
          thrust (:thrust new-klingon)]
      thrust => [0 0])
    )

  (fact
    "klingon within tactical range of ship thrusts towards ship"
    (prerequisite (k/evasion-angle anything) => 0)
    (let [ship (mom/set-pos ship [0 0])
          klingon (mom/set-pos klingon [(dec klingon-tactical-range) 0])
          klingon (assoc klingon :antimatter klingon-antimatter)
          world (assoc world :ship ship :klingons [klingon])
          new-world (k/klingon-motion 2 world)
          new-klingon (->> new-world :klingons first)
          thrust (:thrust new-klingon)]
      (first thrust) => (roughly (* -1 klingon-thrust) 1e-5)
      (second thrust) => (roughly 0 1e-10))
    )

  (fact
    "klingon within evasion range of ship thrusts orthogonal to ship"
    (let [ship (mom/set-pos ship [0 0])
          klingon (mom/set-pos klingon [(dec klingon-evasion-limit) 0])
          world (assoc world :ship ship :klingons [klingon])
          new-world (k/klingon-motion 2 world)
          new-klingon (->> new-world :klingons first)
          thrust (:thrust new-klingon)]
      (second thrust) => (roughly (* -1 klingon-thrust) 1e-5)
      (first thrust) => (roughly 0 1e-10))
    )

  (fact
    "klingon in range but low on antimatter thrusts away from ship"
    (let [ship (mom/set-pos ship [0 0])
          klingon (mom/set-pos klingon [(dec klingon-tactical-range) 0])
          klingon (assoc klingon :antimatter (dec klingon-antimatter-runaway-threshold))
          world (assoc world :ship ship :klingons [klingon])
          new-world (k/klingon-motion 2 world)
          new-klingon (->> new-world :klingons first)
          thrust (:thrust new-klingon)]
      (first thrust) => (roughly (* klingon-thrust) 1e-5)
      (second thrust) => (roughly 0 1e-10))
    )

  (fact
    "klingon out of range of ship continues on course"
    (let [ship (mom/set-pos ship [0 0])
          klingon (mom/set-pos klingon [(inc klingon-tactical-range) 0])
          klingon (assoc klingon :thrust [1 1])
          world (assoc world :ship ship :klingons [klingon])
          new-world (k/klingon-motion 2 world)
          new-klingon (->> new-world :klingons first)
          thrust (:thrust new-klingon)]
      thrust => [1 1]))

  (fact
    "thrusting klingon increases velocity"
    (prerequisites (k/calc-drag anything) => 1
                   (k/evasion-angle anything) => 0)
    (let [klingon (mom/set-pos klingon [(dec klingon-tactical-range) 0])
          klingon (assoc klingon :antimatter klingon-antimatter)
          world (assoc world :klingons [klingon])
          new-world (k/klingon-motion 2 world)
          velocity (->> new-world :klingons first :velocity)]
      (first velocity) => (roughly (* -2 klingon-thrust))
      (second velocity) => (roughly 0 1e-10)
      ))

  (fact
    "klingons with velocity move"
    (let [klingon (assoc klingon :velocity [1 1])
          klingon (mom/set-pos klingon [1000 1000])
          world (assoc world :klingons [klingon])
          new-world (k/klingon-motion 2 world)
          x (->> new-world :klingons first :x)
          y (->> new-world :klingons first :y)]
      x => (roughly 1002)
      y => (roughly 1002)))

  (fact
    "klingons far from ship don't thrust towards base if no bases"
    (let [ship (assoc ship :x 1e7 :y 1e7)
          world (assoc world :ship ship)
          world (k/update-thrust-towards-nearest-base world)
          klingon (-> world :klingons first)]
      (:thrust klingon) => [0 0]))

  (fact
      "klingons near ship do not thrust towards nearest base"
      (let [ship (assoc ship :x (dec klingon-tactical-range) :y 0)
            base (bases/make-base [1000 0] :antimatter-factory)
            world (assoc world :ship ship :bases [base])
            world (k/update-thrust-towards-nearest-base world)
            klingon (-> world :klingons first)]
        (:thrust klingon) => [0 0]))

  (fact
    "klingons far from ship  thrust towards nearest base"
    (let [ship (assoc ship :x 1e7 :y 1e7)
          base1 (bases/make-base [0 2000] :antimatter-factory)
          base2 (bases/make-base [1000 0] :antimatter-factory)
          world (assoc world :ship ship :bases [base1 base2])
          world (k/update-thrust-towards-nearest-base world)
          klingon (-> world :klingons first)]
      (:thrust klingon) => (vt/roughly-v [klingon-thrust 0])))

  )
