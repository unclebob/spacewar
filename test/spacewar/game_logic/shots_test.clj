(ns spacewar.game-logic.shots-test
  (:require [midje.sweet :refer [facts fact tabular roughly =>]]
            [spacewar.game-logic.config :refer [phaser-velocity
                                                kinetic-velocity
                                                phaser-range
                                                torpedo-velocity
                                                torpedo-range
                                                phaser-proximity
                                                torpedo-proximity
                                                kinetic-proximity
                                                torpedo-damage
                                                kinetic-damage
                                                ship-shields
                                                klingon-kinetic-proximity
                                                klingon-kinetic-damage
                                                klingon-torpedo-damage
                                                klingon-phaser-damage
                                                klingon-torpedo-proximity
                                                klingon-phaser-proximity
                                                ship-antimatter
                                                phaser-power
                                                ship-kinetics
                                                ship-torpedos]]
            [spacewar.game-logic.shots :refer [update-shot-positions
                                               fire-weapon
                                               update-hits
                                               update-ship-hits
                                               ->shot
                                               weapon-fire-handler
                                               calc-damage
                                               incur-damage
                                               corrupt-shots-by-damage
                                               weapon-failure-dice
                                               weapon-bearing-deviation
                                               warp-bearing-deviation
                                               warp-corruption]]
            [spacewar.game-logic.test-mother :as mom]))

(facts
  "about shot movement"
  (tabular
    (fact
      "update-phaser-shot"
      (let [shot {:x ?x :y ?y :bearing ?bearing :range 0 :type :phaser}
            world (assoc (mom/make-world) :shots [shot])
            world (update-shot-positions ?ms world)
            shot (first (:shots world))
            ]
        (:x shot) => (roughly ?sx 1e-10)
        (:y shot) => (roughly ?sy 1e-10)
        (:bearing shot) => (roughly ?bearing 1e-10)
        (:range shot) => (roughly (* ?ms phaser-velocity))))
    ?ms ?x ?y ?bearing ?sx ?sy
    1000 0 0 0 (* phaser-velocity ?ms) 0
    1000 0 0 90 0 (* phaser-velocity ?ms)
    )

  (tabular
    (fact
      "update-kinetic-shot"
      (let [shot {:x ?x :y ?y :bearing ?bearing :range 0 :type :kinetic}
            world (assoc (mom/make-world) :shots [shot])
            world (update-shot-positions ?ms world)
            shot (->> world :shots first)
            ]
        (:x shot) => (roughly ?sx 1e-10)
        (:y shot) => (roughly ?sy 1e-10)
        (:bearing shot) => (roughly ?bearing 1e-10)
        (:range shot) => (roughly (* ?ms kinetic-velocity))))
    ?ms ?x ?y ?bearing ?sx ?sy
    1000 0 0 0 (* kinetic-velocity ?ms) 0
    1000 0 0 90 0 (* kinetic-velocity ?ms)
    )

  (fact
    "phaser shots go out of range"
    (let [ms-out-of-range (+ 1 (/ phaser-range phaser-velocity))
          world (mom/make-world)
          world (assoc world :shots [{:x 0 :y 0 :bearing 0 :range 0 :type :phaser}])
          new-world (update-shot-positions ms-out-of-range world)]
      (count (:shots new-world)) => 0))

  (fact
    "phaser shots in range"
    (let [ms-in-range 0
          world (mom/make-world)
          world (assoc world :shots [{:x 0 :y 0 :bearing 0 :range 0 :type :phaser}])
          new-world (update-shot-positions ms-in-range world)]
      (count (:shots new-world)) => 1))

  (tabular
    (fact
      "update-torpedo-shot"
      (let [shot {:x ?x :y ?y :bearing ?bearing :range ?range :type :torpedo}
            world (assoc (mom/make-world) :shots [shot])
            world (update-shot-positions ?ms world)
            shot (first (:shots world))]
        (:x shot) => (roughly ?sx 1e-10)
        (:y shot) => (roughly ?sy 1e-10)
        (:bearing shot) => (roughly ?bearing 1e-10)
        (:range shot) => (roughly
                           (+ ?range
                              (* ?ms torpedo-velocity)))))
    ?ms ?x ?y ?bearing ?range ?sx ?sy
    1000 0 0 0 0 (* torpedo-velocity ?ms) 0
    1000 0 0 90 0 0 (* torpedo-velocity ?ms)
    1000 0 0 0 (/ torpedo-range 2) (* torpedo-velocity ?ms) 0))

