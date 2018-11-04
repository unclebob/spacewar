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

