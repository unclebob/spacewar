(ns spacewar.game-logic.bases-spec
  (:require
    [spacewar.game-logic.bases :as bases]
    [spacewar.game-logic.config :as glc]
    [spacewar.game-logic.ship :as ship]
    [spacewar.game-logic.spec-mother :as mom]
    [spacewar.spec-utils :as ut]
    [speclj.core :refer :all]))

(describe "base aging"
  (it "ages bases by the specified time"
    (let [bases [(bases/make-random-base) (bases/make-random-base)]
          aged-bases (bases/age-bases 10 bases)]
      (should= 10 (:age (first aged-bases)))
      (should= 10 (:age (second aged-bases)))))

  (it "stops aging bases once they reach maturity"
    (let [base (bases/make-random-base)
          base (assoc base :age glc/base-maturity-age)
          bases [base]
          aged-bases (bases/age-bases 10 bases)]
      (should= glc/base-maturity-age (:age (first aged-bases)))))
  )

(describe "bases manufacturing"
  (it "does not manufacture resources for immature bases"
    (let [am-base (mom/make-base)
          dl-base (mom/make-base)
          wpn-base (mom/make-base)
          am-base (assoc am-base :x 1
                                 :age (dec glc/base-maturity-age)
                                 :type :antimatter-factory)
          dl-base (assoc dl-base :x 2
                                 :age (dec glc/base-maturity-age)
                                 :type :dilithium-factory)
          wpn-base (assoc wpn-base :x 3
                                   :age (dec glc/base-maturity-age)
                                   :type :weapon-factory)
          bases [am-base dl-base wpn-base]
          [am-base dl-base wpn-base] (bases/update-bases-manufacturing 10 bases)]
      (should= 0 (:antimatter am-base))
      (should= 0 (:dilithium dl-base))
      (should= 0 (:torpedos wpn-base))
      (should= 0 (:kinetics wpn-base))))

  (it "manufactures resources correctly for mature bases"
    (let [am-base (mom/make-base)
          dl-base (mom/make-base)
          wpn-base (mom/make-base)
          am-base (assoc am-base :x 1
                                 :age (inc glc/base-maturity-age)
                                 :type :antimatter-factory)
          dl-base (assoc dl-base :x 2
                                 :age (inc glc/base-maturity-age)
                                 :type :dilithium-factory
                                 :antimatter glc/base-antimatter-maximum)
          wpn-base (assoc wpn-base :x 3
                                   :age (inc glc/base-maturity-age)
                                   :type :weapon-factory
                                   :antimatter glc/base-antimatter-maximum
                                   :dilithium glc/base-dilithium-maximum)
          bases [am-base dl-base wpn-base]
          ms 100000
          [am-base dl-base wpn-base] (bases/update-bases-manufacturing ms bases)]
      (should= (* ms glc/antimatter-factory-production-rate) (:antimatter am-base))
      (should= (* ms glc/dilithium-factory-production-rate) (:dilithium dl-base))
      (should= (* ms glc/weapon-factory-torpedo-production-rate) (:torpedos wpn-base))
      (should= (* ms glc/weapon-factory-kinetic-production-rate) (:kinetics wpn-base))
      (should= (- glc/base-antimatter-maximum
                  (* (:dilithium dl-base) glc/dilithium-factory-dilithium-antimatter-cost))
               (:antimatter dl-base))
      (should= (- glc/base-antimatter-maximum
                  (* (:torpedos wpn-base) glc/weapon-factory-torpedo-antimatter-cost)
                  (* (:kinetics wpn-base) glc/weapon-factory-kinetic-antimatter-cost))
               (:antimatter wpn-base))
      (should= (- glc/base-dilithium-maximum
                  (* (:torpedos wpn-base) glc/weapon-factory-torpedo-dilithium-cost))
               (:dilithium wpn-base))))

  (it "does not manufacture beyond maximum resource capacities"
    (let [am-base (mom/make-base)
          dl-base (mom/make-base)
          wpn-base (mom/make-base)
          am-base (assoc am-base :x 1
                                 :age (inc glc/base-maturity-age)
                                 :type :antimatter-factory
                                 :antimatter glc/base-antimatter-maximum)
          dl-base (assoc dl-base :x 2
                                 :age (inc glc/base-maturity-age)
                                 :type :dilithium-factory
                                 :dilithium glc/base-dilithium-maximum)
          wpn-base (assoc wpn-base :x 3
                                   :age (inc glc/base-maturity-age)
                                   :type :weapon-factory
                                   :kinetics glc/base-kinetics-maximum
                                   :torpedos glc/base-torpedos-maximum)
          bases [am-base dl-base wpn-base]
          [am-base dl-base wpn-base] (bases/update-bases-manufacturing 10 bases)]
      (should= glc/base-antimatter-maximum (:antimatter am-base))
      (should= glc/base-dilithium-maximum (:dilithium dl-base))
      (should= glc/base-torpedos-maximum (:torpedos wpn-base))
      (should= glc/base-kinetics-maximum (:kinetics wpn-base))))
  )

