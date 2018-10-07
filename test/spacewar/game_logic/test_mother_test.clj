(ns spacewar.game-logic.test-mother-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.test-mother :refer :all]
            [spacewar.game-logic.ship :as ship]
            [spacewar.game-logic.klingons :as klingons]
            [clojure.spec.alpha :as spec]
            [spacewar.core :as core]))

(fact "make-world"
      (spec/explain-data ::core/world (make-world)) => nil)

(fact "make-ship"
      (spec/explain-data ::ship/ship (make-ship)) => nil)

(fact "make-klingon"
      (spec/explain-data ::klingons/klingon (make-klingon)) => nil)