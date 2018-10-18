(ns spacewar.game-logic.shots-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.shots :refer :all]
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
  (fire-weapon [0 0] 90 2 10) => [{:x 0 :y 0 :bearing 85 :range 0} {:x 0 :y 0 :bearing 95 :range 0}])

(facts
  "about phasers hitting klingons"
  (fact
    "no shots"
    (let [world {:shots []
                 :klingons [{:x 0 :y 0}]}
          world (update-klingon-hits world)
          klingons (:klingons world)
          shots (:shots world)]
      shots => []
      klingons => [{:x 0 :y 0}]))

  (fact
    "shot out of range"
    (let [world {:shots [{:x (inc phaser-proximity) :y 0 :bearing 0 :range ..range.. :type :phaser}]
                 :klingons [{:x 0 :y 0}]}
          world (update-klingon-hits world)
          klingons (:klingons world)
          shots (:shots world)]
      shots => [{:x (inc phaser-proximity) :y 0 :bearing 0 :range ..range.. :type :phaser}]
      klingons => [{:x 0 :y 0}]))

  (fact
    "phaser shots hit klingon"
    (let [world {:shots [{:x (dec phaser-proximity) :y 0 :bearing 0 :range ..range.. :type :phaser}]
                 :klingons [{:x 0 :y 0}] :explosions [:before]}
          world (update-klingon-hits world)
          klingons (:klingons world)
          shots (:shots world)
          explosions (:explosions world)]
      shots => []
      klingons => [{:x 0 :y 0 :hit {:weapon :phaser :damage [..range..]}}]
      (first explosions) => :before
      (dissoc (second explosions) :fragments) => {:x (dec phaser-proximity) :y 0 :type :phaser :age 0}))

  (fact
    "Two phaser shots, one hits klingon"
    (let [world {:shots [{:x (inc phaser-proximity) :y 0 :bearing 0 :range ..range.. :type :phaser}
                         {:x (dec phaser-proximity) :y 0 :bearing 0 :range ..range.. :type :phaser}]
                 :klingons [{:x 0 :y 0}]
                 :explosions [:before]}
          world (update-klingon-hits world)
          klingons (:klingons world)
          shots (:shots world)
          explosions (:explosions world)]
      shots => [{:x (inc phaser-proximity) :y 0 :bearing 0 :range ..range.. :type :phaser}]
      klingons => [{:x 0 :y 0 :hit {:weapon :phaser :damage [..range..]}}]
      (first explosions) => :before
      (dissoc (second explosions) :fragments) => {:x (dec phaser-proximity) :y 0 :type :phaser :age 0}))

  (fact
    "Two phaser shots, both hit klingon"
    (let [world {:shots [{:x (dec phaser-proximity) :y 0 :bearing 0 :range ..range.. :type :phaser}
                         {:x (dec phaser-proximity) :y 0 :bearing 1 :range ..range.. :type :phaser}]
                 :klingons [{:x 0 :y 0}]
                 :explosions [:before]}
          world (update-klingon-hits world)
          klingons (:klingons world)
          shots (:shots world)
          explosions (:explosions world)]
      shots => []
      klingons => [{:x 0 :y 0 :hit {:weapon :phaser :damage [..range.. ..range..]}}]
      (nth explosions 0) => :before
      (dissoc (nth explosions 1) :fragments) => {:x (dec phaser-proximity) :y 0 :type :phaser :age 0}
      (dissoc (nth explosions 2) :fragments) => {:x (dec phaser-proximity) :y 0 :type :phaser :age 0}))
  )

(facts
  "about Torpedos hitting klingons"
  (fact
    "no torpedo"
    (let [world {:shots []
                 :klingons [{:x 0 :y 0}]}
          world (update-klingon-hits world)
          klingons (:klingons world)
          shots (:shots world)]
      shots => []
      klingons => [{:x 0 :y 0}]))

  (fact
    "Torpedo out of range"
    (let [world {:shots [{:x (inc torpedo-proximity) :y 0 :bearing 0 :range ..range.. :type :torpedo}]
                 :klingons [{:x 0 :y 0}]}
          world (update-klingon-hits world)
          klingons (:klingons world)
          shots (:shots world)]
      shots => [{:x (inc torpedo-proximity) :y 0 :bearing 0 :range ..range.. :type :torpedo}]
      klingons => [{:x 0 :y 0}]))

  (fact
    "Torpedo hits klingon"
    (let [world {:shots [{:x (dec torpedo-proximity) :y 0 :bearing 0 :range ..range.. :type :torpedo}]
                 :klingons [{:x 0 :y 0}]
                 :explosions [:before]}
          world (update-klingon-hits world)
          klingons (:klingons world)
          shots (:shots world)
          explosions (:explosions world)]
      shots => []
      klingons => [{:x 0 :y 0 :hit {:weapon :torpedo :damage torpedo-damage}}]
      (first explosions) => :before
      (dissoc (second explosions) :fragments) => {:x (dec torpedo-proximity) :y 0 :type :torpedo :age 0}))

  (fact
    "Two torpedoes, one hits klingon"
    (let [world {:shots [{:x (inc torpedo-proximity) :y 0 :bearing 0 :range ..range.. :type :torpedo}
                         {:x (dec torpedo-proximity) :y 0 :bearing 0 :range ..range.. :type :torpedo}]
                 :klingons [{:x 0 :y 0}]}
          world (update-klingon-hits world)
          klingons (:klingons world)
          shots (:shots world)]
      shots => [{:x (inc torpedo-proximity) :y 0 :bearing 0 :range ..range.. :type :torpedo}]
      klingons => [{:x 0 :y 0 :hit {:weapon :torpedo :damage torpedo-damage}}]))

  (fact
    "Two torpedoes, both hit klingon"
    (let [world {:shots [{:x (dec torpedo-proximity) :y 0 :bearing 0 :range ..range.. :type :torpedo}
                         {:x (dec torpedo-proximity) :y 0 :bearing 0 :range ..range.. :type :torpedo}]
                 :klingons [{:x 0 :y 0}]}
          world (update-klingon-hits world)
          klingons (:klingons world)
          shots (:shots world)]
      shots => []
      klingons => [{:x 0 :y 0 :hit {:weapon :torpedo :damage (* 2 torpedo-damage)}}]))
  )