(describe "base transport behavior"
  (it "prevents transport launch when transport is not ready"
    (let [base (mom/make-base 0 0 :antimatter-factory 0 0)
          base (assoc base :transport-readiness (dec glc/transport-ready))]
      (should= false (bases/transport-ready? base))))

  (it "allows transport launch when transport is ready"
    (let [base (mom/make-base 0 0 :antimatter-factory 0 0)
          base (assoc base :transport-readiness glc/transport-ready)]
      (should= true (bases/transport-ready? base))))

  (it "increases transport readiness with time"
    (let [base (mom/make-base)
          base (assoc base :transport-readiness 0)
          world (assoc (mom/make-world) :bases [base])
          {:keys [bases]} (bases/update-bases 10 world)
          base (first bases)]
      (should= 10 (:transport-readiness base))))

  (it "caps transport readiness at maximum"
    (let [base (mom/make-base)
          base (assoc base :transport-readiness glc/transport-ready)
          base (bases/update-transport-readiness-for 10 base)]
      (should= glc/transport-ready (:transport-readiness base)))))

(def antimatter-transport-parameters
  [{:source-type :antimatter-factory :dest-type :antimatter-factory
    :reserve glc/antimatter-factory-antimatter-reserve :sufficient glc/antimatter-factory-sufficient-antimatter}
   {:source-type :antimatter-factory :dest-type :weapon-factory
    :reserve glc/antimatter-factory-antimatter-reserve :sufficient glc/weapon-factory-sufficient-antimatter}
   {:source-type :antimatter-factory :dest-type :dilithium-factory
    :reserve glc/antimatter-factory-antimatter-reserve :sufficient glc/dilithium-factory-sufficient-antimatter}
   {:source-type :dilithium-factory :dest-type :antimatter-factory
    :reserve glc/dilithium-factory-antimatter-reserve :sufficient glc/antimatter-factory-sufficient-antimatter}
   {:source-type :dilithium-factory :dest-type :weapon-factory
    :reserve glc/dilithium-factory-antimatter-reserve :sufficient glc/weapon-factory-sufficient-antimatter}
   {:source-type :dilithium-factory :dest-type :dilithium-factory
    :reserve glc/dilithium-factory-antimatter-reserve :sufficient glc/dilithium-factory-sufficient-antimatter}
   {:source-type :weapon-factory :dest-type :antimatter-factory
    :reserve glc/weapon-factory-antimatter-reserve :sufficient glc/antimatter-factory-sufficient-antimatter}
   {:source-type :weapon-factory :dest-type :weapon-factory
    :reserve glc/weapon-factory-antimatter-reserve :sufficient glc/weapon-factory-sufficient-antimatter}
   {:source-type :weapon-factory :dest-type :dilithium-factory
    :reserve glc/weapon-factory-antimatter-reserve :sufficient glc/dilithium-factory-sufficient-antimatter}])

