(ns spacewar.game-logic.ship-spec
  (:require [spacewar.game-logic.bases :as bases]
            [spacewar.game-logic.config :refer [antimatter-to-heat
                                                base-deployment-antimatter
                                                base-deployment-dilithium
                                                dilithium-heat-dissipation
                                                drag-factor
                                                impulse-thrust
                                                rotation-rate
                                                ship-antimatter
                                                ship-deploy-distance
                                                ship-dilithium
                                                ship-dilithium-consumption
                                                ship-docking-distance
                                                ship-kinetics
                                                ship-repair-capacity
                                                ship-shield-recharge-rate
                                                ship-shields
                                                ship-torpedos]]
            [spacewar.game-logic.config :as glc]
            [spacewar.game-logic.ship :refer [add-transport-routes-to
                                              apply-drag
                                              apply-impulse
                                              charge-shields
                                              deploy-base
                                              deployable?
                                              dissipate-core-heat
                                              dock-ship
                                              dockable?
                                              drag
                                              heat-core
                                              repair-capacity
                                              repair-ship
                                              rotate-ship
                                              rotation-direction
                                              update-destruction
                                              update-ship]]
            [spacewar.game-logic.spec-mother :as mom]
            [spacewar.spec-utils :as ut]
            [spacewar.vector :as vector]
            [speclj.core :refer [context describe it should should-not should= stub with with-stubs]]))

