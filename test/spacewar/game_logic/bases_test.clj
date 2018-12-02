(ns spacewar.game-logic.bases-test
  (:require [midje.sweet :refer [fact facts just tabular =>]]
            [spacewar.game-logic.config :as glc]
            [spacewar.game-logic.bases :refer [make-random-base
                                               age-bases
                                               update-bases-manufacturing
                                               find-transport-targets-for
                                               transport-ready?
                                               update-bases
                                               update-transport-readiness-for
                                               should-transport-antimatter?
                                               make-transport
                                               should-transport-dilithium?
                                               check-new-transport-time
                                               check-new-transports
                                               random-transport-velocity-magnitude
                                               update-transports
                                               receive-transports
                                               ]]
            [spacewar.game-logic.test-mother :as mom]
            [spacewar.vector-test :as vt]))

(fact
  "bases age"
  (let [bases [(make-random-base) (make-random-base)]
        aged-bases (age-bases 10 bases)]
    (:age (first aged-bases)) => 10
    (:age (second aged-bases)) => 10))

(fact
  "bases stop aging once mature"
  (let [base (make-random-base)
        base (assoc base :age glc/base-maturity-age)
        bases [base]
        aged-bases (age-bases 10 bases)]
    (:age (first aged-bases)) => glc/base-maturity-age))

(fact
  "Immature bases do not manufacture"
  (let [am-base (mom/make-base)
        dl-base (mom/make-base)
        wpn-base (mom/make-base)
        am-base (assoc am-base :age (dec glc/base-maturity-age)
                               :type :antimatter-factory)
        dl-base (assoc dl-base :age (dec glc/base-maturity-age)
                               :type :dilithium-factory)
        wpn-base (assoc wpn-base :age (dec glc/base-maturity-age)
                                 :type :weapon-factory)
        bases [am-base dl-base wpn-base]
        [am-base dl-base wpn-base] (update-bases-manufacturing 10 bases)]
    (:antimatter am-base) => 0
    (:dilithium dl-base) => 0
    (:torpedos wpn-base) => 0
    (:kinetics wpn-base) => 0))

(fact
  "Mature bases do manufacture"
  (let [am-base (mom/make-base)
        dl-base (mom/make-base)
        wpn-base (mom/make-base)
        am-base (assoc am-base :age (inc glc/base-maturity-age)
                               :type :antimatter-factory)
        dl-base (assoc dl-base :age (inc glc/base-maturity-age)
                               :type :dilithium-factory
                               :antimatter glc/base-antimatter-maximum)
        wpn-base (assoc wpn-base :age (inc glc/base-maturity-age)
                                 :type :weapon-factory
                                 :antimatter glc/base-antimatter-maximum
                                 :dilithium glc/base-dilithium-maximum)
        bases [am-base dl-base wpn-base]
        ms 100000
        [am-base dl-base wpn-base] (update-bases-manufacturing ms bases)]
    (:antimatter am-base) => (* ms glc/antimatter-factory-production-rate)
    (:dilithium dl-base) => (* ms glc/dilithium-factory-production-rate)
    (:torpedos wpn-base) => (* ms glc/weapon-factory-torpedo-production-rate)
    (:kinetics wpn-base) => (* ms glc/weapon-factory-kinetic-production-rate)
    (:antimatter dl-base) => (- glc/base-antimatter-maximum (* (:dilithium dl-base) glc/dilithium-factory-dilithium-antimatter-cost))
    (:antimatter wpn-base) => (- glc/base-antimatter-maximum
                                 (* (:torpedos wpn-base) glc/weapon-factory-torpedo-antimatter-cost)
                                 (* (:kinetics wpn-base) glc/weapon-factory-kinetic-antimatter-cost))
    (:dilithium wpn-base) => (- glc/base-dilithium-maximum
                                (* (:torpedos wpn-base) glc/weapon-factory-torpedo-dilithium-cost))))

(fact
  "Mature bases do not manufacture beyond maximums"
  (let [am-base (mom/make-base)
        dl-base (mom/make-base)
        wpn-base (mom/make-base)
        am-base (assoc am-base :age (inc glc/base-maturity-age)
                               :type :antimatter-factory
                               :antimatter glc/base-antimatter-maximum)
        dl-base (assoc dl-base :age (inc glc/base-maturity-age)
                               :type :dilithium-factory
                               :dilithium glc/base-dilithium-maximum)
        wpn-base (assoc wpn-base :age (inc glc/base-maturity-age)
                                 :type :weapon-factory
                                 :kinetics glc/base-kinetics-maximum
                                 :torpedos glc/base-torpedos-maximum)
        bases [am-base dl-base wpn-base]
        [am-base dl-base wpn-base] (update-bases-manufacturing 10 bases)]
    (:antimatter am-base) => glc/base-antimatter-maximum
    (:dilithium dl-base) => glc/base-dilithium-maximum
    (:torpedos wpn-base) => glc/base-torpedos-maximum
    (:kinetics wpn-base) => glc/base-kinetics-maximum))

