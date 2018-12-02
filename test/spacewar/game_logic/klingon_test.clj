(ns spacewar.game-logic.klingon-test
  (:require [midje.sweet :refer [fact facts => tabular roughly anything]]
            [spacewar.game-logic.config :refer [klingon-shields
                                                number-of-klingons
                                                klingon-antimatter
                                                klingon-battle-state-transition-age
                                                phaser-range
                                                phaser-damage
                                                klingon-shield-recharge-rate
                                                klingon-shield-recharge-cost
                                                klingon-kinetic-firing-distance
                                                klingon-kinetic-power
                                                klingon-kinetic-threshold
                                                klingon-phaser-firing-distance
                                                klingon-phaser-power
                                                klingon-phaser-threshold
                                                klingon-torpedo-firing-distance
                                                klingon-torpedo-threshold
                                                klingon-torpedo-power
                                                klingon-tactical-range
                                                klingon-thrust
                                                klingon-antimatter-runaway-threshold
                                                klingon-evasion-limit
                                                ship-docking-distance
                                                klingon-evasion-trajectories]]
            [spacewar.game-logic.klingons :as k]
            [spacewar.game-logic.test-mother :as mom]
            [clojure.spec.alpha :as s]
            [spacewar.game-logic.shots :as shots]
            [spacewar.game-logic.bases :as bases]
            [spacewar.vector-test :as vt]
            [spacewar.geometry :as geo]))

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
            klingon (->> new-world :klingons first)]
        new-world => mom/valid-world?
        (:shields klingon) => klingon-shields
        (:hit klingon) => nil?
        (:battle-state-age klingon) => 20
        (:explosions new-world) => []
        ))

    (fact
      "simple kinetic hit"
      (let [klingon (assoc klingon :hit {:weapon :kinetic :damage 20}
                                   :antimatter klingon-antimatter
                                   :battle-state :advancing
                                   :battle-state-age 1)
            world (assoc world :klingons [klingon])
            new-world (k/update-klingons 0 world)
            klingons (:klingons new-world)
            explosions (:explosions new-world)
            klingon (first klingons)]
        (count klingons) => 1
        (:hit klingon) => nil
        (:shields klingon) => (roughly 180)
        (:battle-state-age klingon) => (partial < klingon-battle-state-transition-age)
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
                offense (k/update-klingon-offense 20 world)]
            offense => world))

        (fact
          "just entered kinetic firing distance"
          (let [in-range (dec klingon-kinetic-firing-distance)
                klingon (mom/set-pos klingon [in-range 0])
                world (-> world (mom/set-ship ship) (mom/set-klingons [klingon]))
                offense (k/update-klingon-offense 20 world)]
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
                offense (k/update-klingon-offense 0 world)]
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
                offense (k/update-klingon-offense 0 world)]
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
                offense (k/update-klingon-offense 0 world)]
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
                offense (k/update-klingon-offense 0 world)]
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
                offense (k/update-klingon-offense 0 world)]
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
                offense (k/update-klingon-offense 0 world)]
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
                offense (k/update-klingon-offense 0 world)]
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
                offense (k/update-klingon-offense 0 world)]
            offense => mom/valid-world?
            (count (:shots offense)) => 0
            (let [klingon (->> offense :klingons first)]
              (klingon :weapon-charge) => klingon-kinetic-threshold
              (klingon :kinetics) => 0))))))

  (tabular
    (fact
      "klingon in a battle state thrusts appropriately relative to ship"
      (let [ship-pos [0 0]
            ship (mom/set-pos ship ship-pos)
            klingon (mom/set-pos klingon [(dec klingon-tactical-range) 0])
            klingon (assoc klingon :antimatter klingon-antimatter :battle-state ?battle-state)
            world (assoc world :ship ship :klingons [klingon])
            new-world (k/update-klingon-motion 2 world)
            new-klingon (->> new-world :klingons first)
            thrust (:thrust new-klingon)
            thrust-angle (geo/angle-degrees thrust ship-pos)]
        thrust-angle => (roughly (klingon-evasion-trajectories ?battle-state) 1e-5)))
    ?battle-state
    :advancing
    :retreating
    :flank-right
    :flank-left
    )

  (fact
    "klingon out of range of ship continues on course"
    (let [ship (mom/set-pos ship [0 0])
          klingon (mom/set-pos klingon [(inc klingon-tactical-range) 0])
          klingon (assoc klingon :thrust [1 1])
          world (assoc world :ship ship :klingons [klingon])
          new-world (k/update-klingon-motion 2 world)
          new-klingon (->> new-world :klingons first)
          thrust (:thrust new-klingon)]
      thrust => [1 1]))

  (fact
    "thrusting klingon increases velocity"
    (prerequisites (k/calc-drag anything) => 1)
    (let [klingon (mom/set-pos klingon [(dec klingon-tactical-range) 0])
          klingon (assoc klingon :antimatter klingon-antimatter)
          world (assoc world :klingons [klingon])
          new-world (k/update-klingon-motion 2 world)
          velocity (->> new-world :klingons first :velocity)]
      (first velocity) => (roughly (* -2 klingon-thrust))
      (second velocity) => (roughly 0 1e-10)
      ))

  (fact
    "klingons with velocity move"
    (let [klingon (assoc klingon :velocity [1 1])
          klingon (mom/set-pos klingon [1000 1000])
          world (assoc world :klingons [klingon])
          new-world (k/update-klingon-motion 2 world)
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
    "klingons far from ship thrust towards nearest base"
    (let [ship (assoc ship :x 1e7 :y 1e7)
          base1 (bases/make-base [0 2000] :antimatter-factory)
          base2 (bases/make-base [1000 0] :antimatter-factory)
          world (assoc world :ship ship :bases [base1 base2])
          world (k/update-thrust-towards-nearest-base world)
          klingon (-> world :klingons first)]
      (:thrust klingon) => (vt/roughly-v [klingon-thrust 0])))


  (def expired-age (inc klingon-battle-state-transition-age))
  (def battle-state? (partial contains? #{:advancing :retreating :flank-left :flank-right}))
  (def run-away (dec klingon-antimatter-runaway-threshold))
  (tabular
    (fact
      "klingon battle-state as function of distance, age, and antimatter"
      (prerequisite (k/random-battle-state) => :flank-right)
      (let [ship (assoc ship :x ?distance)
            klingon (assoc klingon :battle-state ?start-state
                                   :battle-state-age ?age
                                   :antimatter ?antimatter)
            world (assoc world :ship ship :klingons [klingon])
            world (k/update-klingons-state 10 world)
            klingon (-> world :klingons first)]
        (:battle-state klingon) => ?end-state
        (:battle-state-age klingon) => ?new-age))
    ?antimatter ?distance ?start-state ?age ?end-state ?new-age
    klingon-antimatter (inc klingon-tactical-range) :advancing 0 :no-battle 10
    klingon-antimatter (dec klingon-tactical-range) :no-battle 0 :advancing 10
    klingon-antimatter (/ klingon-evasion-limit 2) :no-battle expired-age :flank-right 0
    klingon-antimatter (dec klingon-evasion-limit) :no-battle 0 :no-battle 10
    run-away (dec klingon-tactical-range) :advancing 0 :retreating 10
    )

  (fact
    "klingon within docking distance of base steals antimatter"
    (let [base (mom/make-base (:x klingon) (+ (:y klingon) (dec ship-docking-distance))
                              :antimatter-factory 100 100)
          klingon (assoc klingon :antimatter 0)
          world (assoc world :klingons [klingon] :bases [base])
          world (k/klingons-steal-antimatter world)
          base (-> world :bases first)
          klingon (-> world :klingons first)]
      (:antimatter base) => 0
      (:antimatter klingon) => 100))

  (fact
    "update-klingons calls all the necessary functions"
    (let [ms 10]
      (k/update-klingons ms world) => world
      (provided (k/update-klingon-defense ms world) => world
                (k/update-klingon-offense ms world) => world
                (k/update-klingon-motion ms world) => world
                (k/update-klingons-state ms world) => world)))

  )
