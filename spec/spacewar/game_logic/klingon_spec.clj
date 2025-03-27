(ns spacewar.game-logic.klingon-spec
  (:require
    [speclj.core :refer :all]
    [clojure.spec.alpha :as s]
    [spacewar.game-logic.config :as glc]
    [spacewar.spec-utils :as ut]
    [spacewar.game-logic.klingons :as k]
    [spacewar.game-logic.spec-mother :as mom]
    [spacewar.game-logic.shots :as shots]
    [spacewar.game-logic.bases :as bases]
    [spacewar.geometry :as geo]
    ))

(declare klingon klingon2 ship world)

(describe "klingons"
  (with klingon (assoc (mom/make-klingon) :shields glc/klingon-shields
                                          :antimatter 100))
  (with klingon2 (assoc (mom/make-klingon) :shields 50
                                           :antimatter 50))
  (with ship (mom/make-ship))
  (with world (assoc (mom/make-world) :klingons [@klingon]
                                      :ship @ship))

  (describe "klingon basics"
    (it "makes a random klingon"
      (should (mom/valid-klingon? (k/make-random-klingon))))

    (it "initializes klingons"
      (let [klingons (k/initialize)]
        (should= nil (s/explain-data ::k/klingons klingons))
        (should= glc/number-of-klingons (count klingons)))))

  (describe "klingon updates"
    (it "handles no hit"
      (let [new-world (k/update-klingons 20 @world)
            klingon (-> new-world :klingons first)]
        (should= true (mom/valid-world? new-world))
        (should= glc/klingon-shields (:shields klingon))
        (should (nil? (:hit klingon)))
        (should= 20 (:battle-state-age klingon))
        (should= [] (:explosions new-world))))

    (it "handles simple kinetic hit"
      (let [klingon (assoc @klingon :hit {:weapon :kinetic :damage 20}
                                    :antimatter glc/klingon-antimatter
                                    :battle-state :advancing
                                    :battle-state-age 1)
            world (assoc @world :klingons [klingon])
            new-world (k/update-klingons 2 world)
            klingons (:klingons new-world)
            explosions (:explosions new-world)
            klingon (first klingons)]
        (should= 1 (count klingons))
        (should (nil? (:hit klingon)))
        (should (< 179 (:shields klingon) 181))
        (should (< glc/klingon-battle-state-transition-age (:battle-state-age klingon)))
        (should= [] explosions)))

    (it "destroys klingon when shields depleted"
      (let [world (mom/make-world)
            klingon (mom/make-klingon)
            klingon (mom/set-pos klingon [50 50])
            klingon (assoc klingon :shields 10
                                   :hit {:weapon :kinetic :damage 20})
            world (assoc world :klingons [klingon])
            world (k/update-klingons 20 world)]
        (should= [] (:klingons world))
        (should= 1 (count (:explosions world)))
        (should= 1 (:klingons-killed world))
        (should= {:age 0 :x 50 :y 50 :type :klingon}
                 (dissoc (first (:explosions world)) :fragments))))

    (it "kamikazee mode depletes shields"
          (let [world (mom/make-world)
                klingon (mom/make-klingon)
                klingon (mom/set-pos klingon [50 50])
                klingon (assoc klingon :battle-state :kamikazee :shields 10)
                world (assoc world :klingons [klingon])
                world (k/update-klingons 20 world)
                klingon (first (:klingons world))]
            (should (> 10 (:shields klingon)))
            )))

  (describe "phaser damage"
    (it "calculates phaser damage based on ranges"
      (should= 0 (k/damage-by-phasers {:damage [glc/phaser-range]}))
      (should= glc/phaser-damage (k/damage-by-phasers {:damage [0]}))
      (should= (/ glc/phaser-damage 2) (k/damage-by-phasers {:damage [(/ glc/phaser-range 2)]}))
      (should= (* 2 glc/phaser-damage) (k/damage-by-phasers {:damage [0 0]}))))

  (describe "shield recharge"
    (it "recharges shields based on antimatter"
      (should= {:antimatter 1000 :shields glc/klingon-shields}
               (k/recharge-shield 20 {:antimatter 1000 :shields glc/klingon-shields}))
      (let [am-in 1000
            shields-in (- glc/klingon-shields 50)
            am-out (- am-in (* glc/klingon-shield-recharge-rate glc/klingon-shield-recharge-cost 20))
            shields-out (+ shields-in (* glc/klingon-shield-recharge-rate 20))]
        (should= {:antimatter am-out :shields shields-out}
                 (k/recharge-shield 20 {:antimatter am-in :shields shields-in})))))

  (describe "klingon offense"
    (around [it] (with-redefs [k/delay-shooting? (fn [] false)] (it)))

    (with ship (-> (mom/make-ship) (mom/set-pos [0 0])))
    (with klingon (mom/make-klingon))

    (it "does not fire when out of kinetic range"
      (let [out-of-range [(inc glc/klingon-kinetic-firing-distance) 0]
            klingon (mom/set-pos @klingon out-of-range)
            world (-> @world (mom/set-ship @ship) (mom/set-klingons [klingon]))
            offense (k/update-klingon-offense 20 world)]
        (should= world offense)))

    (it "charges weapon when just in kinetic range"
      (let [in-range (dec glc/klingon-kinetic-firing-distance)
            klingon (mom/set-pos @klingon [in-range 0])
            world (-> @world (mom/set-ship @ship) (mom/set-klingons [klingon]))
            offense (k/update-klingon-offense 20 world)]
        (should (mom/valid-world? offense))
        (should= 20 (-> offense :klingons first :weapon-charge))))

    (it "does not fire kinetic with insufficient antimatter"
      (let [in-range (dec glc/klingon-kinetic-firing-distance)
            klingon (mom/set-pos @klingon [in-range 0])
            klingon (assoc klingon :antimatter (dec glc/klingon-kinetic-power)
                                   :weapon-charge glc/klingon-kinetic-threshold
                                   :kinetics 20)
            world (mom/set-klingons @world [klingon])
            world (assoc world :shots [(shots/->shot 0 0 0 :phaser)])
            offense (k/update-klingon-offense 0 world)]
        (should (mom/valid-world? offense))
        (should= 1 (count (:shots offense)))
        (let [klingon (-> offense :klingons first)]
          (should= glc/klingon-kinetic-threshold (:weapon-charge klingon))
          (should= 20 (:kinetics klingon))
          (should= (dec glc/klingon-kinetic-power) (:antimatter klingon)))))

    (it "fires kinetic when conditions met"
      (let [in-range (dec glc/klingon-kinetic-firing-distance)
            klingon (mom/set-pos @klingon [in-range 0])
            klingon (assoc klingon :antimatter 1000
                                   :weapon-charge glc/klingon-kinetic-threshold
                                   :kinetics 20)
            world (mom/set-klingons @world [klingon])
            world (assoc world :shots [(shots/->shot 0 0 0 :phaser)])
            offense (k/update-klingon-offense 0 world)]
        (should (mom/valid-world? offense))
        (should= 2 (count (:shots offense)))
        (let [shot (second (:shots offense))
              klingon (first (:klingons offense))]
          (should= :klingon-kinetic (:type shot))
          (should (ut/roughly= 180 (:bearing shot) 1e-8))
          (should= 0 (:weapon-charge klingon))
          (should= 19 (:kinetics klingon))
          (should= (- 1000 glc/klingon-kinetic-power) (:antimatter klingon)))))

    (it "does not fire phaser with insufficient antimatter"
      (let [in-range (dec glc/klingon-phaser-firing-distance)
            klingon (mom/set-pos @klingon [in-range 0])
            klingon (assoc klingon :antimatter (dec glc/klingon-phaser-power)
                                   :weapon-charge glc/klingon-phaser-threshold)
            world (mom/set-klingons @world [klingon])
            world (assoc world :shots [(shots/->shot 0 0 0 :phaser)])
            offense (k/update-klingon-offense 0 world)]
        (should (mom/valid-world? offense))
        (should= 1 (count (:shots offense)))
        (let [klingon (-> offense :klingons first)]
          (should= glc/klingon-phaser-threshold (:weapon-charge klingon))
          (should= (dec glc/klingon-phaser-power) (:antimatter klingon)))))

    (it "fires phaser when in range and charged"
      (let [in-range (dec glc/klingon-phaser-firing-distance)
            klingon (mom/set-pos @klingon [in-range 0])
            klingon (assoc klingon :antimatter 1000 :weapon-charge glc/klingon-phaser-threshold)
            world (mom/set-klingons @world [klingon])
            world (assoc world :shots [(shots/->shot 0 0 0 :phaser)])
            offense (k/update-klingon-offense 0 world)]
        (should (mom/valid-world? offense))
        (should= 2 (count (:shots offense)))
        (let [shot (second (:shots offense))
              klingon (first (:klingons offense))]
          (should= :klingon-phaser (:type shot))
          (should (ut/roughly= 180 (:bearing shot) 1e-8))
          (should= 0 (:weapon-charge klingon))
          (should= (- 1000 glc/klingon-phaser-power) (:antimatter klingon)))))

    (it "fires torpedo when in range, charged, and ship not turning"
      (let [in-range (dec glc/klingon-torpedo-firing-distance)
            klingon (mom/set-pos @klingon [in-range 0])
            klingon (assoc klingon :antimatter 1000 :weapon-charge glc/klingon-torpedo-threshold :torpedos 1)
            world (mom/set-klingons @world [klingon])
            world (assoc world :shots [])
            offense (k/update-klingon-offense 0 world)]
        (should (mom/valid-world? offense))
        (should= 1 (count (:shots offense)))
        (let [shot (first (:shots offense))
              klingon (first (:klingons offense))]
          (should= :klingon-torpedo (:type shot))
          (should (ut/roughly= 180 (:bearing shot) 1e-8))
          (should= 0 (:weapon-charge klingon))
          (should= (- 1000 glc/klingon-torpedo-power) (:antimatter klingon)))))

    (it "does not fire torpedo with no torpedos"
      (let [in-range (dec glc/klingon-torpedo-firing-distance)
            klingon (mom/set-pos @klingon [in-range 0])
            klingon (assoc klingon :antimatter 1000 :weapon-charge glc/klingon-torpedo-threshold :torpedos 0)
            world (mom/set-klingons @world [klingon])
            world (assoc world :shots [])
            offense (k/update-klingon-offense 0 world)]
        (should (mom/valid-world? offense))
        (should= 0 (count (:shots offense)))))

    (it "does not fire torpedo when ship is turning"
      (let [in-range (dec glc/klingon-torpedo-firing-distance)
            klingon (mom/set-pos @klingon [in-range 0])
            klingon (assoc klingon :antimatter 1000 :weapon-charge glc/klingon-torpedo-threshold :torpedos 1)
            ship (assoc @ship :heading 90 :heading-setting 0)
            world (mom/set-klingons @world [klingon])
            world (assoc world :shots [] :ship ship)
            offense (k/update-klingon-offense 0 world)]
        (should (mom/valid-world? offense))
        (should= 0 (count (:shots offense)))))

    (it "does not fire kinetic with no kinetics left"
      (let [in-range (dec glc/klingon-kinetic-firing-distance)
            klingon (mom/set-pos @klingon [in-range 0])
            klingon (assoc klingon :weapon-charge glc/klingon-kinetic-threshold :kinetics 0)
            world (mom/set-klingons @world [klingon])
            world (assoc world :shots [])
            offense (k/update-klingon-offense 0 world)]
        (should (mom/valid-world? offense))
        (should= 0 (count (:shots offense)))
        (let [klingon (-> offense :klingons first)]
          (should= glc/klingon-kinetic-threshold (:weapon-charge klingon))
          (should= 0 (:kinetics klingon)))))
    )

  (describe "klingon battle motion"
    (it "thrusts appropriately in battle states"
      (doseq [battle-state [:advancing :retreating :flank-right :flank-left]]
        (let [ship-pos [0 0]
              ship (mom/set-pos @ship ship-pos)
              klingon (mom/set-pos @klingon [(dec glc/klingon-tactical-range) 0])
              klingon (assoc klingon :antimatter glc/klingon-antimatter :battle-state battle-state)
              world (assoc @world :ship ship :klingons [klingon])
              new-world (k/update-klingon-motion 2 world)
              new-klingon (-> new-world :klingons first)
              thrust (:thrust new-klingon)
              thrust-angle (geo/angle-degrees thrust ship-pos)]
          (should (ut/roughly= (glc/klingon-evasion-trajectories battle-state) thrust-angle 1e-5))))))

  (describe "klingon super-state"
    (it "determines battle or cruise state based on range"
      (let [ship (mom/set-pos @ship [0 0])
            klingon-out (mom/set-pos @klingon [(inc glc/klingon-tactical-range) 0])
            klingon-in (mom/set-pos @klingon [(dec glc/klingon-tactical-range) 0])]
        (should= :cruise (k/super-state klingon-out ship))
        (should= :battle (k/super-state klingon-in ship))))

    (it "maintains course when out of range in cruise state"
      (let [ship (mom/set-pos @ship [0 0])
            klingon (mom/set-pos @klingon [(inc glc/klingon-tactical-range) 0])
            klingon (assoc klingon :thrust [1 1])
            world (assoc @world :ship ship :klingons [klingon])
            new-world (k/update-klingon-motion 2 world)
            new-klingon (-> new-world :klingons first)]
        (should= [1 1] (:thrust new-klingon))
        (should= :cruise (k/super-state klingon ship)))))

  (describe "klingon cruise behavior"
    (it "seeks and destroys in mission state"
      (let [ship (assoc @ship :x 1e7 :y 1e7)
            klingon (assoc @klingon :cruise-state :mission :mission :seek-and-destroy)
            world (assoc @world :ship ship :klingons [klingon])
            world (k/cruise-klingons world)
            klingon (-> world :klingons first)
            thrust (* (Math/sqrt 2) 0.5 glc/klingon-cruise-thrust)]
        (should (ut/roughly-v [thrust thrust] (:thrust klingon)))
        (should= :cruise (k/super-state klingon ship))))

    (it "guards nearest base"
      (let [ship (assoc @ship :x 1e7 :y 1e7)
            klingon (assoc @klingon :cruise-state :guard)
            base1 (bases/make-base [0 2000] :antimatter-factory)
            base2 (bases/make-base [1000 0] :antimatter-factory)
            world (assoc @world :ship ship :bases [base1 base2] :klingons [klingon])
            world (k/cruise-klingons world)
            klingon (-> world :klingons first)]
        (should (ut/roughly-v [glc/klingon-cruise-thrust 0] (:thrust klingon)))
        (should= :cruise (k/super-state klingon ship))))

    (it "blockades nearest base"
      (let [ship (assoc @ship :x 1e7 :y 1e7)
            klingon (assoc @klingon :cruise-state :mission :mission :blockade)
            base1 (bases/make-base [0 2000] :antimatter-factory)
            base2 (bases/make-base [1000 0] :antimatter-factory)
            world (assoc @world :ship ship :bases [base1 base2] :klingons [klingon])
            world (k/cruise-klingons world)
            klingon (-> world :klingons first)]
        (should (ut/roughly-v [glc/klingon-cruise-thrust 0] (:thrust klingon)))
        (should= :cruise (k/super-state klingon ship))))

    (it "refuels at nearest antimatter star"
      (let [ship (assoc @ship :x 1e7 :y 1e7)
            star1 (mom/make-star 100 100 :o)
            star2 (mom/make-star 20 0 :g)
            star3 (mom/make-star 0 200 :o)
            klingon-hi (assoc @klingon :cruise-state :refuel :antimatter glc/klingon-antimatter :id :hi)
            klingon-low (assoc @klingon :cruise-state :refuel :antimatter (/ glc/klingon-antimatter 10) :id :low)
            world (assoc @world :stars [star1 star2 star3] :klingons [klingon-hi klingon-low] :ship ship)
            world (k/cruise-klingons world)
            klingons (group-by :id (:klingons world))
            klingon-hi (first (:hi klingons))
            klingon-low (first (:low klingons))
            thrust (* (Math/sqrt 2) 0.5 glc/klingon-cruise-thrust)]
        (should (ut/roughly-v [thrust thrust] (:thrust klingon-hi)))
        (should (ut/roughly-v [glc/klingon-cruise-thrust 0] (:thrust klingon-low)))
        (should= :cruise (k/super-state klingon-hi ship))
        (should= :cruise (k/super-state klingon-low ship))))

    (it "refuels at nearest antimatter base if close"
      (let [ship (assoc @ship :x 1e7 :y 1e7)
            star1 (mom/make-star 100 100 :o)
            star2 (mom/make-star 20 0 :g)
            star3 (mom/make-star 0 200 :o)
            base (mom/make-base 150 0 :antimatter-factory 0 0)
            klingon (assoc @klingon :cruise-state :refuel)
            world (assoc @world :stars [star1 star2 star3] :klingons [klingon] :ship ship :bases [base])
            world (k/cruise-klingons world)
            klingon (-> world :klingons first)]
        (should (ut/roughly-v [glc/klingon-cruise-thrust 0] (:thrust klingon)))
        (should= :cruise (k/super-state klingon ship))))

    (it "patrols in random direction"
      (let [ship (assoc @ship :x 1e7 :y 1e7)
            klingon (assoc @klingon :cruise-state :patrol)
            world (assoc @world :klingons [klingon] :ship ship)
            world (k/change-patrol-direction world)
            klingon (-> world :klingons first)
            [tx ty] (:thrust klingon)
            abs-thrust (Math/sqrt (+ (* tx tx) (* ty ty)))]
        (should (ut/roughly= glc/klingon-cruise-thrust abs-thrust 1e-8))
        (should= :cruise (k/super-state klingon ship)))))

  (describe "cruise state transitions"
    (it "transitions based on antimatter and torpedos"
      (should= :low-antimatter (k/cruise-transition {:antimatter 0 :torpedos 0}))
      (should= :low-antimatter (k/cruise-transition {:antimatter (* 0.4 glc/klingon-antimatter) :torpedos 0}))
      (should= :low-torpedo (k/cruise-transition {:antimatter (* 0.41 glc/klingon-antimatter) :torpedos 0}))
      (should= :low-torpedo (k/cruise-transition {:antimatter (* 0.41 glc/klingon-antimatter) :torpedos (* 0.40 glc/klingon-torpedos)}))
      (should= :capable (k/cruise-transition {:antimatter (* 0.41 glc/klingon-antimatter) :torpedos (* 0.41 glc/klingon-torpedos)}))
      (should= :well-supplied (k/cruise-transition {:antimatter (* 0.41 glc/klingon-antimatter) :torpedos (* 0.61 glc/klingon-torpedos)}))))

  (describe "weapon production"
    (it "produces kinetics"
      (let [klingon (assoc @klingon :antimatter glc/klingon-antimatter :kinetics 0)
            klingon (k/update-torpedo-and-kinetic-production 2 klingon)]
        (should (ut/roughly= (* 2 glc/klingon-kinetic-production-rate) (:kinetics klingon) 1e-8))))

    (it "does not produce kinetics when full"
      (let [klingon (assoc @klingon :antimatter glc/klingon-antimatter :kinetics glc/klingon-kinetics)
            klingon (k/update-torpedo-and-kinetic-production 2 klingon)]
        (should= glc/klingon-kinetics (:kinetics klingon))))

    (it "produces torpedos with sufficient antimatter"
      (let [klingon (assoc @klingon :antimatter glc/klingon-antimatter :torpedos 0)
            klingon (k/update-torpedo-and-kinetic-production 2 klingon)]
        (should (ut/roughly= (* 2 glc/klingon-torpedo-production-rate) (:torpedos klingon) 1e-8))
        (should (ut/roughly= (- glc/klingon-antimatter (* 2 glc/klingon-torpedo-antimatter-cost)) (:antimatter klingon) 1e-8))))

    (it "does not produce torpedos when full"
      (let [klingon (assoc @klingon :antimatter glc/klingon-antimatter :torpedos glc/klingon-torpedos)
            klingon (k/update-torpedo-and-kinetic-production 2 klingon)]
        (should= glc/klingon-antimatter (:antimatter klingon))
        (should= glc/klingon-torpedos (:torpedos klingon))))

    (it "does not produce with insufficient antimatter"
      (let [klingon (assoc @klingon :antimatter (dec glc/klingon-torpedo-antimatter-threshold) :torpedos 0)
            klingon (k/update-torpedo-and-kinetic-production 2 klingon)]
        (should= (dec glc/klingon-torpedo-antimatter-threshold) (:antimatter klingon))
        (should= 0 (:torpedos klingon)))))

  (describe "antimatter collection"
    (it "refuels near stars and stops"
      (let [klingon (assoc @klingon :antimatter 0 :x 0 :y 0 :thrust [1 1] :velocity [1 1] :cruise-state :refuel)
            star (mom/make-star 0 (dec glc/klingon-range-for-antimatter-production) :o)
            klingon (k/produce-antimatter 2 klingon [star])]
        (should (ut/roughly= (* 2 (glc/klingon-antimatter-production-rate :o)) (:antimatter klingon) 1e-8))
        (should= [0 0] (:thrust klingon))
        (should= [0 0] (:velocity klingon))))

    (it "stops but does not exceed antimatter limit near stars"
      (let [klingon (assoc @klingon :antimatter glc/klingon-antimatter :x 0 :y 0 :thrust [1 1] :velocity [1 1] :cruise-state :refuel)
            star (mom/make-star 0 (dec glc/klingon-range-for-antimatter-production) :o)
            klingon (k/produce-antimatter 2 klingon [star])]
        (should= glc/klingon-antimatter (:antimatter klingon))
        (should= [0 0] (:thrust klingon))
        (should= [0 0] (:velocity klingon))))

    (it "collects antimatter but does not stop if not refueling"
      (let [klingon (assoc @klingon :antimatter 0 :x 0 :y 0 :thrust [1 1] :velocity [1 1] :cruise-state :patrol)
            star (mom/make-star 0 (dec glc/klingon-range-for-antimatter-production) :o)
            klingon (k/produce-antimatter 2 klingon [star])]
        (should (ut/roughly= (* 2 (glc/klingon-antimatter-production-rate :o)) (:antimatter klingon) 1e-8))
        (should= [1 1] (:thrust klingon))
        (should= [1 1] (:velocity klingon))))
    )

  (describe "klingon motion"
    (around [it] (with-redefs [k/calc-drag (constantly 1)]) it)
    (it "increases velocity with tactical thrust"
      (let [klingon (mom/set-pos @klingon [(dec glc/klingon-tactical-range) 0])
            klingon (assoc klingon :antimatter glc/klingon-antimatter)
            world (assoc @world :klingons [klingon])
            new-world (k/update-klingon-motion 2 world)
            velocity (-> new-world :klingons first :velocity)]
        (should (ut/roughly= (* -2 glc/klingon-tactical-thrust) (first velocity) 1e-8))
        (should (ut/roughly= 0 (second velocity) 1e-10))))

    (it "moves with velocity"
      (let [klingon (assoc @klingon :velocity [1 1])
            klingon (mom/set-pos klingon [1000 1000])
            world (assoc @world :klingons [klingon])
            new-world (k/update-klingon-motion 2 world)
            x (-> new-world :klingons first :x)
            y (-> new-world :klingons first :y)]
        (should (ut/roughly= 1002 x 1e-8))
        (should (ut/roughly= 1002 y 1e-8)))))

  (describe "battle state transitions"
    (around [it] (with-redefs [k/random-battle-state (constantly :flank-right)]) it)
    (it "transitions battle state based on distance, age, and antimatter"
      (let [expired-age (inc glc/klingon-battle-state-transition-age)
            run-away (dec glc/klingon-antimatter-runaway-threshold)]
        (doseq [[am dist start-state age end-state new-age]
                [[glc/klingon-antimatter (inc glc/klingon-tactical-range) :advancing 0 :no-battle 10]
                 [glc/klingon-antimatter (dec glc/klingon-tactical-range) :no-battle 0 :advancing 10]
                 [glc/klingon-antimatter (/ glc/klingon-evasion-limit 2) :no-battle expired-age :flank-right 0]
                 [glc/klingon-antimatter (dec glc/klingon-evasion-limit) :no-battle 0 :no-battle 10]
                 [run-away (dec glc/klingon-tactical-range) :advancing 0 :retreating 10]]]
          (let [ship (assoc @ship :x dist)
                klingon (assoc @klingon :battle-state start-state :battle-state-age age :antimatter am)
                world (assoc @world :ship ship :klingons [klingon])
                world (k/update-klingons-state 10 world)
                klingon (-> world :klingons first)]
            (should= end-state (:battle-state klingon))
            (should= new-age (:battle-state-age klingon)))))))

  (describe "klingon antimatter theft"
    (it "steals from base and stops if in guard state"
      (let [base (mom/make-base (:x @klingon) (+ (:y @klingon) (dec glc/ship-docking-distance)) :antimatter-factory 100 100)
            klingon (assoc @klingon :antimatter 0 :thrust [1 1] :velocity [1 1] :cruise-state :guard)
            world (assoc @world :klingons [klingon] :bases [base])
            world (k/klingons-steal-antimatter world)
            base (-> world :bases first)
            klingon (-> world :klingons first)]
        (should= 0 (:antimatter base))
        (should= 100 (:antimatter klingon))
        (should= [0 0] (:velocity klingon))
        (should= [0 0] (:thrust klingon))))

    (it "steals from base and stops if in refuel state"
      (let [base (mom/make-base (:x @klingon) (+ (:y @klingon) (dec glc/ship-docking-distance)) :antimatter-factory 100 100)
            klingon (assoc @klingon :antimatter 0 :thrust [1 1] :velocity [1 1] :cruise-state :refuel)
            world (assoc @world :klingons [klingon] :bases [base])
            world (k/klingons-steal-antimatter world)
            base (-> world :bases first)
            klingon (-> world :klingons first)]
        (should= 0 (:antimatter base))
        (should= 100 (:antimatter klingon))
        (should= [0 0] (:velocity klingon))
        (should= [0 0] (:thrust klingon))))

    (it "steals from base and stops if blockading"
      (let [base (mom/make-base (:x @klingon) (+ (:y @klingon) (dec glc/ship-docking-distance)) :antimatter-factory 100 100)
            klingon (assoc @klingon :antimatter 0 :thrust [1 1] :velocity [1 1] :cruise-state :mission :mission :blockade)
            world (assoc @world :klingons [klingon] :bases [base])
            world (k/klingons-steal-antimatter world)
            base (-> world :bases first)
            klingon (-> world :klingons first)]
        (should= 0 (:antimatter base))
        (should= 100 (:antimatter klingon))
        (should= [0 0] (:velocity klingon))
        (should= [0 0] (:thrust klingon))))

    (it "steals from base but continues if seek-and-destroy"
      (let [base (mom/make-base (:x @klingon) (+ (:y @klingon) (dec glc/ship-docking-distance)) :antimatter-factory 100 100)
            klingon (assoc @klingon :antimatter 0 :thrust [1 1] :velocity [1 1] :cruise-state :mission :mission :seek-and-destroy)
            world (assoc @world :klingons [klingon] :bases [base])
            world (k/klingons-steal-antimatter world)
            base (-> world :bases first)
            klingon (-> world :klingons first)]
        (should= 0 (:antimatter base))
        (should= 100 (:antimatter klingon))
        (should= [1 1] (:velocity klingon))
        (should= [1 1] (:thrust klingon))))

    (it "allows two klingons to steal from one base"
      (let [base (mom/make-base (:x @klingon) (+ (:y @klingon) (/ glc/ship-docking-distance 2)) :antimatter-factory 10000000 100)
            klingon (assoc @klingon :antimatter (- glc/klingon-antimatter 10000) :id 1)
            klingon2 (assoc @klingon2 :antimatter 0 :id 2 :x (inc (:x klingon)))
            world (assoc @world :klingons [klingon klingon2] :bases [base])
            world (k/klingons-steal-antimatter world)
            base (-> world :bases first)
            klingon (first (:klingons world))
            klingon2 (second (:klingons world))]
        (should= 1 (count (:bases world)))
        (should= (- 10000000 glc/klingon-antimatter 10000) (:antimatter base))
        (should= glc/klingon-antimatter (:antimatter klingon))
        (should= glc/klingon-antimatter (:antimatter klingon2)))))

  (describe "klingon update orchestration"
    (with-stubs)
    (it "calls all necessary update functions"
      (with-redefs [k/update-klingon-defense (stub :defense {:return @world})
                    k/update-klingon-offense (stub :offense {:return @world})
                    k/update-klingon-motion (stub :motion {:return @world})
                    k/update-klingons-state (stub :state {:return @world})
                    k/update-klingon-torpedo-production (stub :torpedo-production {:return @world})]
        (let [ms 10]
          (should= @world (k/update-klingons ms @world))
          (should-have-invoked :defense {:times 1 :with [ms @world]})
          (should-have-invoked :offense {:times 1 :with [ms @world]})
          (should-have-invoked :motion {:times 1 :with [ms @world]})
          (should-have-invoked :state {:times 1 :with [ms @world]})
          (should-have-invoked :torpedo-production {:times 1 :with [ms @world]})))))
  )