(fact
  "no transport route to bases that are out of range"
  (let [wpn-base (mom/make-base 0 0 :weapon-factory 0 0)
        dl-base (mom/make-base (inc glc/transport-range) 0 :dilithium-factory 0 0)
        am-base (mom/make-base 0 (inc glc/transport-range) :antimatter-factory 0 0 0 0)
        bases [am-base dl-base wpn-base]
        transport-routes (find-transport-targets-for am-base bases)]
    transport-routes => []))

(fact
  "find transport route to bases that are in range"
  (let [wpn-base (mom/make-base 0 0 :weapon-factory 0 0)
        dl-base (mom/make-base (dec glc/transport-range) 0 :dilithium-factory 0 0)
        am-base (mom/make-base 0 (dec glc/transport-range) :antimatter-factory 0 0 0 0)
        bases [am-base dl-base wpn-base]
        transport-routes (find-transport-targets-for wpn-base bases)]
    transport-routes => (just #{dl-base am-base})))

(fact
  "base cannot launch transport if transport is not ready"
  (let [base (mom/make-base 0 0 :antimatter-factory 0 0)
        base (assoc base :transport-readiness (dec glc/transport-ready))]
    (transport-ready? base) => false))

(fact
  "base can launch transport if transport is ready"
  (let [base (mom/make-base 0 0 :antimatter-factory 0 0)
        base (assoc base :transport-readiness glc/transport-ready)]
    (transport-ready? base) => true))

(fact
  "transport readiness increases with time"
  (let [base (mom/make-base)
        base (assoc base :transport-readiness 0)
        world (assoc (mom/make-world) :bases [base])
        {:keys [bases]} (update-bases 10 world)
        base (first bases)]
    (:transport-readiness base) => 10))

(fact
  "transport readiness does not increase beyond readiness"
  (let [base (mom/make-base)
        base (assoc base :transport-readiness glc/transport-ready)
        base (update-transport-readiness-for 10 base)]
    (:transport-readiness base) => glc/transport-ready))

(tabular
  (facts
    "Transport antimatter from source to destination "
    (let [source (mom/make-base 0 0 ?source-type 0 0)
          dest (mom/make-base 20 20 ?dest-type 0 0)]

      (fact
        "should not transport if destination has sufficient antimatter"
        (let [source (assoc source :antimatter (inc (+ ?reserve glc/antimatter-cargo-size)))
              dest (assoc dest :antimatter (inc ?sufficient))]
          (should-transport-antimatter? source dest []) => false))

      (fact
        "should not transport if destination has sufficient antimatter promised"
        (let [source (assoc source :antimatter (inc (+ ?reserve glc/antimatter-cargo-size)))
              dest (assoc dest :antimatter 0)
              transport (make-transport :antimatter (inc ?sufficient) [20 20])
              transports [transport]]
          (should-transport-antimatter? source dest transports) => false))

      (fact
        "should not transport if source antimatter would go below reserve"
        (let [source (assoc source :antimatter (dec (+ ?reserve glc/antimatter-cargo-size)))
              dest (assoc dest :antimatter (dec ?sufficient))]
          (should-transport-antimatter? source dest []) => false))

      (fact
        "should transport if destination has insufficient antimatter and source will not go below reserve"
        (let [source (assoc source :antimatter (inc (+ ?reserve glc/antimatter-cargo-size)))
              dest (assoc dest :antimatter (dec ?sufficient))]
          (should-transport-antimatter? source dest []) => true))))
  ?source-type ?dest-type ?reserve ?sufficient
  :antimatter-factory :antimatter-factory glc/antimatter-factory-antimatter-reserve glc/antimatter-factory-sufficient-antimatter
  :antimatter-factory :weapon-factory glc/antimatter-factory-antimatter-reserve glc/weapon-factory-sufficient-antimatter
  :antimatter-factory :dilithium-factory glc/antimatter-factory-antimatter-reserve glc/dilithium-factory-sufficient-antimatter

  :dilithium-factory :antimatter-factory glc/dilithium-factory-antimatter-reserve glc/antimatter-factory-sufficient-antimatter
  :dilithium-factory :weapon-factory glc/dilithium-factory-antimatter-reserve glc/weapon-factory-sufficient-antimatter
  :dilithium-factory :dilithium-factory glc/dilithium-factory-antimatter-reserve glc/dilithium-factory-sufficient-antimatter

  :weapon-factory :antimatter-factory glc/weapon-factory-antimatter-reserve glc/antimatter-factory-sufficient-antimatter
  :weapon-factory :weapon-factory glc/weapon-factory-antimatter-reserve glc/weapon-factory-sufficient-antimatter
  :weapon-factory :dilithium-factory glc/weapon-factory-antimatter-reserve glc/dilithium-factory-sufficient-antimatter
  )

(tabular
  (facts
    "Transport dilithium from source to destination "
    (let [source (mom/make-base 0 0 ?source-type 0 0)
          dest (mom/make-base 20 20 ?dest-type 0 0)]

      (fact
        "should not transport if destination has sufficient dilithium"
        (let [source (assoc source :dilithium (inc (+ ?reserve glc/dilithium-cargo-size)))
              dest (assoc dest :dilithium (inc ?sufficient))]
          (should-transport-dilithium? source dest []) => false))

      (fact
        "should not transport if destination has sufficient dilithium promised"
        (let [source (assoc source :dilithium (inc (+ ?reserve glc/dilithium-cargo-size)))
              dest (assoc dest :dilithium 0)
              transport (make-transport :dilithium (inc ?sufficient) [20 20])
              transports [transport]]
          (should-transport-dilithium? source dest transports) => false))

      (fact
        "should not transport if source antimatter would go below reserve"
        (let [source (assoc source :dilithium (dec (+ ?reserve glc/dilithium-cargo-size)))
              dest (assoc dest :dilithium (dec ?sufficient))]
          (should-transport-dilithium? source dest []) => false))

      (fact
        "should transport if destination has insufficient dilithium and source will not go below reserve"
        (let [source (assoc source :dilithium (inc (+ ?reserve glc/dilithium-cargo-size)))
              dest (assoc dest :dilithium (dec ?sufficient))]
          (should-transport-dilithium? source dest []) => true))))

  ?source-type ?dest-type ?reserve ?sufficient
  :antimatter-factory :antimatter-factory glc/antimatter-factory-dilithium-reserve glc/antimatter-factory-sufficient-dilithium
  :antimatter-factory :weapon-factory glc/antimatter-factory-dilithium-reserve glc/weapon-factory-sufficient-dilithium
  :antimatter-factory :dilithium-factory glc/antimatter-factory-dilithium-reserve glc/dilithium-factory-sufficient-dilithium

  :dilithium-factory :antimatter-factory glc/dilithium-factory-dilithium-reserve glc/antimatter-factory-sufficient-dilithium
  :dilithium-factory :weapon-factory glc/dilithium-factory-dilithium-reserve glc/weapon-factory-sufficient-dilithium
  :dilithium-factory :dilithium-factory glc/dilithium-factory-dilithium-reserve glc/dilithium-factory-sufficient-dilithium

  :weapon-factory :antimatter-factory glc/weapon-factory-dilithium-reserve glc/antimatter-factory-sufficient-dilithium
  :weapon-factory :weapon-factory glc/weapon-factory-dilithium-reserve glc/weapon-factory-sufficient-dilithium
  :weapon-factory :dilithium-factory glc/weapon-factory-dilithium-reserve glc/dilithium-factory-sufficient-dilithium
  )

(fact
  "transport analysis is not done within the transport-check-period of the last"
  (let [world (assoc (mom/make-world) :transport-check-time 0
                                      :update-time (dec glc/transport-check-period))]
    (check-new-transport-time world) => world))

(fact
  "transport analysis is done once transport-check-period has passed"
  (let [world (assoc (mom/make-world) :transport-check-time 0
                                      :update-time (inc glc/transport-check-period))]
    (:transport-check-time (check-new-transport-time world)) => glc/transport-check-period))

(fact
  "No dilithium transport created when there is nothing to ship"
  (let [world (mom/make-world)
        source (mom/make-base 0 0 :dilithium-factory 0 0)
        dest (mom/make-base 0 (dec glc/transport-range) :weapon-factory 0 0 0 0)
        world (assoc world :bases [source dest])
        world (check-new-transports world)
        transports (:transports world)]
    transports => []))

(fact
  "No transport created when there is a klingon nearby"
  (let [world (mom/make-world)
        source (mom/make-base 0 0 :dilithium-factory 0 (+ glc/dilithium-cargo-size glc/dilithium-factory-dilithium-reserve))
        source (assoc source :transport-readiness glc/transport-ready)
        dest (mom/make-base 0 (dec glc/transport-range) :weapon-factory 0 0 0 0)
        klingon (assoc (mom/make-klingon) :x (:x source) :y (+ (:y source) (dec glc/ship-docking-distance)))
        world (assoc world :bases [source dest] :klingons [klingon])
        world (check-new-transports world)
        transports (:transports world)]
    transports => []))

(fact
  "Dilithium transport created when there is something to ship"
  (prerequisite (random-transport-velocity-magnitude) => glc/transport-velocity)
  (let [world (mom/make-world)
        source (mom/make-base 0 0 :dilithium-factory 0 (+ glc/dilithium-cargo-size glc/dilithium-factory-dilithium-reserve))
        source (assoc source :transport-readiness glc/transport-ready)
        dest (mom/make-base 0 (dec glc/transport-range) :weapon-factory 0 0 0 0)
        world (assoc world :bases [source dest])
        world (check-new-transports world)
        transports (:transports world)
        transport (first transports)
        [source _] (:bases world)]
    (count transports) => 1
    (:x transport) => 0
    (:y transport) => 0
    (:commodity transport) => :dilithium
    (:amount transport) => glc/dilithium-cargo-size
    (:destination transport) => [0 (dec glc/transport-range)]
    (:velocity transport) => (vt/roughly-v [0 glc/transport-velocity])
    (:dilithium source) => glc/dilithium-factory-dilithium-reserve
    (:transport-readiness source) => 0))

(fact
  "Antimatter transport created when there is something to ship"
  (prerequisite (random-transport-velocity-magnitude) => glc/transport-velocity)
  (let [world (mom/make-world)
        source (mom/make-base 0 0 :antimatter-factory (+ glc/antimatter-cargo-size glc/antimatter-factory-antimatter-reserve) 0)
        dest (mom/make-base (dec glc/transport-range) 0 :weapon-factory 0 0 0 0)
        source (assoc source :transport-readiness glc/transport-ready)
        world (assoc world :bases [source dest])
        world (check-new-transports world)
        transports (:transports world)
        transport (first transports)
        [source _] (:bases world)]
    (count transports) => 1
    (:x transport) => 0
    (:y transport) => 0
    (:commodity transport) => :antimatter
    (:amount transport) => glc/antimatter-cargo-size
    (:destination transport) => [(dec glc/transport-range) 0]
    (:velocity transport) => (vt/roughly-v [glc/transport-velocity 0])
    (:antimatter source) => glc/antimatter-factory-antimatter-reserve
    (:transport-readiness source) => 0))

(fact
  "Only one transport launched for a source base to nearest dest."
  (prerequisite (random-transport-velocity-magnitude) => glc/transport-velocity)
  (let [world (mom/make-world)
        source (mom/make-base 0 0 :antimatter-factory (+ glc/antimatter-cargo-size glc/antimatter-factory-antimatter-reserve) 0)
        wrong-dest (mom/make-base (dec glc/transport-range) 0 :weapon-factory 0 0 0 0)
        right-dest (mom/make-base (- glc/transport-range 2) 0 :weapon-factory 0 0 0 0)
        source (assoc source :transport-readiness glc/transport-ready)
        world (assoc world :bases [source wrong-dest right-dest])
        world (check-new-transports world)
        transports (:transports world)
        transport (first transports)
        [source _] (:bases world)]
    (count transports) => 1
    (:x transport) => 0
    (:y transport) => 0
    (:commodity transport) => :antimatter
    (:amount transport) => glc/antimatter-cargo-size
    (:destination transport) => [(- glc/transport-range 2) 0]
    (:velocity transport) => (vt/roughly-v [glc/transport-velocity 0])
    (:antimatter source) => glc/antimatter-factory-antimatter-reserve
    (:transport-readiness source) => 0))

(fact
  "transports move"
  (let [world (mom/make-world)
        transport (make-transport :antimatter 100 [100 100])
        transport (assoc transport :velocity [2 2])
        world (assoc world :transports [transport])
        world (update-transports 10 world)
        transports (:transports world)
        transport (first transports)]
    (:x transport) => 20
    (:y transport) => 20))

(fact
  "transports that are not near destination are not received"
  (let [world (mom/make-world)
        dest (mom/make-base 0 0 :antimatter-factory 0 0)
        transport (make-transport :antimatter 100 [0 (inc glc/transport-delivery-range)])
        world (assoc world :bases [dest] :transports [transport])
        world (receive-transports world)
        transports (:transports world)
        [dest] (:bases world)]
    (count transports) => 1
    (:antimatter dest) => 0))

(fact
  "transports that are near destination are received"
  (let [world (mom/make-world)
        dest (mom/make-base 0 (dec glc/transport-delivery-range) :antimatter-factory 0 0)
        transport (make-transport :antimatter 100 [0 (dec glc/transport-delivery-range)])
        world (assoc world :bases [dest] :transports [transport])
        world (receive-transports world)
        transports (:transports world)
        [dest] (:bases world)]
    (count transports) => 0
    (:antimatter dest) => 100))

