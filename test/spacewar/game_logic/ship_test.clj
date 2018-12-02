(ns spacewar.game-logic.ship-test
  (:require [midje.sweet :refer [facts fact tabular => roughly anything]]
            [spacewar.game-logic.config :refer [rotation-rate
                                                drag-factor
                                                impulse-thrust
                                                ship-shield-recharge-rate
                                                ship-antimatter
                                                ship-shields
                                                ship-docking-distance
                                                ship-dilithium
                                                ship-kinetics
                                                ship-torpedos
                                                ship-deploy-distance
                                                ship-repair-capacity
                                                ship-dilithium-consumption
                                                antimatter-to-heat
                                                dilithium-heat-dissipation
                                                base-deployment-antimatter
                                                base-deployment-dilithium
                                                ]]
            [spacewar.game-logic.ship :refer [rotation-direction
                                              rotate-ship
                                              drag
                                              apply-drag
                                              apply-impulse
                                              charge-shields
                                              dockable?
                                              dock-ship
                                              deployable?
                                              repair-capacity
                                              repair-ship
                                              update-destruction
                                              calc-dilithium-consumed
                                              update-ship
                                              heat-core
                                              dissipate-core-heat
                                              deploy-base]]
            [spacewar.vector-test :as vt]
            [spacewar.vector :as vector]
            [midje.experimental :refer [for-all]]
            [clojure.spec.alpha :as s]
            [spacewar.game-logic.test-mother :as mom]
            [spacewar.game-logic.bases :as bases]))

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

(fact
  "Docking replenishes fuel and weapons and subtracts from in-range base."
  (let [world (mom/make-world)
        ship (:ship world)
        ship (assoc ship :kinetics 0 :torpedos 1 :antimatter 2 :dilithium 3)
        base (mom/make-base 0 (dec ship-docking-distance) :weapon-factory
                            ship-antimatter ship-dilithium
                            ship-kinetics ship-torpedos)
        bases [base]
        world (assoc world :ship ship :bases bases)
        world (dock-ship [] world)
        {:keys [kinetics torpedos antimatter dilithium]} (:ship world)
        base (first (:bases world))]
    kinetics => ship-kinetics
    torpedos => ship-torpedos
    antimatter => ship-antimatter
    dilithium => ship-dilithium
    (:antimatter base) => 2
    (:dilithium base) => 3
    (:torpedos base) => 1
    (:kinetics base) => 0))

(fact
  "Docking at undersupplied bases replenishes some fuel and weapons and subtracts from in-range base."
  (let [world (mom/make-world)
        ship (:ship world)
        ship (assoc ship :kinetics 0 :torpedos 0 :antimatter 0 :dilithium 0)
        base (mom/make-base)
        base (assoc base :x 0 :y (dec ship-docking-distance)
                         :type :weapon-factory
                         :antimatter 1 :dilithium 2
                         :kinetics 3 :torpedos 4)
        bases [base]
        world (assoc world :ship ship :bases bases)
        world (dock-ship [] world)
        {:keys [kinetics torpedos antimatter dilithium]} (:ship world)
        base (first (:bases world))]
    kinetics => 3
    torpedos => 4
    antimatter => 1
    dilithium => 2
    (:antimatter base) => 0
    (:dilithium base) => 0
    (:torpedos base) => 0
    (:kinetics base) => 0))

(fact
  "Docking at several undersupplied bases replenishes some fuel and weapons and subtracts from in-range base."
  (let [world (mom/make-world)
        ship (:ship world)
        ship (assoc ship :kinetics 0 :torpedos 0 :antimatter 0 :dilithium 0)
        base1 (mom/make-base)
        base2 (mom/make-base)
        base1 (assoc base1 :x 0 :y (dec ship-docking-distance)
                           :type :weapon-factory
                           :antimatter 1 :dilithium 2
                           :kinetics 3 :torpedos 4)
        base2 (assoc base2 :x 0 :y (dec ship-docking-distance)
                           :type :weapon-factory
                           :antimatter 2 :dilithium 3
                           :kinetics 4 :torpedos 5)
        bases [base1 base2]
        world (assoc world :ship ship :bases bases)
        world (dock-ship [] world)
        {:keys [kinetics torpedos antimatter dilithium]} (:ship world)
        base1 (first (:bases world))
        base2 (second (:bases world))]
    kinetics => 7
    torpedos => 9
    antimatter => 3
    dilithium => 5
    (:antimatter base1) => 0
    (:dilithium base1) => 0
    (:torpedos base1) => 0
    (:kinetics base1) => 0
    (:antimatter base2) => 0
    (:dilithium base2) => 0
    (:torpedos base2) => 0
    (:kinetics base2) => 0))

