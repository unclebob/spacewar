(ns spacewar.game-logic.bases-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.bases :refer :all]
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
        base (assoc base :age base-maturity-age)
        bases [base]
        aged-bases (age-bases 10 bases)]
    (:age (first aged-bases)) => base-maturity-age))

(fact
  "Immature bases do not manufacture"
  (let [am-base (mom/make-base)
        dl-base (mom/make-base)
        wpn-base (mom/make-base)
        am-base (assoc am-base :age (dec base-maturity-age)
                               :type :antimatter-factory)
        dl-base (assoc dl-base :age (dec base-maturity-age)
                               :type :dilithium-factory)
        wpn-base (assoc wpn-base :age (dec base-maturity-age)
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
        am-base (assoc am-base :age (inc base-maturity-age)
                               :type :antimatter-factory)
        dl-base (assoc dl-base :age (inc base-maturity-age)
                               :type :dilithium-factory
                               :antimatter base-antimatter-maximum)
        wpn-base (assoc wpn-base :age (inc base-maturity-age)
                                 :type :weapon-factory
                                 :antimatter base-antimatter-maximum
                                 :dilithium base-dilithium-maximum)
        bases [am-base dl-base wpn-base]
        ms 100000
        [am-base dl-base wpn-base] (update-bases-manufacturing ms bases)]
    (:antimatter am-base) => (* ms antimatter-factory-production-rate)
    (:dilithium dl-base) => (* ms dilithium-factory-production-rate)
    (:torpedos wpn-base) => (* ms weapon-factory-torpedo-production-rate)
    (:kinetics wpn-base) => (* ms weapon-factory-kinetic-production-rate)
    (:antimatter dl-base) => (- base-antimatter-maximum (* (:dilithium dl-base) dilithium-factory-dilithium-antimatter-cost))
    (:antimatter wpn-base) => (- base-antimatter-maximum
                                 (* (:torpedos wpn-base) weapon-factory-torpedo-antimatter-cost)
                                 (* (:kinetics wpn-base) weapon-factory-kinetic-antimatter-cost))
    (:dilithium wpn-base) => (- base-dilithium-maximum
                                (* (:torpedos wpn-base) weapon-factory-torpedo-dilithium-cost))))

(fact
  "Mature bases do not manufacture beyond maximums"
  (let [am-base (mom/make-base)
        dl-base (mom/make-base)
        wpn-base (mom/make-base)
        am-base (assoc am-base :age (inc base-maturity-age)
                               :type :antimatter-factory
                               :antimatter base-antimatter-maximum)
        dl-base (assoc dl-base :age (inc base-maturity-age)
                               :type :dilithium-factory
                               :dilithium base-dilithium-maximum)
        wpn-base (assoc wpn-base :age (inc base-maturity-age)
                                 :type :weapon-factory
                                 :kinetics base-kinetics-maximum
                                 :torpedos base-torpedos-maximum)
        bases [am-base dl-base wpn-base]
        [am-base dl-base wpn-base] (update-bases-manufacturing 10 bases)]
    (:antimatter am-base) => base-antimatter-maximum
    (:dilithium dl-base) => base-dilithium-maximum
    (:torpedos wpn-base) => base-torpedos-maximum
    (:kinetics wpn-base) => base-kinetics-maximum))

(fact
  "no transport route to bases that are out of range"
  (let [wpn-base (mom/make-base 0 0 :weapon-factory 0 0)
        dl-base (mom/make-base (inc transport-range) 0 :dilithium-factory 0 0)
        am-base (mom/make-base 0 (inc transport-range) :antimatter-factory 0 0 0 0)
        bases [am-base dl-base wpn-base]
        transport-routes (find-transport-targets-for am-base bases)]
    transport-routes => []))

(fact
  "find transport route to bases that are in range"
  (let [wpn-base (mom/make-base 0 0 :weapon-factory 0 0)
        dl-base (mom/make-base (dec transport-range) 0 :dilithium-factory 0 0)
        am-base (mom/make-base 0 (dec transport-range) :antimatter-factory 0 0 0 0)
        bases [am-base dl-base wpn-base]
        transport-routes (find-transport-targets-for wpn-base bases)]
    transport-routes => (just #{dl-base am-base})))

(fact
  "base cannot launch transport if transport is not ready"
  (let [base (mom/make-base 0 0 :antimatter-factory 0 0)
        base (assoc base :transport-readiness (dec transport-ready))]
    (transport-ready? base) => false))

(fact
  "base can launch transport if transport is ready"
  (let [base (mom/make-base 0 0 :antimatter-factory 0 0)
        base (assoc base :transport-readiness transport-ready)]
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
        base (assoc base :transport-readiness transport-ready)
        base (update-transport-readiness-for 10 base)]
    (:transport-readiness base) => transport-ready))

(tabular
  (facts
    "Transport antimatter from source to destination "
    (let [source (mom/make-base 0 0 ?source-type 0 0)
          dest (mom/make-base 20 20 ?dest-type 0 0)]

      (fact
        "should not transport if destination has sufficient antimatter"
        (let [source (assoc source :antimatter (inc (+ ?reserve antimatter-cargo-size)))
              dest (assoc dest :antimatter (inc ?sufficient))]
          (should-transport-antimatter? source dest []) => false))

      (fact
        "should not transport if destination has sufficient antimatter promised"
        (let [source (assoc source :antimatter (inc (+ ?reserve antimatter-cargo-size)))
              dest (assoc dest :antimatter 0)
              transport (make-transport :antimatter (inc ?sufficient) [20 20])
              transports [transport]]
          (should-transport-antimatter? source dest transports) => false))

      (fact
        "should not transport if source antimatter would go below reserve"
        (let [source (assoc source :antimatter (dec (+ ?reserve antimatter-cargo-size)))
              dest (assoc dest :antimatter (dec ?sufficient))]
          (should-transport-antimatter? source dest []) => false))

      (fact
        "should transport if destination has insufficient antimatter and source will not go below reserve"
        (let [source (assoc source :antimatter (inc (+ ?reserve antimatter-cargo-size)))
              dest (assoc dest :antimatter (dec ?sufficient))]
          (should-transport-antimatter? source dest []) => true))))
  ?source-type ?dest-type ?reserve ?sufficient
  :antimatter-factory :antimatter-factory antimatter-factory-antimatter-reserve antimatter-factory-sufficient-antimatter
  :antimatter-factory :weapon-factory antimatter-factory-antimatter-reserve weapon-factory-sufficient-antimatter
  :antimatter-factory :dilithium-factory antimatter-factory-antimatter-reserve dilithium-factory-sufficient-antimatter

  :dilithium-factory :antimatter-factory dilithium-factory-antimatter-reserve antimatter-factory-sufficient-antimatter
  :dilithium-factory :weapon-factory dilithium-factory-antimatter-reserve weapon-factory-sufficient-antimatter
  :dilithium-factory :dilithium-factory dilithium-factory-antimatter-reserve dilithium-factory-sufficient-antimatter

  :weapon-factory :antimatter-factory weapon-factory-antimatter-reserve antimatter-factory-sufficient-antimatter
  :weapon-factory :weapon-factory weapon-factory-antimatter-reserve weapon-factory-sufficient-antimatter
  :weapon-factory :dilithium-factory weapon-factory-antimatter-reserve dilithium-factory-sufficient-antimatter
  )

(tabular
  (facts
    "Transport dilithium from source to destination "
    (let [source (mom/make-base 0 0 ?source-type 0 0)
          dest (mom/make-base 20 20 ?dest-type 0 0)]

      (fact
        "should not transport if destination has sufficient dilithium"
        (let [source (assoc source :dilithium (inc (+ ?reserve dilithium-cargo-size)))
              dest (assoc dest :dilithium (inc ?sufficient))]
          (should-transport-dilithium? source dest []) => false))

      (fact
        "should not transport if destination has sufficient dilithium promised"
        (let [source (assoc source :dilithium (inc (+ ?reserve dilithium-cargo-size)))
              dest (assoc dest :dilithium 0)
              transport (make-transport :dilithium (inc ?sufficient) [20 20])
              transports [transport]]
          (should-transport-dilithium? source dest transports) => false))

      (fact
        "should not transport if source antimatter would go below reserve"
        (let [source (assoc source :dilithium (dec (+ ?reserve dilithium-cargo-size)))
              dest (assoc dest :dilithium (dec ?sufficient))]
          (should-transport-dilithium? source dest []) => false))

      (fact
        "should transport if destination has insufficient dilithium and source will not go below reserve"
        (let [source (assoc source :dilithium (inc (+ ?reserve dilithium-cargo-size)))
              dest (assoc dest :dilithium (dec ?sufficient))]
          (should-transport-dilithium? source dest []) => true))))

  ?source-type ?dest-type ?reserve ?sufficient
  :antimatter-factory :antimatter-factory antimatter-factory-dilithium-reserve antimatter-factory-sufficient-dilithium
  :antimatter-factory :weapon-factory antimatter-factory-dilithium-reserve weapon-factory-sufficient-dilithium
  :antimatter-factory :dilithium-factory antimatter-factory-dilithium-reserve dilithium-factory-sufficient-dilithium

  :dilithium-factory :antimatter-factory dilithium-factory-dilithium-reserve antimatter-factory-sufficient-dilithium
  :dilithium-factory :weapon-factory dilithium-factory-dilithium-reserve weapon-factory-sufficient-dilithium
  :dilithium-factory :dilithium-factory dilithium-factory-dilithium-reserve dilithium-factory-sufficient-dilithium

  :weapon-factory :antimatter-factory weapon-factory-dilithium-reserve antimatter-factory-sufficient-dilithium
  :weapon-factory :weapon-factory weapon-factory-dilithium-reserve weapon-factory-sufficient-dilithium
  :weapon-factory :dilithium-factory weapon-factory-dilithium-reserve dilithium-factory-sufficient-dilithium
  )

(fact
  "transport analysis is not done within the transport-check-period of the last"
  (let [world (assoc (mom/make-world) :transport-check-time 0
                                      :update-time (dec transport-check-period))]
    (check-new-transport-time world) => world))

(fact
  "transport analysis is done once transport-check-period has passed"
  (let [world (assoc (mom/make-world) :transport-check-time 0
                                      :update-time (inc transport-check-period))]
    (:transport-check-time (check-new-transport-time world)) => transport-check-period))

(fact
  "No dilithium transport created when there is nothing to ship"
  (let [world (mom/make-world)
        source (mom/make-base 0 0 :dilithium-factory 0 0)
        dest (mom/make-base 0 (dec transport-range) :weapon-factory 0 0 0 0)
        world (assoc world :bases [source dest])
        world (check-new-transports world)
        transports (:transports world)]
    transports => []))

(fact
  "No transport created when there is a klingon nearby"
  (let [world (mom/make-world)
        source (mom/make-base 0 0 :dilithium-factory 0 (+ dilithium-cargo-size dilithium-factory-dilithium-reserve))
        source (assoc source :transport-readiness transport-ready)
        dest (mom/make-base 0 (dec transport-range) :weapon-factory 0 0 0 0)
        klingon (assoc (mom/make-klingon) :x (:x source) :y (+ (:y source) (dec ship-docking-distance)))
        world (assoc world :bases [source dest] :klingons [klingon])
        world (check-new-transports world)
        transports (:transports world)]
    transports => []))

(fact
  "Dilithium transport created when there is something to ship"
  (prerequisite (random-transport-velocity-magnitude) => transport-velocity)
  (let [world (mom/make-world)
        source (mom/make-base 0 0 :dilithium-factory 0 (+ dilithium-cargo-size dilithium-factory-dilithium-reserve))
        source (assoc source :transport-readiness transport-ready)
        dest (mom/make-base 0 (dec transport-range) :weapon-factory 0 0 0 0)
        world (assoc world :bases [source dest])
        world (check-new-transports world)
        transports (:transports world)
        transport (first transports)
        [source _] (:bases world)]
    (count transports) => 1
    (:x transport) => 0
    (:y transport) => 0
    (:commodity transport) => :dilithium
    (:amount transport) => dilithium-cargo-size
    (:destination transport) => [0 (dec transport-range)]
    (:velocity transport) => (vt/roughly-v [0 transport-velocity])
    (:dilithium source) => dilithium-factory-dilithium-reserve
    (:transport-readiness source) => 0))

(fact
  "Antimatter transport created when there is something to ship"
  (prerequisite (random-transport-velocity-magnitude) => transport-velocity)
  (let [world (mom/make-world)
        source (mom/make-base 0 0 :antimatter-factory (+ antimatter-cargo-size antimatter-factory-antimatter-reserve) 0)
        dest (mom/make-base (dec transport-range) 0 :weapon-factory 0 0 0 0)
        source (assoc source :transport-readiness transport-ready)
        world (assoc world :bases [source dest])
        world (check-new-transports world)
        transports (:transports world)
        transport (first transports)
        [source _] (:bases world)]
    (count transports) => 1
    (:x transport) => 0
    (:y transport) => 0
    (:commodity transport) => :antimatter
    (:amount transport) => antimatter-cargo-size
    (:destination transport) => [(dec transport-range) 0]
    (:velocity transport) => (vt/roughly-v [transport-velocity 0])
    (:antimatter source) => antimatter-factory-antimatter-reserve
    (:transport-readiness source) => 0))

(fact
  "Only one transport launched for a source base to nearest dest."
  (prerequisite (random-transport-velocity-magnitude) => transport-velocity)
  (let [world (mom/make-world)
        source (mom/make-base 0 0 :antimatter-factory (+ antimatter-cargo-size antimatter-factory-antimatter-reserve) 0)
        wrong-dest (mom/make-base (dec transport-range) 0 :weapon-factory 0 0 0 0)
        right-dest (mom/make-base (- transport-range 2) 0 :weapon-factory 0 0 0 0)
        source (assoc source :transport-readiness transport-ready)
        world (assoc world :bases [source wrong-dest right-dest])
        world (check-new-transports world)
        transports (:transports world)
        transport (first transports)
        [source _] (:bases world)]
    (count transports) => 1
    (:x transport) => 0
    (:y transport) => 0
    (:commodity transport) => :antimatter
    (:amount transport) => antimatter-cargo-size
    (:destination transport) => [(- transport-range 2) 0]
    (:velocity transport) => (vt/roughly-v [transport-velocity 0])
    (:antimatter source) => antimatter-factory-antimatter-reserve
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
        transport (make-transport :antimatter 100 [0 (inc transport-delivery-range)])
        world (assoc world :bases [dest] :transports [transport])
        world (receive-transports world)
        transports (:transports world)
        [dest] (:bases world)]
    (count transports) => 1
    (:antimatter dest) => 0))

(fact
  "transports that are near destination are received"
  (let [world (mom/make-world)
        dest (mom/make-base 0 (dec transport-delivery-range) :antimatter-factory 0 0)
        transport (make-transport :antimatter 100 [0 (dec transport-delivery-range)])
        world (assoc world :bases [dest] :transports [transport])
        world (receive-transports world)
        transports (:transports world)
        [dest] (:bases world)]
    (count transports) => 0
    (:antimatter dest) => 100))