(describe "antimatter transport"
  (context "sufficient antimatter at destination"
    (for [{:keys [source-type dest-type reserve sufficient]} antimatter-transport-parameters]
      (it (str "transports from " source-type " to " dest-type)
        (let [source (assoc (mom/make-base 0 0 source-type 0 0) :antimatter (inc (+ reserve glc/antimatter-cargo-size)))
              dest (assoc (mom/make-base 20 20 dest-type 0 0) :antimatter (inc sufficient))]
          (should= false (bases/should-transport-antimatter? source dest []))))))

  (context "sufficient antimatter promised"
    (for [{:keys [source-type dest-type reserve sufficient]} antimatter-transport-parameters]
      (it (str "does not transport from " source-type " to " dest-type)
        (let [source (assoc (mom/make-base 0 0 source-type 0 0) :antimatter (inc (+ reserve glc/antimatter-cargo-size)))
              dest (assoc (mom/make-base 20 20 dest-type 0 0) :antimatter 0)
              transport (bases/make-transport :antimatter (inc sufficient) [20 20])
              transports [transport]]
          (should= false (bases/should-transport-antimatter? source dest transports))))))

  (context "antimatter would deplete reserve"
    (for [{:keys [source-type dest-type reserve sufficient]} antimatter-transport-parameters]
      (it (str "does not transport from " source-type " to " dest-type)
        (let [source (assoc (mom/make-base 0 0 source-type 0 0) :antimatter (dec (+ reserve glc/antimatter-cargo-size)))
              dest (assoc (mom/make-base 20 20 dest-type 0 0) :antimatter (dec sufficient))]
          (should= false (bases/should-transport-antimatter? source dest []))))))

  (context "destination has insufficient antimatter and source stays above reserve"
    (for [{:keys [source-type dest-type reserve sufficient]} antimatter-transport-parameters]
      (it (str "transports from " source-type " to " dest-type " if destination has insufficient antimatter and source stays above reserve")
        (let [source (assoc (mom/make-base 0 0 source-type 0 0) :antimatter (inc (+ reserve glc/antimatter-cargo-size)))
              dest (assoc (mom/make-base 20 20 dest-type 0 0) :antimatter (dec sufficient))]
          (should= true (bases/should-transport-antimatter? source dest []))))))
  )

(def dilithium-transport-parameters
  [{:source-type :antimatter-factory :dest-type :antimatter-factory
    :reserve glc/antimatter-factory-dilithium-reserve :sufficient glc/antimatter-factory-sufficient-dilithium}
   {:source-type :antimatter-factory :dest-type :weapon-factory
    :reserve glc/antimatter-factory-dilithium-reserve :sufficient glc/weapon-factory-sufficient-dilithium}
   {:source-type :antimatter-factory :dest-type :dilithium-factory
    :reserve glc/antimatter-factory-dilithium-reserve :sufficient glc/dilithium-factory-sufficient-dilithium}
   {:source-type :dilithium-factory :dest-type :antimatter-factory
    :reserve glc/dilithium-factory-dilithium-reserve :sufficient glc/antimatter-factory-sufficient-dilithium}
   {:source-type :dilithium-factory :dest-type :weapon-factory
    :reserve glc/dilithium-factory-dilithium-reserve :sufficient glc/weapon-factory-sufficient-dilithium}
   {:source-type :dilithium-factory :dest-type :dilithium-factory
    :reserve glc/dilithium-factory-dilithium-reserve :sufficient glc/dilithium-factory-sufficient-dilithium}
   {:source-type :weapon-factory :dest-type :antimatter-factory
    :reserve glc/weapon-factory-dilithium-reserve :sufficient glc/antimatter-factory-sufficient-dilithium}
   {:source-type :weapon-factory :dest-type :weapon-factory
    :reserve glc/weapon-factory-dilithium-reserve :sufficient glc/weapon-factory-sufficient-dilithium}
   {:source-type :weapon-factory :dest-type :dilithium-factory
    :reserve glc/weapon-factory-dilithium-reserve :sufficient glc/dilithium-factory-sufficient-dilithium}])