(facts
  "about deployment"
  (let [ship (mom/make-ship)]
    (fact
      "no stars close, nothing deployable"
      (deployable? :antimatter-factory ship []) => false
      (deployable? :dilithium-factory ship []) => false
      (deployable? :weapon-factory ship []) => false)

    (fact
      "near class O, antimatter-factory deployable"
      (let [star (mom/make-star (dec ship-deploy-distance) 0 :o)]
        (deployable? :antimatter-factory ship [star]) => true
        (deployable? :dilithium-factory ship [star]) => false
        (deployable? :weapon-factory ship [star]) => false
        ))
    (fact
      "near class B, antimatter-factory deployable"
      (let [star (mom/make-star (dec ship-deploy-distance) 0 :b)]
        (deployable? :antimatter-factory ship [star]) => true
        (deployable? :dilithium-factory ship [star]) => false
        (deployable? :weapon-factory ship [star]) => false
        ))
    (fact
      "near class A, antimatter-factory deployable"
      (let [star (mom/make-star (dec ship-deploy-distance) 0 :a)]
        (deployable? :antimatter-factory ship [star]) => true
        (deployable? :dilithium-factory ship [star]) => false
        (deployable? :weapon-factory ship [star]) => false
        ))
    (fact
      "near class F, weapon-factory deployable"
      (let [star (mom/make-star (dec ship-deploy-distance) 0 :f)]
        (deployable? :antimatter-factory ship [star]) => false
        (deployable? :dilithium-factory ship [star]) => false
        (deployable? :weapon-factory ship [star]) => true
        ))
    (fact
      "near class G, weapon-factory deployable"
      (let [star (mom/make-star (dec ship-deploy-distance) 0 :g)]
        (deployable? :antimatter-factory ship [star]) => false
        (deployable? :dilithium-factory ship [star]) => false
        (deployable? :weapon-factory ship [star]) => true
        ))
    (fact
      "near class K, dilithium-factory deployable"
      (let [star (mom/make-star (dec ship-deploy-distance) 0 :k)]
        (deployable? :antimatter-factory ship [star]) => false
        (deployable? :dilithium-factory ship [star]) => true
        (deployable? :weapon-factory ship [star]) => false
        ))
    (fact
      "near class M, dilithium-factory deployable"
      (let [star (mom/make-star (dec ship-deploy-distance) 0 :m)]
        (deployable? :antimatter-factory ship [star]) => false
        (deployable? :dilithium-factory ship [star]) => true
        (deployable? :weapon-factory ship [star]) => false
        ))))

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
      )))

(facts
  "Ship destroyed"
  (let [ship (mom/make-ship)]
    (fact
      "ship destroyed if life support damage is 100%"
      (let [ship (assoc ship :life-support-damage 100)
            destroyed-ship (update-destruction ship)]
        (:destroyed destroyed-ship) => true))

    (fact
      "ship destroyed if hull damage is 100%"
      (let [ship (assoc ship :hull-damage 100)
            destroyed-ship (update-destruction ship)]
        (:destroyed destroyed-ship) => true))

    (fact
      "ship not destroyed if life support and hull damage is less than 100%"
      (let [ship (assoc ship :life-support-damage 99
                             :hull-damage 99)
            destroyed-ship (update-destruction ship)]
        (:destroyed destroyed-ship) => false))))

(facts
  "dilithium"
  (let [world (mom/make-world)
        ship (:ship world)]
    (fact
      "ship not under warp does not consumes dilithium"
      (prerequisite
        (calc-dilithium-consumed anything anything) => 1)
      (let [ship (assoc ship :warp 0)
            world (assoc world :ship ship)
            world (update-ship 1 world)
            warped-ship (:ship world)
            dilithium (:dilithium warped-ship)]
        dilithium => ship-dilithium))
    (fact
      "ship under warp consumes dilithium"
      (prerequisite
        (calc-dilithium-consumed anything anything) => 1)
      (let [ship (assoc ship :warp 1)
            world (assoc world :ship ship)
            world (update-ship 1 world)
            warped-ship (:ship world)
            dilithium (:dilithium warped-ship)]
        dilithium => (roughly (- ship-dilithium 1) 1e-10)))
    (fact
      "dilithium never goes negative."
      (prerequisite
        (calc-dilithium-consumed anything anything) => 2)
      (let [ship (assoc ship :warp 1 :dilithium ship-dilithium-consumption)
            world (assoc world :ship ship)
            world (update-ship 1 world)
            warped-ship (:ship world)
            dilithium (:dilithium warped-ship)]
        dilithium => (roughly 0 1e-10)))
    )
  )