(fact
  "fire-weapon"
  (fire-weapon [0 0] 0 1 0) => [{:x 0 :y 0 :bearing 0 :range 0}]
  (fire-weapon [1 1] 90 1 0) => [{:x 1 :y 1 :bearing 90 :range 0}]
  (fire-weapon [0 0] 90 2 10) => [{:x 0 :y 0 :bearing 85 :range 0}
                                  {:x 0 :y 0 :bearing 95 :range 0}]
  (fire-weapon [0 0] 0 2 10) => [{:bearing 355 :range 0 :x 0 :y 0}
                                 {:bearing 5 :range 0 :x 0 :y 0}]
  )

(tabular
  (facts
    "about shots hitting enemies"
    (fact
      "no shots"
      (let [world {:shots []
                   ?enemy [{:x 0 :y 0}]}
            world (update-hits ?enemy world)
            enemies (?enemy world)
            shots (:shots world)]
        shots => []
        enemies => [{:x 0 :y 0}]))

    (fact
      "shot out of range"
      (let [world {:shots [{:x (inc ?proximity) :y 0 :bearing 0 :range ..range.. :type ?weapon}]
                   ?enemy [{:x 0 :y 0}]}
            world (update-hits ?enemy world)
            enemies (?enemy world)
            shots (:shots world)]
        shots => [{:x (inc ?proximity) :y 0 :bearing 0 :range ..range.. :type ?weapon}]
        enemies => [{:x 0 :y 0}]))

    (fact
      "one shot hits enemy"
      (let [world {:shots [{:x (dec ?proximity) :y 0 :bearing 0 :range ..range.. :type ?weapon}]
                   ?enemy [{:x 0 :y 0}]
                   :explosions [:before]}
            world (update-hits ?enemy world)
            enemies (?enemy world)
            shots (:shots world)
            explosions (:explosions world)]
        shots => []
        enemies => [{:x 0 :y 0 :hit {:weapon ?weapon :damage (if (= ?weapon :phaser)
                                                               [..range..]
                                                               ?damage)}}]
        (first explosions) => :before
        (dissoc (second explosions) :fragments) => {:x (dec ?proximity) :y 0 :type ?weapon :age 0}))

    (fact
      "Two shots, one hits enemy"
      (let [world {:shots [{:x (inc ?proximity) :y 0 :bearing 0 :range ..range.. :type ?weapon}
                           {:x (dec ?proximity) :y 0 :bearing 0 :range ..range.. :type ?weapon}]
                   ?enemy [{:x 0 :y 0}]
                   :explosions [:before]}
            world (update-hits ?enemy world)
            enemies (?enemy world)
            shots (:shots world)
            explosions (:explosions world)]
        shots => [{:x (inc ?proximity) :y 0 :bearing 0 :range ..range.. :type ?weapon}]
        enemies => [{:x 0 :y 0 :hit {:weapon ?weapon :damage (if (= ?weapon :phaser)
                                                               [..range..]
                                                               ?damage)}}]
        (first explosions) => :before
        (dissoc (second explosions) :fragments) => {:x (dec ?proximity) :y 0 :type ?weapon :age 0}))

    (fact
      "Two shots, both hit enemy"
      (let [world {:shots [{:x (dec ?proximity) :y 0 :bearing 0 :range ..range.. :type ?weapon}
                           {:x (dec ?proximity) :y 0 :bearing 1 :range ..range.. :type ?weapon}]
                   ?enemy [{:x 0 :y 0}]
                   :explosions [:before]}
            world (update-hits ?enemy world)
            enemies (?enemy world)
            shots (:shots world)
            explosions (:explosions world)]
        shots => []
        enemies => [{:x 0 :y 0 :hit {:weapon ?weapon :damage (if (= ?weapon :phaser)
                                                               [..range.. ..range..]
                                                               (* 2 ?damage))}}]
        (nth explosions 0) => :before
        (dissoc (nth explosions 1) :fragments) => {:x (dec ?proximity) :y 0 :type ?weapon :age 0}
        (dissoc (nth explosions 2) :fragments) => {:x (dec ?proximity) :y 0 :type ?weapon :age 0}))
    )
  ?enemy ?proximity ?weapon ?damage
  :klingons phaser-proximity :phaser nil
  :klingons torpedo-proximity :torpedo torpedo-damage
  :klingons kinetic-proximity :kinetic kinetic-damage
  :romulans phaser-proximity :phaser nil
  :romulans torpedo-proximity :torpedo torpedo-damage
  :romulans kinetic-proximity :kinetic kinetic-damage
  )