(describe "dilithium transport from source to destination"
  (context "destination has sufficient dilithium"
    (for [{:keys [source-type dest-type reserve sufficient]} dilithium-transport-parameters]
      (it (str "does not transport from " source-type " to " dest-type)
        (let [source (assoc (mom/make-base 0 0 source-type 0 0) :dilithium (inc (+ reserve glc/dilithium-cargo-size)))
              dest (assoc (mom/make-base 20 20 dest-type 0 0) :dilithium (inc sufficient))]
          (should= false (bases/should-transport-dilithium? source dest []))))))

  (context "destination has sufficient dilithium promised"
    (for [{:keys [source-type dest-type reserve sufficient]} dilithium-transport-parameters]
      (it (str "does not transport from " source-type " to " dest-type)
        (let [source (assoc (mom/make-base 0 0 source-type 0 0) :dilithium (inc (+ reserve glc/dilithium-cargo-size)))
              dest (assoc (mom/make-base 20 20 dest-type 0 0) :dilithium 0)
              transport (bases/make-transport :dilithium (inc sufficient) [20 20])
              transports [transport]]
          (should= false (bases/should-transport-dilithium? source dest transports))))))

  (context "source dilithium would go below reserve"
    (for [{:keys [source-type dest-type reserve sufficient]} dilithium-transport-parameters]
      (it (str "does not transport from " source-type " to " dest-type)
        (let [source (assoc (mom/make-base 0 0 source-type 0 0) :dilithium (dec (+ reserve glc/dilithium-cargo-size)))
              dest (assoc (mom/make-base 20 20 dest-type 0 0) :dilithium (dec sufficient))]
          (should= false (bases/should-transport-dilithium? source dest []))))))

  (context "destination has insufficient dilithium and source stays above reserve"
    (for [{:keys [source-type dest-type reserve sufficient]} dilithium-transport-parameters]
      (it (str "transports from " source-type " to " dest-type)
        (let [source (assoc (mom/make-base 0 0 source-type 0 0) :dilithium (inc (+ reserve glc/dilithium-cargo-size)))
              dest (assoc (mom/make-base 20 20 dest-type 0 0) :dilithium (dec sufficient))]
          (should= true (bases/should-transport-dilithium? source dest []))))))
  )

(describe "transport to corbomite device"
  (it "does not transport antimatter to a corbomite device"
    (let [am-base (mom/make-base 0 0 :antimatter-factory glc/base-antimatter-maximum 0)
          corbomite-device (mom/make-base 100 100 :corbomite-device 0 0)]
      (should= false (bases/should-transport-antimatter? am-base corbomite-device []))))

  (it "does not transport dilithium to a corbomite device"
    (let [dl-base (mom/make-base 0 0 :dilithium-factory 0 glc/base-dilithium-maximum)
          corbomite-device (mom/make-base 100 100 :corbomite-device 0 0)]
      (should= false (bases/should-transport-dilithium? dl-base corbomite-device [])))))

(describe "transport analysis timing"
  (it "updates transport-check-time once transport-check-period has passed"
    (let [world (assoc (mom/make-world) :transport-check-time 0
                                        :update-time (inc glc/transport-check-period))]
      (should= glc/transport-check-period (:transport-check-time (bases/check-new-transport-time world))))))

(defn- deploy-bases [world source dest]
  (let [world (assoc world :bases [source])
        world (ship/add-transport-routes-to world dest)
        world (update-in world [:bases] conj dest)]
    world))

