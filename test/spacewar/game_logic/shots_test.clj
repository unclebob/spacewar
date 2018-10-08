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


