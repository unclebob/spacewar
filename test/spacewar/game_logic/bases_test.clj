(ns spacewar.game-logic.bases-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.bases :refer :all]))

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

