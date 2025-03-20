(ns spacewar.game-logic.shots-spec
  (:require
    [spacewar.game-logic.config :refer [kinetic-damage
                                        kinetic-proximity
                                        kinetic-velocity
                                        klingon-kinetic-damage
                                        klingon-kinetic-proximity
                                        klingon-phaser-damage
                                        klingon-phaser-proximity
                                        klingon-torpedo-damage
                                        klingon-torpedo-proximity
                                        phaser-power
                                        phaser-proximity
                                        phaser-range
                                        phaser-velocity
                                        ship-antimatter
                                        ship-kinetics
                                        ship-shields
                                        ship-torpedos
                                        torpedo-damage
                                        torpedo-proximity
                                        torpedo-range
                                        torpedo-velocity]]
    [spacewar.game-logic.shots :refer [->shot
                                       calc-damage
                                       corrupt-shots-by-damage
                                       fire-weapon
                                       incur-damage
                                       update-hits
                                       update-ship-hits
                                       update-shot-positions
                                       warp-bearing-deviation
                                       warp-corruption
                                       weapon-bearing-deviation
                                       weapon-failure-dice
                                       weapon-fire-handler]]
    [spacewar.game-logic.spec-mother :as mom]
    [spacewar.spec-utils :as ut]
    [speclj.core :refer [context describe it should should=]]))