(facts
  "core temperature"
  (let [ship (mom/make-ship)]
    (fact
      "antimatter consumption heats core"
      (let [hot-ship (heat-core 100 ship)]
        (:core-temp hot-ship) => (* 100 antimatter-to-heat)))

    (fact
      "dissipate heat with full dilithium"
      (let [hot-ship (assoc ship :core-temp 50)
            cool-ship (dissipate-core-heat 1 hot-ship)]
        (:core-temp cool-ship) => (* 50 (- 1 dilithium-heat-dissipation))))

    (fact
      "dissipate heat with no dilithium"
      (let [hot-ship (assoc ship :core-temp 50
                                 :dilithium 0)
            cool-ship (dissipate-core-heat 1 hot-ship)]
        (:core-temp cool-ship) => (roughly 50 1e-10)))

    (fact
      "dissipate heat with half dilithium"
      (let [hot-ship (assoc ship :core-temp 50
                                 :dilithium (/ ship-dilithium 2))
            cool-ship (dissipate-core-heat 1 hot-ship)]
        (:core-temp cool-ship) => (roughly (* 50
                                              (- 1 (* dilithium-heat-dissipation
                                                      (/ (Math/sqrt 2) 2)))) 1e-10)))
    ))

(fact
  "cannot deploy base if ship not near star"
  (let [world (mom/make-world)
        ship (:ship world)
        star (mom/make-star (inc ship-deploy-distance) 0 :o)
        world (assoc world :stars [star])
        world (deploy-base :antimatter-factory world)
        bases (:bases world)
        new-ship (:ship world)]
    (count bases) => 0
    new-ship => ship
    (-> world :messages count) => 1))

(fact
  "cannot deploy base if insufficient antimatter"
  (let [world (mom/make-world)
        ship (:ship world)
        ship (assoc ship :antimatter (dec base-deployment-antimatter))
        star (mom/make-star (dec ship-deploy-distance) 0 :o)
        world (assoc world :ship ship :stars [star])
        world (deploy-base :antimatter-factory world)
        bases (:bases world)
        new-ship (:ship world)]
    (count bases) => 0
    new-ship => ship
    (-> world :messages count) => 1))

(fact
  "cannot deploy base if insufficient dilithium"
  (let [world (mom/make-world)
        ship (:ship world)
        ship (assoc ship :dilithium (dec base-deployment-dilithium))
        star (mom/make-star (dec ship-deploy-distance) 0 :o)
        world (assoc world :ship ship :stars [star])
        world (deploy-base :antimatter-factory world)
        bases (:bases world)
        new-ship (:ship world)]
    (count bases) => 0
    new-ship => ship
    (-> world :messages count) => 1))

(fact
  "cannot deploy base if another base is already deployed at that star"
  (let [world (mom/make-world)
        ship (:ship world)
        star (mom/make-star (dec ship-deploy-distance) 0 :o)
        base (mom/make-base (:x star) (+ (:y star) (dec ship-deploy-distance)) :antimatter-factory 0 0)
        world (assoc world :stars [star] :bases [base])
        world (deploy-base :antimatter-factory world)
        bases (:bases world)
        new-ship (:ship world)]
    (count bases) => 1
    new-ship => ship
    (-> world :messages count) => 1))

(fact
  "can deploy base if ship near star"
  (let [world (mom/make-world)
        star (mom/make-star (dec ship-deploy-distance) 0 :o)
        world (assoc world :stars [star])
        world (deploy-base :antimatter-factory world)
        bases (:bases world)
        ship (:ship world)]
    (count bases) => 1
    (first bases) => (bases/make-base [0 0] :antimatter-factory)
    (:antimatter ship) => (- ship-antimatter base-deployment-antimatter)
    (:dilithium ship) => (- ship-dilithium base-deployment-dilithium)))