(facts
  "about kinetics hitting klingons"
  (fact
    "no kinetic"
    (let [world {:shots []
                 :klingons [{:x 0 :y 0}]}
          world (update-klingon-hits world)
          klingons (:klingons world)
          shots (:shots world)]
      shots => []
      klingons => [{:x 0 :y 0}]))

  (fact
    "Kinetic out of range"
    (let [world {:shots [{:x (inc kinetic-proximity) :y 0 :bearing 0 :range ..range.. :type :kinetic}]
                 :klingons [{:x 0 :y 0}]}
          world (update-klingon-hits world)
          klingons (:klingons world)
          shots (:shots world)]
      shots => [{:x (inc kinetic-proximity) :y 0 :bearing 0 :range ..range.. :type :kinetic}]
      klingons => [{:x 0 :y 0}]))

  (fact
    "Kinetic hits klingon"
    (let [world {:shots [{:x (dec kinetic-proximity) :y 0 :bearing 0 :range ..range.. :type :kinetic}]
                 :klingons [{:x 0 :y 0}]
                 :explosions []}
          world (update-klingon-hits world)
          klingons (:klingons world)
          shots (:shots world)
          explosions (:explosions world)]
      shots => []
      klingons => [{:x 0 :y 0 :hit {:weapon :kinetic :damage kinetic-damage}}]
      (dissoc (first explosions) :fragments) => {:x (dec kinetic-proximity) :y 0 :type :kinetic :age 0}))

  (fact
    "Two kinetics, one hits klingon"
    (let [world {:shots [{:x (inc kinetic-proximity) :y 0 :bearing 0 :range ..range.. :type :kinetic}
                         {:x (dec kinetic-proximity) :y 0 :bearing 0 :range ..range.. :type :kinetic}]
                 :klingons [{:x 0 :y 0}]}
          world (update-klingon-hits world)
          klingons (:klingons world)
          shots (:shots world)]
      shots => [{:x (inc kinetic-proximity) :y 0 :bearing 0 :range ..range.. :type :kinetic}]
      klingons => [{:x 0 :y 0 :hit {:weapon :kinetic :damage kinetic-damage}}]))

  (fact
    "Two kinetics, both hit klingon"
    (let [world {:shots [{:x (dec kinetic-proximity) :y 0 :bearing 0 :range ..range.. :type :kinetic}
                         {:x (dec kinetic-proximity) :y 0 :bearing 0 :range ..range.. :type :kinetic}]
                 :klingons [{:x 0 :y 0}]}
          world (update-klingon-hits world)
          klingons (:klingons world)
          shots (:shots world)]
      shots => []
      klingons => [{:x 0 :y 0 :hit {:weapon :kinetic :damage (* 2 kinetic-damage)}}]))
  )

(fact
  "klingon shot does not hit klingon"
  (let [world {:shots [{:x (dec kinetic-proximity) :y 0 :bearing 0 :range ..range.. :type :klingon-kinetic}]
               :klingons [{:x 0 :y 0}]
               :explosions []}
        world (update-klingon-hits world)
        klingons (:klingons world)
        shots (:shots world)
        explosions (:explosions world)]
    (count shots) => 1
    klingons => [{:x 0 :y 0}]
    explosions => empty?))

(fact
  "shot constructor"
  (->shot 0 0 180 :kinetic) => mom/valid-shot?)

(fact
  "klingon-kinetic misses ship"
  (let [ship (mom/make-ship)
        shot (->shot 0 (inc klingon-kinetic-proximity)
                     180 :klingon-kinetic)
        world (assoc (mom/make-world) :shots [shot] :ship ship)
        new-world (update-ship-hits world)]
    (->> new-world :explosions) => []
    (->> new-world :ship :shields) => ship-shields
    (count (->> new-world :shots)) => 1))

(fact
  "klingon-kinetic hits ship"
  (let [ship (mom/make-ship)
        shot (->shot 0 (dec klingon-kinetic-proximity)
                     180 :klingon-kinetic)
        world (assoc (mom/make-world) :shots [shot] :ship ship)
        new-world (update-ship-hits world)]
    (count (->> new-world :explosions)) => 1
    (->> new-world :ship :shields) => (- ship-shields klingon-kinetic-damage)
    (count (->> new-world :shots)) => 0))

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
  (/ ship-shields 2) 100 0
  (/ ship-shields 4) 100 50
  0 100 100
  )

(fact
  "damage can be incurred"
  (incur-damage 100 :y {:x 5}) => {:x 5}
  (incur-damage 100 :x {:x 0}) => {:x 100}
  (incur-damage 50 :x {:x 20}) => {:x 70}
  (incur-damage 50 :x {:x 80}) => {:x 100}
  )