(tabular
  (facts
    "about klingon shots"
    (fact
      "klingon shot misses ship"
      (let [ship (mom/make-ship)
            shot (->shot 0 (inc ?proximity)
                         180 ?weapon)
            world (assoc (mom/make-world) :shots [shot] :ship ship)
            new-world (update-ship-hits world)]
        (->> new-world :explosions) => []
        (->> new-world :ship :shields) => ship-shields
        (count (->> new-world :shots)) => 1))

    (fact
      "klingon shot hits ship"
      (let [ship (mom/make-ship)
            shot (->shot 0 (dec ?proximity)
                         180 ?weapon)
            world (assoc (mom/make-world) :shots [shot] :ship ship)
            new-world (update-ship-hits world)]
        (count (->> new-world :explosions)) => 1
        (->> new-world :ship :shields) => (- ship-shields ?damage)
        (count (->> new-world :shots)) => 0))

    (fact
      "klingon shot does not hit klingon"
      (let [world {:shots [{:x (dec ?proximity) :y 0 :bearing 0 :range ..range.. :type ?weapon}]
                   :klingons [{:x 0 :y 0}]
                   :explosions []}
            world (update-hits :klingons world)
            klingons (:klingons world)
            shots (:shots world)
            explosions (:explosions world)]
        (count shots) => 1
        klingons => [{:x 0 :y 0}]
        explosions => empty?))
    )
  ?proximity ?weapon ?damage
  klingon-kinetic-proximity :klingon-kinetic klingon-kinetic-damage
  klingon-torpedo-proximity :klingon-torpedo klingon-torpedo-damage
  klingon-phaser-proximity :klingon-phaser klingon-phaser-damage
  )

(facts
  "romulan-blast"
  (fact
    "hasn't hit ship yet"
    (let [
          world (mom/make-world)
          ship (assoc (:ship world) :shields ship-shields :x 10000)
          blast (assoc (mom/make-shot) :type :romulan-blast :bearing 0 :x 5000 :y 0 :range 5000)
          world (assoc world :shots [blast] :ship ship)
          world (update-ship-hits world)
          ship (:ship world)
          shots (:shots world)]
      (count shots) => 1
      (:shields ship) => ship-shields))

  (fact "hits ship"
        (let [
              world (mom/make-world)
              ship (assoc (:ship world) :shields ship-shields :y 10000)
              blast (assoc (mom/make-shot) :type :romulan-blast :bearing 0 :x 11000 :y 0 :range 11000)
              world (assoc world :shots [blast] :ship ship)
              world (update-ship-hits world)
              ship (:ship world)
              shots (:shots world)
              explosions (:explosions world)]
          (count shots) => 0
          (count explosions) => 1
          (:shields ship) => (partial > ship-shields))
        )
  )

(fact
  "shot constructor"
  (->shot 0 0 180 :kinetic) => mom/valid-shot?)

(fact
  "Weapons use power"
  (let [world (mom/make-world)
        ship (assoc (mom/make-ship) :selected-weapon :phaser
                                    :weapon-number-setting 2)
        world (assoc world :ship ship)
        new-world (weapon-fire-handler {} world)
        shots (->> new-world :shots)
        antimatter (->> new-world :ship :antimatter)]
    (count shots) => 2
    antimatter => (- ship-antimatter (* 2 phaser-power))))

(fact
  "weapons fail if not enough antimatter"
  (let [world (mom/make-world)
        ship (assoc (mom/make-ship) :selected-weapon :phaser
                                    :weapon-number-setting 2
                                    :antimatter 1)
        world (assoc world :ship ship)
        new-world (weapon-fire-handler {} world)
        shots (->> new-world :shots)
        antimatter (->> new-world :ship :antimatter)]
    (count shots) => 0
    antimatter => 1)
  )

