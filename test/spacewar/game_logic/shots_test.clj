(ns spacewar.game-logic.shots-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.shots :refer :all]))

(facts
  "about shot movement"
  (tabular
    (fact
      "update-phaser-shot"
      (let [shot
            (update-phaser-shot
              ?ms {:x ?x :y ?y :bearing ?bearing :range 0})]
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
      (let [shot
            (update-kinetic-shot
              ?ms {:x ?x :y ?y :bearing ?bearing :range 0})]
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
    (let [ms-out-of-range (+ 1 (/ phaser-range phaser-velocity))]
      (update-phaser-shot
        ms-out-of-range
        {:x 0 :y 0 :bearing 0 :range 0}) => nil))

  (tabular
    (fact
      "update-torpedo-shot"
      (let [shot
            (update-torpedo-shot
              ?ms {:x ?x :y ?y :bearing ?bearing :range ?range})]
        (:x shot) => (roughly ?sx 1e-10)
        (:y shot) => (roughly ?sy 1e-10)
        (:bearing shot) => (roughly ?bearing 1e-10)
        (:range shot) => (roughly
                           (+ ?range
                              (* ?ms torpedo-velocity
                                 (- 1 (/ ?range torpedo-range)))))))
    ?ms ?x ?y ?bearing ?range ?sx ?sy
    1000 0 0 0 0 (* torpedo-velocity ?ms) 0
    1000 0 0 90 0 0 (* torpedo-velocity ?ms)
    1000 0 0 0 (/ torpedo-range 2) (* torpedo-velocity ?ms 0.5) 0))

(fact
  "fire-weapon"
  (fire-weapon [0 0] 0 1 0) => [{:x 0 :y 0 :bearing 0 :range 0}]
  (fire-weapon [1 1] 90 1 0) => [{:x 1 :y 1 :bearing 90 :range 0}]
  (fire-weapon [0 0] 90 2 10) => [{:x 0 :y 0 :bearing 85 :range 0} {:x 0 :y 0 :bearing 95 :range 0}])

(facts
  "about phasers hitting klingons"
  (fact
  "no shots"
    (let [world {:phaser-shots []
                 :klingons [{:x 0 :y 0}]}
          world (update-phaser-klingon-hits world)
          klingons (:klingons world)
          shots (:phaser-shots world)]
      shots => []
      klingons => [{:x 0 :y 0}]))

  (fact
  "shot out of range"
    (let [world {:phaser-shots [{:x (inc phaser-proximity) :y 0 :bearing 0 :range ..range..}]
                 :klingons [{:x 0 :y 0}]}
          world (update-phaser-klingon-hits world)
          klingons (:klingons world)
          shots (:phaser-shots world)]
      shots => [{:x (inc phaser-proximity) :y 0 :bearing 0 :range ..range..}]
      klingons => [{:x 0 :y 0}]))

  (fact
    "phaser shots hit klingon"
    (let [world {:phaser-shots [{:x (dec phaser-proximity) :y 0 :bearing 0 :range ..range..}]
                 :klingons [{:x 0 :y 0}]}
          world (update-phaser-klingon-hits world)
          klingons (:klingons world)
          shots (:phaser-shots world)]
      shots => []
      klingons => [{:x 0 :y 0 :hit {:weapon :phaser :damage [..range..]}}]))

  (fact
      "Two phaser shots, one hits klingon"
      (let [world {:phaser-shots [{:x (inc phaser-proximity) :y 0 :bearing 0 :range ..range..}
                                  {:x (dec phaser-proximity) :y 0 :bearing 0 :range ..range..}]
                   :klingons [{:x 0 :y 0}]}
            world (update-phaser-klingon-hits world)
            klingons (:klingons world)
            shots (:phaser-shots world)]
        shots => [{:x (inc phaser-proximity) :y 0 :bearing 0 :range ..range..}]
        klingons => [{:x 0 :y 0 :hit {:weapon :phaser :damage [..range.. ..range..]}}]))

  )