(describe "shot movement"
  (context "update-phaser-shot"
    (for [[ms x y bearing sx sy]
          [[100 0 0 0 (* phaser-velocity 100) 0]
           [100 0 0 90 0 (* phaser-velocity 100)]]]
      (it (str "moves phaser shot with ms=" ms " x=" x " y=" y " bearing=" bearing)
        (let [shot {:x x :y y :bearing bearing :range 0 :type :phaser}
              world (assoc (mom/make-world) :shots [shot])
              world (update-shot-positions ms world)
              shot (first (:shots world))]
          (prn 'shots (:shots world))
          (should (ut/roughly= sx (:x shot) 1e-10))
          (should (ut/roughly= sy (:y shot) 1e-10))
          (should (ut/roughly= bearing (:bearing shot) 1e-10))
          (should (ut/roughly= (* ms phaser-velocity) (:range shot) 1e-10))))))

  (context "update-kinetic-shot"
    (for [[ms x y bearing sx sy]
          [[1000 0 0 0 (* kinetic-velocity 1000) 0]
           [1000 0 0 90 0 (* kinetic-velocity 1000)]]]
      (it (str "moves kinetic shot with ms=" ms " x=" x " y=" y " bearing=" bearing)
        (let [shot {:x x :y y :bearing bearing :range 0 :type :kinetic}
              world (assoc (mom/make-world) :shots [shot])
              world (update-shot-positions ms world)
              shot (first (:shots world))]
          (should (ut/roughly= sx (:x shot) 1e-10))
          (should (ut/roughly= sy (:y shot) 1e-10))
          (should (ut/roughly= bearing (:bearing shot) 1e-10))
          (should (ut/roughly= (* ms kinetic-velocity) (:range shot) 1e-10))))))

  (it "removes phaser shots out of range"
    (let [ms-out-of-range (+ 1 (/ phaser-range phaser-velocity))
          world (assoc (mom/make-world) :shots [{:x 0 :y 0 :bearing 0 :range 0 :type :phaser}])
          new-world (update-shot-positions ms-out-of-range world)]
      (should= 0 (count (:shots new-world)))))

  (it "keeps phaser shots in range"
    (let [world (assoc (mom/make-world) :shots [{:x 0 :y 0 :bearing 0 :range 0 :type :phaser}])
          new-world (update-shot-positions 0 world)]
      (should= 1 (count (:shots new-world)))))

  (context "update-torpedo-shot"
    (for [[ms x y bearing range sx sy]
          [[1000 0 0 0 0 (* torpedo-velocity 1000) 0]
           [1000 0 0 90 0 0 (* torpedo-velocity 1000)]
           [1000 0 0 0 (/ torpedo-range 2) (* torpedo-velocity 1000) 0]]]
      (it (str "moves torpedo shot with ms=" ms " x=" x " y=" y " bearing=" bearing " range=" range)
        (let [shot {:x x :y y :bearing bearing :range range :type :torpedo}
              world (assoc (mom/make-world) :shots [shot])
              world (update-shot-positions ms world)
              shot (first (:shots world))]
          (should (ut/roughly= sx (:x shot) 1e-10))
          (should (ut/roughly= sy (:y shot) 1e-10))
          (should (ut/roughly= bearing (:bearing shot) 1e-10))
          (should (ut/roughly= (+ range (* ms torpedo-velocity)) (:range shot) 1e-10)))))))

(describe "fire-weapon"
  (for [[pos bearing number spread corbomite expected]
        [[[0 0] 0 1 0 false [{:x 0 :y 0 :bearing 0 :range 0 :corbomite false}]]
         [[1 1] 90 1 0 false [{:x 1 :y 1 :bearing 90 :range 0 :corbomite false}]]
         [[0 0] 90 2 10 false [{:x 0 :y 0 :bearing 85 :range 0 :corbomite false}
                               {:x 0 :y 0 :bearing 95 :range 0 :corbomite false}]]
         [[0 0] 0 2 10 false [{:x 0 :y 0 :bearing 355 :range 0 :corbomite false}
                              {:x 0 :y 0 :bearing 5 :range 0 :corbomite false}]]]]
    (it (str "fires weapon from " pos " with bearing " bearing)
      (should= expected (fire-weapon pos bearing number spread corbomite)))))

(describe "shots hitting enemies"
  (for [[enemy proximity weapon damage]
        [[:klingons phaser-proximity :phaser nil]
         [:klingons torpedo-proximity :torpedo torpedo-damage]
         [:klingons kinetic-proximity :kinetic kinetic-damage]
         [:romulans phaser-proximity :phaser nil]
         [:romulans torpedo-proximity :torpedo torpedo-damage]
         [:romulans kinetic-proximity :kinetic kinetic-damage]]]
    (context (str enemy " with " weapon)
      (it "handles no shots"
        (let [world {:shots [] enemy [{:x 0 :y 0}]}
              world (update-hits enemy world)]
          (should= [] (:shots world))
          (should= [{:x 0 :y 0}] (enemy world))))

      (it "keeps shot out of range"
        (let [world {:shots [{:x (inc proximity) :y 0 :bearing 0 :range 100 :type weapon}]
                     enemy [{:x 0 :y 0}]}
              world (update-hits enemy world)]
          (should= [{:x (inc proximity) :y 0 :bearing 0 :range 100 :type weapon}]
                   (:shots world))
          (should= [{:x 0 :y 0}] (enemy world))))

      (it "handles one shot hitting enemy"
        (let [world {:shots [{:x (dec proximity) :y 0 :bearing 0 :range 100 :type weapon}]
                     enemy [{:x 0 :y 0}]
                     :explosions [:before]}
              world (update-hits enemy world)]
          (should= [] (:shots world))
          (should= [{:x 0 :y 0 :hit {:weapon weapon :damage (if (= weapon :phaser) [100] damage)}}]
                   (enemy world))
          (should= :before (first (:explosions world)))
          (should= {:x (dec proximity) :y 0 :type weapon :age 0}
                   (dissoc (second (:explosions world)) :fragments))))

      (it "handles two shots with one hitting"
        (let [world {:shots [{:x (inc proximity) :y 0 :bearing 0 :range 100 :type weapon}
                             {:x (dec proximity) :y 0 :bearing 0 :range 100 :type weapon}]
                     enemy [{:x 0 :y 0}]
                     :explosions [:before]}
              world (update-hits enemy world)]
          (should= [{:x (inc proximity) :y 0 :bearing 0 :range 100 :type weapon}]
                   (:shots world))
          (should= [{:x 0 :y 0 :hit {:weapon weapon :damage (if (= weapon :phaser) [100] damage)}}]
                   (enemy world))
          (should= :before (first (:explosions world)))
          (should= {:x (dec proximity) :y 0 :type weapon :age 0}
                   (dissoc (second (:explosions world)) :fragments))))

      (it "handles two shots both hitting"
        (let [world {:shots [{:x (dec proximity) :y 0 :bearing 0 :range 100 :type weapon}
                             {:x (dec proximity) :y 0 :bearing 1 :range 100 :type weapon}]
                     enemy [{:x 0 :y 0}]
                     :explosions [:before]}
              world (update-hits enemy world)]
          (should= [] (:shots world))
          (should= [{:x 0 :y 0 :hit {:weapon weapon :damage (if (= weapon :phaser) [100 100] (* 2 damage))}}]
                   (enemy world))
          (should= :before (nth (:explosions world) 0))
          (should= {:x (dec proximity) :y 0 :type weapon :age 0}
                   (dissoc (nth (:explosions world) 1) :fragments))
          (should= {:x (dec proximity) :y 0 :type weapon :age 0}
                   (dissoc (nth (:explosions world) 2) :fragments)))))))

(describe "klingon shots"
  (for [[proximity weapon damage]
        [[klingon-kinetic-proximity :klingon-kinetic klingon-kinetic-damage]
         [klingon-torpedo-proximity :klingon-torpedo klingon-torpedo-damage]
         [klingon-phaser-proximity :klingon-phaser klingon-phaser-damage]]]
    (context (str "with " weapon)
      (it "misses ship"
        (let [ship (mom/make-ship)
              shot (->shot 0 (inc proximity) 180 weapon)
              world (assoc (mom/make-world) :shots [shot] :ship ship)
              new-world (update-ship-hits world)]
          (should= [] (:explosions new-world))
          (should= ship-shields (:shields (:ship new-world)))
          (should= 1 (count (:shots new-world)))))

      (it "hits ship"
        (let [ship (mom/make-ship)
              shot (->shot 0 (dec proximity) 180 weapon)
              world (assoc (mom/make-world) :shots [shot] :ship ship)
              new-world (update-ship-hits world)]
          (should= 1 (count (:explosions new-world)))
          (should= (- ship-shields damage) (:shields (:ship new-world)))
          (should= 0 (count (:shots new-world)))))

      (it "doesn't hit klingon"
        (let [world {:shots [{:x (dec proximity) :y 0 :bearing 0 :range 100 :type weapon}]
                     :klingons [{:x 0 :y 0}]
                     :explosions []}
              world (update-hits :klingons world)]
          (should= 1 (count (:shots world)))
          (should= [{:x 0 :y 0}] (:klingons world))
          (should= [] (:explosions world)))))))

(describe "romulan-blast"
  (it "hasn't hit ship yet"
    (let [world (mom/make-world)
          ship (assoc (:ship world) :shields ship-shields :x 10000)
          blast (assoc (mom/make-shot) :type :romulan-blast :bearing 0 :x 5000 :y 0 :range 5000)
          world (assoc world :shots [blast] :ship ship)
          world (update-ship-hits world)]
      (should= 1 (count (:shots world)))
      (should= ship-shields (:shields (:ship world)))))

  (it "hits ship"
    (let [world (mom/make-world)
          ship (assoc (:ship world) :shields ship-shields :y 10000)
          blast (assoc (mom/make-shot) :type :romulan-blast :bearing 0 :x 11000 :y 0 :range 11000)
          world (assoc world :shots [blast] :ship ship)
          world (update-ship-hits world)]
      (should= 0 (count (:shots world)))
      (should= 1 (count (:explosions world)))
      (should (> ship-shields (:shields (:ship world)))))))

(describe "additional tests"
  (it "constructs valid shot"
    (should= true (mom/valid-shot? (->shot 0 0 180 :kinetic))))

  (it "weapons use power"
    (let [world (assoc (mom/make-world) :ship (assoc (mom/make-ship) :selected-weapon :phaser
                                                                     :weapon-number-setting 2))
          new-world (weapon-fire-handler {} world)]
      (should= 2 (count (:shots new-world)))
      (should= (- ship-antimatter (* 2 phaser-power)) (:antimatter (:ship new-world)))))

  (it "weapons fail if not enough antimatter"
    (let [world (assoc (mom/make-world) :ship (assoc (mom/make-ship) :selected-weapon :phaser
                                                                     :weapon-number-setting 2
                                                                     :antimatter 1))
          new-world (weapon-fire-handler {} world)]
      (should= 0 (count (:shots new-world)))
      (should= 1 (:antimatter (:ship new-world)))))

  (it "decrements kinetics inventory on successful firing"
    (let [world (assoc (mom/make-world) :ship (assoc (mom/make-ship) :selected-weapon :kinetic
                                                                     :weapon-number-setting 2))
          new-world (weapon-fire-handler {} world)]
      (should= (- ship-kinetics 2) (:kinetics (:ship new-world)))))

  (it "decrements torpedos inventory on successful firing"
    (let [world (assoc (mom/make-world) :ship (assoc (mom/make-ship) :selected-weapon :torpedo
                                                                     :weapon-number-setting 2))
          new-world (weapon-fire-handler {} world)]
      (should= (- ship-torpedos 2) (:torpedos (:ship new-world)))))

  (it "can't fire kinetic if not in inventory"
    (let [world (assoc (mom/make-world) :ship (assoc (mom/make-ship) :selected-weapon :kinetic
                                                                     :weapon-number-setting 2
                                                                     :kinetics 1))
          new-world (weapon-fire-handler {} world)]
      (should= 0 (count (:shots new-world)))
      (should= 1 (:kinetics (:ship new-world)))))

  (it "can't fire torpedo if not in inventory"
    (let [world (assoc (mom/make-world) :ship (assoc (mom/make-ship) :selected-weapon :torpedo
                                                                     :weapon-number-setting 2
                                                                     :torpedos 1))
          new-world (weapon-fire-handler {} world)]
      (should= 0 (count (:shots new-world)))
      (should= 1 (:torpedos (:ship new-world)))))

  (context "calculate real damage to ship"
    (for [[shields hit-strength real-damage]
          [[ship-shields 100 0]
           [(/ ship-shields 2) 100 55]
           [(/ ship-shields 4) 100 80]
           [0 100 100]]]
      (it (str "with shields=" shields " hit-strength=" hit-strength)
        (should= real-damage (calc-damage shields hit-strength)))))

  (context "damage can be incurred"
    (for [[damage system state expected]
          [[100 :not-a-system {:system 5} {:system 5}]
           [100 :system {:system 0} {:system 100}]
           [50 :system {:system 20} {:system 70}]
           [50 :system {:system 80} {:system 100}]]]
      (it (str "with damage=" damage " system=" system)
        (should= expected (incur-damage damage system state)))))

  (context "Shot corruption"
    (let [shot1 (assoc (mom/make-shot) :bearing 90)
          shot2 (assoc (mom/make-shot) :bearing 95)
          shot3 (assoc (mom/make-shot) :bearing 100)
          shots [shot1 shot2 shot3]]
      (it "no corruption if weapons not damaged"
        (should= shots (corrupt-shots-by-damage 0 shots)))

      (it "no corruption if dice say no"
        (with-redefs [weapon-failure-dice (fn [_ _] [false false false])
                      weapon-bearing-deviation (fn [_ _] [0 0 0])]
          (should= shots (corrupt-shots-by-damage 50 shots))))

      (it "shots removed if dice say yes"
        (with-redefs [weapon-failure-dice (fn [_ _] [false true false])
                      weapon-bearing-deviation (fn [_ _] [0 0])]
          (should= [shot1 shot3] (corrupt-shots-by-damage 50 shots))))

      (it "bearing not corrupted if bearing dice are kind"
        (with-redefs [weapon-failure-dice (fn [_ _] [false false false])
                      weapon-bearing-deviation (fn [_ _] [0 0 0])]
          (should= shots (corrupt-shots-by-damage 50 shots))))

      (it "bearing corrupted by bearing dice"
        (with-redefs [weapon-failure-dice (fn [_ _] [true true false])
                      weapon-bearing-deviation (fn [_ _] [1])]
          (should= 101 (:bearing (first (corrupt-shots-by-damage 50 shots))))))

      (it "bearing corruption normalized"
        (with-redefs [weapon-failure-dice (fn [_ _] [true true false])
                      weapon-bearing-deviation (fn [_ _] [261])]
          (should= 1 (:bearing (first (corrupt-shots-by-damage 50 shots))))))

      (it "no warp corruption if not warping"
        (should= shots (warp-corruption 0 shots)))

      (it "warp speed corrupts bearing"
        (with-redefs [warp-bearing-deviation (fn [] 10)]
          (let [shots (warp-corruption 1 shots)]
            (should= 100 (:bearing (nth shots 0)))
            (should= 105 (:bearing (nth shots 1)))
            (should= 110 (:bearing (nth shots 2)))))))))