(fact
  "successful firing of kinetics decrements inventory"
  (let [world (mom/make-world)
        ship (assoc (mom/make-ship) :selected-weapon :kinetic
                                    :weapon-number-setting 2)
        world (assoc world :ship ship)
        new-world (weapon-fire-handler {} world)
        kinetics (->> new-world :ship :kinetics)]
    kinetics => (- ship-kinetics 2)))

(fact
  "successful firing of torpedos decrements inventory"
  (let [world (mom/make-world)
        ship (assoc (mom/make-ship) :selected-weapon :torpedo
                                    :weapon-number-setting 2)
        world (assoc world :ship ship)
        new-world (weapon-fire-handler {} world)
        torpedos (->> new-world :ship :torpedos)]
    torpedos => (- ship-torpedos 2)))

(fact
  "can't fire kinetic if not in inventory"
  (let [world (mom/make-world)
        ship (assoc (mom/make-ship) :selected-weapon :kinetic
                                    :weapon-number-setting 2
                                    :kinetics 1)
        world (assoc world :ship ship)
        new-world (weapon-fire-handler {} world)
        shots (->> new-world :shots)
        kinetics (->> new-world :ship :kinetics)]
    (count shots) => 0
    kinetics => 1))

(fact
  "can't fire torpedo if not in inventory"
  (let [world (mom/make-world)
        ship (assoc (mom/make-ship) :selected-weapon :torpedo
                                    :weapon-number-setting 2
                                    :torpedos 1)
        world (assoc world :ship ship)
        new-world (weapon-fire-handler {} world)
        shots (->> new-world :shots)
        torpedos (->> new-world :ship :torpedos)]
    (count shots) => 0
    torpedos => 1))

(tabular
  (fact
    "calculate real damage to ship"
    (calc-damage ?shields ?hit-strength) => ?real-damage)
  ?shields ?hit-strength ?real-damage
  ship-shields 100 0
  (/ ship-shields 2) 100 55
  (/ ship-shields 4) 100 80
  0 100 100
  )

(fact
  "damage can be incurred"
  (incur-damage 100 :not-a-system {:system 5}) => {:system 5}
  (incur-damage 100 :system {:system 0}) => {:system 100}
  (incur-damage 50 :system {:system 20}) => {:system 70}
  (incur-damage 50 :system {:system 80}) => {:system 100}
  )

(facts
  "Shot corruption"
  (let [shot1 (assoc (mom/make-shot) :bearing 90)
        shot2 (assoc (mom/make-shot) :bearing 95)
        shot3 (assoc (mom/make-shot) :bearing 100)
        shots [shot1 shot2 shot3]
        ]
    (fact
      "no corruption if weapons not damaged"
      (corrupt-shots-by-damage 0 shots) => shots)

    (fact
      "no corruption if dice say no"
      (corrupt-shots-by-damage 50 shots) => shots
      (provided (weapon-failure-dice 3 50) => [false false false]
                (weapon-bearing-deviation 3 50) => [0 0 0]))

    (fact
      "shots removed if dice say yes"
      (corrupt-shots-by-damage 50 shots) => [shot1 shot3]
      (provided (weapon-failure-dice 3 50) => [false true false]
                (weapon-bearing-deviation 2 50) => [0 0]))
    (fact
      "bearing not corrupted if bearing dice are kind"
      (corrupt-shots-by-damage 50 shots) => shots
      (provided (weapon-failure-dice 3 50) => [false false false]
                (weapon-bearing-deviation 3 50) => [0 0 0]))
    (fact
      "bearing corrupted by bearing dice"
      (->> (corrupt-shots-by-damage 50 shots) first :bearing) => 101
      (provided (weapon-failure-dice 3 50) => [true true false]
                (weapon-bearing-deviation 1 50) => [1])
      )
    (fact
      "bearing corruption normalized"
      (->> (corrupt-shots-by-damage 50 shots) first :bearing) => 1
      (provided (weapon-failure-dice 3 50) => [true true false]
                (weapon-bearing-deviation 1 50) => [261])
      )

    (fact
      "no warp corruption if not warping"
      (warp-corruption 0 shots) => shots)

    (fact
      (prerequisite (warp-bearing-deviation) => 10)
      "warp speed corrupts bearing"
      (let [shots (warp-corruption 1 shots)
            s0 (nth shots 0)
            s1 (nth shots 1)
            s2 (nth shots 2)]
        (:bearing s0) => 100
        (:bearing s1) => 105
        (:bearing s2) => 110))
    ))


