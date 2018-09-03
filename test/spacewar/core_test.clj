(ns spacewar.core-test
  (:require [midje.sweet :refer :all]
            [spacewar.core :refer :all]))

(require '[clojure.test.check.generators :as gen])
(require '[midje.experimental :refer [for-all]])

(facts "hello"
       (fact "you"
             1 => 1
             1 =not=> 2

             ))

(facts "forall"
       (for-all
         [positive-num gen/s-pos-int
          int gen/int]
         (fact "An integer added to a positive number is always a number?"
               (+ positive-num int) => integer?)
         (fact "An integer added to a positive number is always positive?"
               (+ positive-num int) => pos?)
         (fact "integer division"
               (/ int positive-num) => integer?)
         (fact "zero divide"
               (/ positive-num int) => integer?)))