(declare ship)
(describe "ship"
  (describe "rotate direction"
    (it "returns 0 when current and desired headings are 0"
      (should= 0 (rotation-direction 0 0)))
    (it "returns 1 when turning right from 0 to 1"
      (should= 1 (rotation-direction 0 1)))
    (it "returns -1 when turning left from 1 to 0"
      (should= -1 (rotation-direction 1 0)))
    (it "returns 180 when turning from 0 to 180"
      (should= 180 (rotation-direction 0 180)))
    (it "returns -179 when turning from 0 to 181"
      (should= -179 (rotation-direction 0 181)))
    (it "returns -55 when turning from 45 to 350"
      (should= -55 (rotation-direction 45 350)))
    (it "returns 55 when turning from 350 to 45"
      (should= 55 (rotation-direction 350 45)))
    (it "returns 1 when turning from 180 to 181"
      (should= 1 (rotation-direction 180 181))))

  (describe "rotation timing"
    (it "rotates ship toward 90 degrees over 1000ms"
      (let [dps (* 1000 rotation-rate)
            ship (mom/make-ship)
            ship1 (assoc ship :heading-setting 90)
            rotated-ship (rotate-ship 1000 ship1)]
        (should= dps (:heading rotated-ship) 0.0001)))
    (it "rotates ship back from 90 degrees over 1000ms"
      (let [dps (* 1000 rotation-rate)
            ship (mom/make-ship)
            ship2 (assoc ship :heading 90 :heading-setting 0)
            rotated-ship (rotate-ship 1000 ship2)]
        (should= (- 90 dps) (:heading rotated-ship) 0.0001))))

  (describe "rotation will not pass desired heading"
    (it "stops at 90 when close to heading-setting"
      (let [ship (mom/make-ship)
            ship1 (assoc ship :heading 89 :heading-setting 90)
            rotated-ship (rotate-ship 1000 ship1)]
        (should= 90 (:heading rotated-ship))))
    (it "stops at 89 when close to heading-setting"
      (let [ship (mom/make-ship)
            ship2 (assoc ship :heading 90 :heading-setting 89)
            rotated-ship (rotate-ship 1000 ship2)]
        (should= 89 (:heading rotated-ship)))))

  (describe "drag"
    (it "applies no drag to zero velocity"
      (should (ut/roughly-v [0 0] (drag [0 0]) 0.0001)))
    (it "applies drag to vertical velocity"
      (should (ut/roughly-v [0 (- drag-factor)] (drag [0 1]) 0.0001)))
    (it "applies drag to horizontal velocity"
      (should (ut/roughly-v [(- drag-factor) 0] (drag [1 0]) 0.0001)))
    (it "applies stronger drag to higher velocity"
      (should (ut/roughly-v [(* -4 drag-factor) 0] (drag [2 0]) 0.0001)))
    (it "applies drag to diagonal velocity"
      (let [expected [(* -1 drag-factor (Math/sqrt 2))
                      (* -1 drag-factor (Math/sqrt 2))]]
        (should (ut/roughly-v expected (drag [1 1]) 0.0001)))))

  (describe "apply drag values"
    (it "reduces velocity by drag"
      (should (ut/roughly-v [1 1] (apply-drag [-1 -1] [2 2]) 0.0001)))
    (it "stops velocity when drag exceeds it"
      (should (ut/roughly-v [0 0] (apply-drag [-2 -2] [1 1]) 0.0001))))

  (describe "apply impulse"
    (it "does nothing with zero impulse"
      (should (ut/roughly-v [0 0] (apply-impulse 1000 [0 0] 0 0) 0.0001)))
    (it "applies impulse eastward"
      (should (ut/roughly-v [(* impulse-thrust 1000 1) 0] (apply-impulse 1000 [0 0] 0 1) 0.0001)))
    (it "applies impulse northward"
      (should (ut/roughly-v [0 (* impulse-thrust 1000 1)] (apply-impulse 1000 [0 0] 90 1) 0.0001)))
    (it "applies half impulse northward"
      (should (ut/roughly-v [0 (* impulse-thrust 500 2)] (apply-impulse 500 [0 0] 90 2) 0.0001)))
    (it "applies impulse westward with initial velocity"
      (let [result (apply-impulse 1000 [1 1] 180 3)
            expected (vector/add [1 1] [(* -1 impulse-thrust 1000 3) 0])]
        (should (ut/roughly-v expected result 0.0001)))))

  (describe "shields recharge from antimatter"
    (it "recharges shields and consumes antimatter"
      (let [ship (mom/make-ship)
            ship (assoc ship :shields 0)
            ms 10
            recharged-ship (charge-shields ms ship)
            charge (* ms ship-shield-recharge-rate)]
        (should= charge (:shields recharged-ship) 0.0001)
        (should (ut/roughly= (- ship-antimatter charge) (:antimatter recharged-ship) 0.0001)))))

  (describe "shields cant charge when antimatter is gone"
    (it "does not recharge shields"
      (let [ship (mom/make-ship)
            ship (assoc ship :shields 0 :antimatter 0)
            ms 10
            recharged-ship (charge-shields ms ship)]
        (should (ut/roughly= 0 (:shields recharged-ship) 0.0001))
        (should (ut/roughly= 0 (:antimatter recharged-ship) 0.0001)))))

  (describe "shields cant charge beyond ship-shields"
    (it "stops at max shields"
      (let [ship (mom/make-ship)
            ship (assoc ship :shields ship-shields :antimatter 100)
            ms 10
            recharged-ship (charge-shields ms ship)]
        (should= ship-shields (:shields recharged-ship))
        (should= 100 (:antimatter recharged-ship)))))

  (describe "dockable?"
    (it "returns false if not within docking distance"
      (let [ship (mom/make-ship)
            base (mom/set-pos {} [0 (inc ship-docking-distance)])]
        (should-not (dockable? ship [base]))))
    (it "returns true if within docking distance"
      (let [ship (mom/make-ship)
            base (mom/set-pos {} [0 (dec ship-docking-distance)])]
        (should (dockable? ship [base])))))

  (describe "docking at corbomite device"
    (it "installs corbomite device and removes base"
      (let [world (mom/make-world)
            ship (:ship world)
            base (mom/make-base 0 (dec ship-docking-distance) :corbomite-device 0 0 0 0)
            bases [base]
            world (assoc world :ship ship :bases bases)
            world (dock-ship [] world)]
        (should (nil? (first (:bases world))))
        (should (:corbomite-device-installed (:ship world))))))

  (describe "docking replenishes resources"
    (it "fully replenishes ship from base"
      (let [world (mom/make-world)
            ship (:ship world)
            ship (assoc ship :kinetics 0 :torpedos 1 :antimatter 2 :dilithium 3)
            base (mom/make-base 0 (dec ship-docking-distance) :weapon-factory
                                ship-antimatter ship-dilithium ship-kinetics ship-torpedos)
            bases [base]
            world (assoc world :ship ship :bases bases)
            world (dock-ship [] world)
            {:keys [kinetics torpedos antimatter dilithium]} (:ship world)
            base (first (:bases world))]
        (should= ship-kinetics kinetics)
        (should= ship-torpedos torpedos)
        (should= ship-antimatter antimatter)
        (should= ship-dilithium dilithium)
        (should= 2 (:antimatter base))
        (should= 3 (:dilithium base))
        (should= 1 (:torpedos base))
        (should= 0 (:kinetics base)))))

  (describe "docking at undersupplied base"
    (it "partially replenishes ship"
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
        (should= 1 (count (:bases world)))
        (should= 3 kinetics)
        (should= 4 torpedos)
        (should= 1 antimatter)
        (should= 2 dilithium)
        (should= 0 (:antimatter base))
        (should= 0 (:dilithium base))
        (should= 0 (:torpedos base))
        (should= 0 (:kinetics base)))))

  (describe "docking at multiple undersupplied bases"
    (it "combines resources from multiple bases"
      (let [world (mom/make-world)
            ship (:ship world)
            ship (assoc ship :kinetics 0 :torpedos 0 :antimatter 0 :dilithium 0)
            base1 (assoc (mom/make-base) :x 0 :y (/ ship-docking-distance 2)
                                         :type :weapon-factory :antimatter 1 :dilithium 2
                                         :kinetics 3 :torpedos 4)
            base2 (assoc (mom/make-base) :x 1 :y (/ ship-docking-distance 2)
                                         :type :weapon-factory :antimatter 2 :dilithium 3
                                         :kinetics 4 :torpedos 5)
            bases [base1 base2]
            world (assoc world :ship ship :bases bases)
            world (dock-ship [] world)
            {:keys [kinetics torpedos antimatter dilithium]} (:ship world)]
        (should= 2 (count (:bases world)))
        (should= 7 kinetics)
        (should= 9 torpedos)
        (should= 3 antimatter)
        (should= 5 dilithium))))

  (describe "deployment"
    (let [ship (mom/make-ship)]
      (context "bases and stars"
        (it "returns false for all factories with no stars"
          (should-not (deployable? :antimatter-factory ship []))
          (should-not (deployable? :dilithium-factory ship []))
          (should-not (deployable? :weapon-factory ship []))
          (should-not (deployable? :corbomite-factory ship [])))
        (it "allows antimatter-factory near class O star"
          (let [star (mom/make-star (dec ship-deploy-distance) 0 :o)]
            (should (deployable? :antimatter-factory ship [star]))
            (should-not (deployable? :dilithium-factory ship [star]))
            (should-not (deployable? :weapon-factory ship [star]))
            (should-not (deployable? :corbomite-factory ship [star]))))
        (it "allows antimatter-factory near class B star"
          (let [star (mom/make-star (dec ship-deploy-distance) 0 :b)]
            (should (deployable? :antimatter-factory ship [star]))
            (should-not (deployable? :dilithium-factory ship [star]))
            (should-not (deployable? :weapon-factory ship [star]))
            (should-not (deployable? :corbomite-factory ship [star]))))
        (it "allows antimatter-factory near class A star"
          (let [star (mom/make-star (dec ship-deploy-distance) 0 :a)]
            (should (deployable? :antimatter-factory ship [star]))
            (should-not (deployable? :dilithium-factory ship [star]))
            (should-not (deployable? :weapon-factory ship [star]))
            (should-not (deployable? :corbomite-factory ship [star]))))
        (it "allows weapon-factory near class F star"
          (let [star (mom/make-star (dec ship-deploy-distance) 0 :f)]
            (should-not (deployable? :antimatter-factory ship [star]))
            (should-not (deployable? :dilithium-factory ship [star]))
            (should (deployable? :weapon-factory ship [star]))
            (should-not (deployable? :corbomite-factory ship [star]))))
        (it "allows weapon-factory near class G star"
          (let [star (mom/make-star (dec ship-deploy-distance) 0 :g)]
            (should-not (deployable? :antimatter-factory ship [star]))
            (should-not (deployable? :dilithium-factory ship [star]))
            (should (deployable? :weapon-factory ship [star]))
            (should-not (deployable? :corbomite-factory ship [star]))))
        (it "allows dilithium-factory near class K star"
          (let [star (mom/make-star (dec ship-deploy-distance) 0 :k)]
            (should-not (deployable? :antimatter-factory ship [star]))
            (should (deployable? :dilithium-factory ship [star]))
            (should-not (deployable? :weapon-factory ship [star]))
            (should-not (deployable? :corbomite-factory ship [star]))))
        (it "allows dilithium-factory near class M star"
          (let [star (mom/make-star (dec ship-deploy-distance) 0 :m)]
            (should-not (deployable? :antimatter-factory ship [star]))
            (should (deployable? :dilithium-factory ship [star]))
            (should-not (deployable? :weapon-factory ship [star]))
            (should-not (deployable? :corbomite-factory ship [star]))))
        (it "allows corbomite-factory near pulsar"
          (let [star (mom/make-star (dec ship-deploy-distance) 0 :pulsar)]
            (should-not (deployable? :antimatter-factory ship [star]))
            (should-not (deployable? :dilithium-factory ship [star]))
            (should-not (deployable? :weapon-factory ship [star]))
            (should (deployable? :corbomite-factory ship [star]))))))

    (context "transport routes"
      (it "does not add a route if there are no old bases"
        (let [new-y (inc glc/transport-range)
              new-base (mom/make-base 0 new-y :antimatter 1 1)
              world (assoc (mom/make-world) :bases [])
              world (add-transport-routes-to world new-base)]
          (should= #{} (:transport-routes world))))

      (it "does not add a route if bases are too far apart"
        (let [old-base (mom/make-base 0 0 :antimatter 1 1)
              new-y (inc glc/transport-range)
              new-base (mom/make-base 0 new-y :antimatter 1 1)
              world (assoc (mom/make-world) :bases [old-base])
              world (add-transport-routes-to world new-base)]
          (should= #{} (:transport-routes world))))

      (it "adds a route if bases are in range"
        (let [old-base (mom/make-base 0 0 :antimatter 1 1)
              new-y (dec glc/transport-range)
              new-base (mom/make-base 0 new-y :antimatter 1 1)
              world (assoc (mom/make-world) :bases [old-base])
              world (add-transport-routes-to world new-base)]
          (should= #{#{[0 0] [0 new-y]}} (:transport-routes world))))

      (it "adds more than one base in range"
        (let [dist (dec glc/transport-range)
              old-base1 (mom/make-base 0 dist :antimatter 1 1)
              old-base2 (mom/make-base dist 0 :antimatter 1 1)
              new-base (mom/make-base 0 0 :antimatter 1 1)
              world (assoc (mom/make-world) :bases [old-base1 old-base2])
              world (add-transport-routes-to world new-base)]
          (should= #{#{[0 0] [0 dist]}
                     #{[0 0] [dist 0]}} (:transport-routes world))))

      (it "adds only the closest bases if more than base-routes-limit are in range"
        (with-redefs [glc/base-routes-limit 2]
          (let [far (dec glc/transport-range)
                far-base (mom/make-base 0 far :antimatter 1 1)
                closer (dec far)
                closer-base (mom/make-base closer 0 :antimatter 1 1)
                closest (- far 2)
                closest-base (mom/make-base closest 0 :antimatter 1 1)
                new-base (mom/make-base 0 0 :antimatter 1 1)
                world (assoc (mom/make-world)
                        :bases [far-base closer-base closest-base])
                world (add-transport-routes-to world new-base)]
            (should= #{#{[0 0] [closest 0]}
                       #{[0 0] [closer 0]}} (:transport-routes world)))))
      )
    )

  (describe "damage repair"
    (with-stubs)
    (with ship (mom/make-ship))
    (it "returns full repair capacity when undamaged"
      (should= (* 10 ship-repair-capacity) (repair-capacity 10 @ship)))
    (it "reduces repair capacity with life-support damage"
      (let [ship (assoc @ship :life-support-damage 50)]
        (should= (* 5 ship-repair-capacity) (repair-capacity 10 ship))))
    (it "repairs life support damage"
      (let [ship (assoc @ship :life-support-damage 50)]
        (with-redefs [repair-capacity (fn [_ _] 10)]
          (should= 40 (:life-support-damage (repair-ship 1 ship))))))
    (it "does not over-repair life support"
      (let [ship (assoc @ship :life-support-damage 50)]
        (with-redefs [repair-capacity (fn [_ _] 60)]
          (should= 0 (:life-support-damage (repair-ship 1 ship))))))
    (it "repairs hull after life support"
      (let [ship (assoc @ship :life-support-damage 50 :hull-damage 50)]
        (with-redefs [repair-capacity (fn [_ _] 60)]
          (let [repaired (repair-ship 1 ship)]
            (should= 0 (:life-support-damage repaired))
            (should= 40 (:hull-damage repaired))))))
    (it "repairs all systems in order"
      (let [ship (assoc @ship :life-support-damage 10
                              :hull-damage 10
                              :warp-damage 10
                              :weapons-damage 10
                              :impulse-damage 10
                              :sensor-damage 10)]
        (with-redefs [repair-capacity (fn [_ _] 55)]
          (let [repaired (repair-ship 1 ship)]
            (should= 0 (:life-support-damage repaired))
            (should= 0 (:hull-damage repaired))
            (should= 0 (:warp-damage repaired))
            (should= 0 (:weapons-damage repaired))
            (should= 0 (:sensor-damage repaired))
            (should= 5 (:impulse-damage repaired)))))))

  (describe "ship destruction"
    (let [ship (mom/make-ship)]
      (it "destroys ship with 100% life support damage"
        (let [ship (assoc ship :life-support-damage 100)]
          (should (:destroyed (update-destruction ship)))))
      (it "destroys ship with 100% hull damage"
        (let [ship (assoc ship :hull-damage 100)]
          (should (:destroyed (update-destruction ship)))))
      (it "does not destroy ship with less than 100% damage"
        (let [ship (assoc ship :life-support-damage 99 :hull-damage 99)]
          (should-not (:destroyed (update-destruction ship)))))))

  (describe "dilithium"
    (with-stubs)
    (let [world (mom/make-world)
          ship (:ship world)]
      (it "does not consume dilithium when not in warp"
        (stub :calc-dilithium-consumed {:return 1})
        (let [ship (assoc ship :warp 0)
              world (assoc world :ship ship)
              world (update-ship 1 world)]
          (should= ship-dilithium (:dilithium (:ship world)))))
      (it "consumes dilithium when in warp"
        (stub :calc-dilithium-consumed {:return 1})
        (let [ship (assoc ship :warp 1)
              world (assoc world :ship ship)
              world (update-ship 1 world)]
          (should= (- ship-dilithium 1) (:dilithium (:ship world)) 1e-10)))
      (it "does not let dilithium go negative"
        (stub :calc-dilithium-consumed {:return 2})
        (let [ship (assoc ship :warp 1 :dilithium ship-dilithium-consumption)
              world (assoc world :ship ship)
              world (update-ship 1 world)]
          (should= 0 (:dilithium (:ship world)) 1e-10)))))

  (describe "core temperature"
    (let [ship (mom/make-ship)]
      (it "heats core with antimatter consumption"
        (let [hot-ship (heat-core 100 ship)]
          (should= (* 100 antimatter-to-heat) (:core-temp hot-ship))))
      (it "dissipates heat with full dilithium"
        (let [hot-ship (assoc ship :core-temp 50)
              cool-ship (dissipate-core-heat 1 hot-ship)]
          (should= (* 50 (- 1 dilithium-heat-dissipation)) (:core-temp cool-ship))))
      (it "dissipates heat with no dilithium"
        (let [hot-ship (assoc ship :core-temp 50 :dilithium 0)
              cool-ship (dissipate-core-heat 1 hot-ship)]
          (should= (- 50 glc/passive-heat-dissipation) (:core-temp cool-ship) 0.0001)))
      (it "dissipates heat with half dilithium"
        (let [hot-ship (assoc ship :core-temp 50 :dilithium (/ ship-dilithium 2))
              cool-ship (dissipate-core-heat 1 hot-ship)]
          (should= (* 50 (- 1 (* dilithium-heat-dissipation (/ (Math/sqrt 2) 2))))
                   (:core-temp cool-ship) 1e-10)))))

  (describe "base deployment"
    (it "fails if ship not near star"
      (let [world (mom/make-world)
            ship (:ship world)
            star (mom/make-star (inc ship-deploy-distance) 0 :o)
            world (assoc world :stars [star])
            world (deploy-base :antimatter-factory world)]
        (should= 0 (count (:bases world)))
        (should= ship (:ship world))
        (should= 1 (count (:messages world)))))
    (it "fails if insufficient antimatter"
      (let [world (mom/make-world)
            ship (:ship world)
            ship (assoc ship :antimatter (dec base-deployment-antimatter))
            star (mom/make-star (dec ship-deploy-distance) 0 :o)
            world (assoc world :ship ship :stars [star])
            world (deploy-base :antimatter-factory world)]
        (should= 0 (count (:bases world)))
        (should= ship (:ship world))
        (should= 1 (count (:messages world)))))
    (it "fails if insufficient dilithium"
      (let [world (mom/make-world)
            ship (:ship world)
            ship (assoc ship :dilithium (dec base-deployment-dilithium))
            star (mom/make-star (dec ship-deploy-distance) 0 :o)
            world (assoc world :ship ship :stars [star])
            world (deploy-base :antimatter-factory world)]
        (should= 0 (count (:bases world)))
        (should= ship (:ship world))
        (should= 1 (count (:messages world)))))
    (it "fails if base already exists at star"
      (let [world (mom/make-world)
            ship (:ship world)
            star (mom/make-star (dec ship-deploy-distance) 0 :o)
            base (mom/make-base (:x star) (+ (:y star) (dec ship-deploy-distance)) :antimatter-factory 0 0)
            world (assoc world :stars [star] :bases [base])
            world (deploy-base :antimatter-factory world)]
        (should= 1 (count (:bases world)))
        (should= ship (:ship world))
        (should= 1 (count (:messages world)))))
    (it "succeeds if ship near star with sufficient resources"
      (let [world (mom/make-world)
            star (mom/make-star (dec ship-deploy-distance) 0 :o)
            world (assoc world :stars [star])
            world (deploy-base :antimatter-factory world)
            ship (:ship world)]
        (should= 1 (count (:bases world)))
        (should= (bases/make-base [0 0] :antimatter-factory) (first (:bases world)))
        (should= (- ship-antimatter base-deployment-antimatter) (:antimatter ship))
        (should= (- ship-dilithium base-deployment-dilithium) (:dilithium ship))))))