(describe "transport scenarios"
  (it "finds base from coordinates"
    (let [base (mom/make-base 0 0 :dilithium-factory 0 0)
          bases [base]]
      (should= base (bases/base-from-coordinates bases [0 0]))
      (should= nil (bases/base-from-coordinates bases [1 1]))))

  (it "does not create dilithium transport when there is nothing to ship"
    (let [world (mom/make-world)
          source (mom/make-base 0 0 :dilithium-factory 0 0)
          dest (mom/make-base 0 (dec glc/transport-range) :weapon-factory 0 0 0 0)
          world (deploy-bases world source dest)
          world (bases/check-new-transports world)
          transports (:transports world)]
      (should= [] transports)))

  (it "does not create transport when a klingon is nearby"
    (let [world (mom/make-world)
          source (mom/make-base 0 0 :dilithium-factory 0 (+ glc/dilithium-cargo-size glc/dilithium-factory-dilithium-reserve))
          source (assoc source :transport-readiness glc/transport-ready)
          dest (mom/make-base 0 (dec glc/transport-range) :weapon-factory 0 0 0 0)
          klingon (assoc (mom/make-klingon) :x (:x source) :y (+ (:y source) (dec glc/ship-docking-distance)))
          world (deploy-bases world source dest)
          world (assoc world :klingons [klingon])
          world (bases/check-new-transports world)
          transports (:transports world)]
      (should= [] transports)))

  (with-stubs)
  (it "creates dilithium transport when there is something to ship"
    (with-redefs [bases/random-transport-velocity-magnitude (constantly glc/transport-velocity)]
      (let [world (mom/make-world)
            source (mom/make-base 0 0 :dilithium-factory 0 (+ glc/dilithium-cargo-size glc/dilithium-factory-dilithium-reserve))
            source (assoc source :transport-readiness glc/transport-ready)
            dest (mom/make-base 0 (dec glc/transport-range) :weapon-factory 0 0 0 0)
            world (deploy-bases world source dest)
            world (bases/check-new-transports world)
            transports (:transports world)
            transport (first transports)
            [source _] (:bases world)]
        (should= 1 (count transports))
        (should= 0 (:x transport))
        (should= 0 (:y transport))
        (should= :dilithium (:commodity transport))
        (should= glc/dilithium-cargo-size (:amount transport))
        (should= [0 (dec glc/transport-range)] (:destination transport))
        (should (ut/roughly-v [0 glc/transport-velocity] (:velocity transport)))
        (should= glc/dilithium-factory-dilithium-reserve (:dilithium source))
        (should= 0 (:transport-readiness source)))))

  (with-stubs)
  (it "creates antimatter transport when there is something to ship"
    (with-redefs [bases/random-transport-velocity-magnitude (constantly glc/transport-velocity)]
      (let [world (mom/make-world)
            source (mom/make-base 0 0 :antimatter-factory (+ glc/antimatter-cargo-size glc/antimatter-factory-antimatter-reserve) 0)
            dest (mom/make-base (dec glc/transport-range) 0 :weapon-factory 0 0 0 0)
            source (assoc source :transport-readiness glc/transport-ready)
            world (deploy-bases world source dest)
            world (bases/check-new-transports world)
            transports (:transports world)
            transport (first transports)
            [source _] (:bases world)]
        (should= 1 (count transports))
        (should= 0 (:x transport))
        (should= 0 (:y transport))
        (should= :antimatter (:commodity transport))
        (should= glc/antimatter-cargo-size (:amount transport))
        (should= [(dec glc/transport-range) 0] (:destination transport))
        (should-be #(ut/roughly-v % [glc/transport-velocity 0]) (:velocity transport))
        (should= glc/antimatter-factory-antimatter-reserve (:antimatter source))
        (should= 0 (:transport-readiness source)))))

  (with-stubs)
  (it "launches only one transport to the neediest destination"
    (with-redefs [bases/random-transport-velocity-magnitude (constantly glc/transport-velocity)]
      (let [world (mom/make-world)
            source (mom/make-base 0 0 :antimatter-factory (+ glc/antimatter-cargo-size glc/antimatter-factory-antimatter-reserve) 0)
            wrong-dest (mom/make-base (dec glc/transport-range) 0 :weapon-factory 10 0 0 0)
            right-dest (mom/make-base (- glc/transport-range 2) 0 :weapon-factory 0 0 0 0)
            source (assoc source :transport-readiness glc/transport-ready)
            world (deploy-bases world source wrong-dest)
            world (ship/add-transport-routes-to world right-dest)
            world (update-in world [:bases] conj right-dest)
            world (bases/check-new-transports world)
            transports (:transports world)
            transport (first transports)
            [source _] (:bases world)]
        (should= 1 (count transports))
        (should= 0 (:x transport))
        (should= 0 (:y transport))
        (should= :antimatter (:commodity transport))
        (should= glc/antimatter-cargo-size (:amount transport))
        (should= [(- glc/transport-range 2) 0] (:destination transport))
        (should-be #(ut/roughly-v % [glc/transport-velocity 0]) (:velocity transport))
        (should= glc/antimatter-factory-antimatter-reserve (:antimatter source))
        (should= 0 (:transport-readiness source)))))

  (it "moves transports over time"
    (let [world (mom/make-world)
          transport (bases/make-transport :antimatter 100 [100 100])
          transport (assoc transport :velocity [2 2])
          world (assoc world :transports [transport])
          world (bases/update-transports 10 world)
          transports (:transports world)
          transport (first transports)]
      (should= 20 (:x transport))
      (should= 20 (:y transport))))

  (it "does not receive transports not near destination"
    (let [world (mom/make-world)
          dest (mom/make-base 0 0 :antimatter-factory 0 0)
          transport (bases/make-transport :antimatter 100 [0 (inc glc/transport-delivery-range)])
          world (assoc world :bases [dest] :transports [transport])
          world (bases/receive-transports world)
          transports (:transports world)
          [dest] (:bases world)]
      (should= 1 (count transports))
      (should= 0 (:antimatter dest))))

  (it "receives transports near destination"
    (let [world (mom/make-world)
          dest (mom/make-base 0 (dec glc/transport-delivery-range) :antimatter-factory 0 0)
          transport (bases/make-transport :antimatter 100 [0 (dec glc/transport-delivery-range)])
          world (assoc world :bases [dest] :transports [transport])
          world (bases/receive-transports world)
          transports (:transports world)
          [dest] (:bases world)]
      (should= 0 (count transports))
      (should= 100 (:antimatter dest))))

  (it "discards excess commodities when receiving transports"
    (let [world (mom/make-world)
          dest (mom/make-base 0 (dec glc/transport-delivery-range) :antimatter-factory 0 0)
          dest (assoc dest :antimatter glc/base-antimatter-maximum)
          transport (bases/make-transport :antimatter 100 [0 (dec glc/transport-delivery-range)])
          world (assoc world :bases [dest] :transports [transport])
          world (bases/receive-transports world)
          transports (:transports world)
          [dest] (:bases world)]
      (should= 0 (count transports))
      (should= glc/base-antimatter-maximum (:antimatter dest)))))

(describe "corbomite base and device behavior"
  (it "converts a full corbomite base into a corbomite device with an explosion"
    (let [world (mom/make-world)
          base (mom/make-base 0 0 :corbomite-factory 100 100)
          base (assoc base :corbomite glc/corbomite-maximum)
          world (assoc world :bases [base])
          world (bases/update-bases 1 world)
          base (-> world :bases first)
          explosion (-> world :explosions first)]
      (should= :corbomite-device (:type base))
      (should= 0 (:corbomite base))
      (should= 0 (:antimatter base))
      (should= 0 (:dilithium base))
      (should-not-be-nil explosion)
      (should= :corbomite-device (:type explosion))))

  (it "does not receive transports near a corbomite device"
    (let [world (mom/make-world)
          dest (mom/make-base 0 (dec glc/transport-delivery-range) :corbomite-device 0 0)
          transport (bases/make-transport :antimatter 100 [0 (dec glc/transport-delivery-range)])
          world (assoc world :bases [dest] :transports [transport])
          world (bases/receive-transports world)
          transports (:transports world)
          [dest] (:bases world)]
      (should= 0 (count transports))
      (should= 0 (:antimatter dest))))

  (it "removes routes to a base"
    (let [world (mom/make-world)
          corb (mom/make-base 0 0 :antimatter-factory 0 0)
          world (assoc world :bases [corb])
          b1 (mom/make-base 1 1 :antimatter-factory 0 0)
          b2 (mom/make-base 2 2 :antimatter-factory 0 0)
          world (ship/add-transport-routes-to world b1)
          world (update-in world [:bases] conj b1)
          world (ship/add-transport-routes-to world b2)
          world (update-in world [:bases] conj b2)
          world (bases/remove-routes-to-base world corb)]
      (should= #{#{[2 2] [1 1]}} (:transport-routes world))))
  )
