(ns spacewar.game-logic.bases-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.bases :refer :all]
            [spacewar.game-logic.test-mother :as mom]))

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
                               :type :dilithium-factory)
        wpn-base (assoc wpn-base :age (inc base-maturity-age)
                                 :type :weapon-factory)
        bases [am-base dl-base wpn-base]
        [am-base dl-base wpn-base] (update-bases-manufacturing 10 bases)]
    (:antimatter am-base) => (* 10 antimatter-factory-production-rate)
    (:dilithium dl-base) => (* 10 dilithium-factory-production-rate)
    (:torpedos wpn-base) => (* 10 weapon-factory-torpedo-production-rate)
    (:kinetics wpn-base) => (* 10 weapon-factory-kinetic-production-rate)))

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
        dl-base (mom/make-base (inc trade-route-limit) 0 :dilithium-factory 0 0)
        am-base (mom/make-base 0 (inc trade-route-limit) :antimatter-factory 0 0 0 0)
        bases [am-base dl-base wpn-base]
        transport-routes (find-transport-targets-for am-base bases)]
    transport-routes => []))

(fact
  "find transport route to bases that are in range"
  (let [wpn-base (mom/make-base 0 0 :weapon-factory 0 0)
        dl-base (mom/make-base (dec trade-route-limit) 0 :dilithium-factory 0 0)
        am-base (mom/make-base 0 (dec trade-route-limit) :antimatter-factory 0 0 0 0)
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

(fact
  "should not transport antimatter from AM to WPN if AM has less than antimatter cargo limit"
  (let [am-base (mom/make-base 0 0 :antimatter-factory (dec antimatter-cargo-size) 0)
        wpn-base (mom/make-base 0 0 :weapon-factory 0 0)]
    (should-transport-antimatter? am-base wpn-base) => false))

(fact
  "should not transport antimatter from AM to WPN if WPN has sufficient antimatter"
  (let [am-base (mom/make-base 0 0 :antimatter-factory (inc antimatter-cargo-size) 0)
        wpn-base (mom/make-base 0 0 :weapon-factory (inc weapon-factory-antimatter-reserve) 0)]
    (should-transport-antimatter? am-base wpn-base) => false))

(fact
  "should transport antimatter from AM to WPN if WPN has insufficient antimatter and AM has more than cargo limit"
  (let [am-base (mom/make-base 0 0 :antimatter-factory (inc antimatter-cargo-size) 0)
        wpn-base (mom/make-base 0 0 :weapon-factory (dec weapon-factory-antimatter-reserve) 0)]
    (should-transport-antimatter? am-base wpn-base) => true))



