(ns spacewar.game-logic.ship-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.ship :refer :all]
            [spacewar.vector-test :as vt]
            [spacewar.vector :as vector]
            [midje.experimental :refer [for-all]]
            [clojure.spec.alpha :as s]
            [spacewar.game-logic.test-mother :as mom]))

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
    (let [dps (* 1000 rotation-rate)
          ship (mom/make-ship)
          ship1 (assoc ship :heading-setting 90)
          ship2 (assoc ship :heading 90 :heading-setting 0)]
      (:heading (rotate-ship 1000 ship1)) => (roughly dps)
      (:heading (rotate-ship 1000 ship2)) => (roughly (- 90 dps))))

  (fact
    "rotation will not pass desired heading"
    (let [ship (mom/make-ship)
          ship1 (assoc ship :heading 89 :heading-setting 90)
          ship2 (assoc ship :heading 90 :heading-setting 89)]
      (:heading (rotate-ship 1000 ship1)) => 90
      (:heading (rotate-ship 1000 ship2)) => 89))

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
                                     [(* -1 impulse-thrust ?ms ?impulse) 0]))))

(fact
  "sheilds recharge from antimatter"
  (let [ship (mom/make-ship)
        ship (assoc ship :shields 0)
        ms 10
        recharged-ship (charge-shields ms ship)
        charge (* ms ship-shield-recharge-rate)]
    (:shields recharged-ship) => (roughly charge)
    (:antimatter recharged-ship) => (roughly (- ship-antimatter charge))))

(fact
  "shields cant charge when antimatter is gone"
  (let [ship (mom/make-ship)
        ship (assoc ship :shields 0 :antimatter 0)
        ms 10
        recharged-ship (charge-shields ms ship)]
    (:shields recharged-ship) => (roughly 0)
    (:antimatter recharged-ship) => (roughly 0)))

(fact
  "shields cant charge beyond ship-shields"
  (let [ship (mom/make-ship)
        ship (assoc ship :shields ship-shields :antimatter 100)
        ms 10
        recharged-ship (charge-shields ms ship)]
    (:shields recharged-ship) => ship-shields
    (:antimatter recharged-ship) => 100))

(fact
  "Ship is not dockable if not within ship-docking-distance of a base"
  (let [ship (mom/make-ship)
        base (mom/set-pos {} [0 (inc ship-docking-distance)])]
    (dockable? ship [base]) => false))

(fact
  "Ship is dockable if within ship-docking-distance of a base"
  (let [ship (mom/make-ship)
        base (mom/set-pos {} [0 (dec ship-docking-distance)])]
    (dockable? ship [base]) => true))

(facts
  "damage repair"
  (let [ship (mom/make-ship)]
    (fact
      "repair capacity normal when undamaged"
      (repair-capacity 10 ship) => (* 10 ship-repair-capacity))
    (fact
      "repair capacity proportional to life-support damage"
      (let [ship (assoc ship :life-support-damage 50)]
        (repair-capacity 10 ship) => (* 5 ship-repair-capacity)))
    (fact
      "life support damage is repaired"
      (let [ship (assoc ship :life-support-damage 50)]
        (:life-support-damage (repair-ship 1 ship)) => 40
        (provided (repair-capacity anything anything) => 10)))
    (fact
      "life support damage is not over-repaired"
      (let [ship (assoc ship :life-support-damage 50)]
        (:life-support-damage (repair-ship 1 ship)) => 0
        (provided (repair-capacity anything anything) => 60)))
    (fact
      "hull is repaired with remainder after life support"
      (let [ship (assoc ship :life-support-damage 50
                             :hull-damage 50)]
        (select-keys
          (repair-ship 1 ship)
          [:life-support-damage
           :hull-damage]) => {:life-support-damage 0
                              :hull-damage 40}
        (provided (repair-capacity anything anything) => 60)))
    (fact
      "all are repaired in order"
      (let [ship (assoc ship :life-support-damage 10
                             :hull-damage 10
                             :warp-damage 10
                             :weapons-damage 10
                             :impulse-damage 10
                             :sensor-damage 10)]
        (select-keys
          (repair-ship 1 ship)
          [:life-support-damage
           :hull-damage
           :warp-damage
           :weapons-damage
           :impulse-damage
           :sensor-damage]) => {:life-support-damage 0
                                :hull-damage 0
                                :warp-damage 0
                                :weapons-damage 0
                                :sensor-damage 0
                                :impulse-damage 5}
        (provided (repair-capacity anything anything) => 55))
      )

    ))
