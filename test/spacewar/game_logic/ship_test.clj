(ns spacewar.game-logic.ship-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.ship :refer :all]
            [spacewar.vector-test :as vt]
            [spacewar.vector :as vector]
            [midje.experimental :refer [for-all]]
            [clojure.spec.alpha :as s]))

(facts
  "ship"
  (fact
    "rotate direction"
    (rotation-direction 0 0) => 0
    (rotation-direction 0 1) => 1
    (rotation-direction 1 0) => -1
    (rotation-direction 0 180) => 180
    (rotation-direction 0 181) => -179
    (rotation-direction 45 350) => -55
    (rotation-direction 350 45) => 55
    (rotation-direction 180 181) => 1)

  (fact
    "rotation timing"
    (let [dps (* 1000 rotation-rate)]
      (rotate-ship 1000 0 90) => (roughly dps)
      (rotate-ship 1000 90 0) => (roughly (- 90 dps))))

  (fact
    "rotation will not pass desired heading"
    (rotate-ship 1000 89 90) => 90
    (rotate-ship 1000 90 89) => 89)

  (fact
    "drag"
    (drag [0 0]) => (vt/roughly-v [0 0])
    (drag [0 1]) => (vt/roughly-v [0 (- drag-factor)])
    (drag [1 0]) => (vt/roughly-v [(- drag-factor) 0])
    (drag [2 0]) => (vt/roughly-v [(* -4 drag-factor) 0])
    (drag [1 1]) => (vt/roughly-v
                      [(* -1 drag-factor (Math/sqrt 2))
                       (* -1 drag-factor (Math/sqrt 2))])
    )

  (fact
    "Apply drag properties"
    (for-all
      [velocity (s/gen ::vector/vector)
       drag (s/gen ::vector/vector)]
      (apply-drag drag velocity) => #(s/valid? ::vector/vector %)))

  (fact
    "Apply drag values"
    (apply-drag [-1 -1] [2 2]) => (vt/roughly-v [1 1])
    (apply-drag [-2 -2] [1 1]) => (vt/roughly-v [0 0])
    )

  (tabular
    (fact
      "apply impulse"
      (apply-impulse ?ms ?velocity ?heading ?impulse) => ?new-velocity)
    ?ms ?velocity ?heading ?impulse ?new-velocity
    1000 [0 0] 0 0 (vt/roughly-v [0 0])
    1000 [0 0] 0 1 (vt/roughly-v [(* impulse-thrust ?ms ?impulse) 0])
    1000 [0 0] 90 1 (vt/roughly-v [0 (* impulse-thrust ?ms ?impulse)])
    500 [0 0] 90 2 (vt/roughly-v [0 (* impulse-thrust ?ms ?impulse)])
    1000 [1 1] 180 3 (vt/roughly-v (vector/add
                                     ?velocity
                                     [(* -1 impulse-thrust ?ms ?impulse) 0])))

  (fact
    "weapon-fire-handler"
    (weapon-fire-handler [] {:selected-weapon :phaser
                             :weapon-spread-setting 1
                             :weapon-number-setting 2
                             :target-bearing 90
                             :phaser-shots [:previous-shots]}) => {:selected-weapon :phaser
                                                                   :weapon-spread-setting 1
                                                                   :weapon-number-setting 2
                                                                   :target-bearing 90
                                                                   :phaser-shots [:previous-shots :shot1 :shot2]}
    (provided (fire-phasers
                anything
                anything
                anything
                anything) => [:shot1 :shot2]))

  (fact
    "fire-phasers"
    (fire-phasers [0 0] 0 1 0) => [{:x 0 :y 0 :bearing 0 :range 0}]
    (fire-phasers [1 1] 90 1 0) => [{:x 1 :y 1 :bearing 90 :range 0}]
    (fire-phasers [0 0] 90 2 10) => [{:x 0 :y 0 :bearing 85 :range 0}
                                     {:x 0 :y 0 :bearing 95 :range 0}]

    )

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

  (fact
    "phaser shots go out of range"
    (let [ms-out-of-range (+ 1 (/ phaser-range phaser-velocity))]
      (update-phaser-shot
        ms-out-of-range
        {:x 0 :y 0 :bearing 0 :range 0}) => nil))
  )

