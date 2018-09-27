(ns spacewar.game-logic.shots-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.shots :refer :all]))

(facts
  "about shots"
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
    1000 0 0 0 (/ torpedo-range 2) (* torpedo-velocity ?ms 0.5) 0
    )
  )


