(ns spacewar.game-logic.klingon-test
  (:require [midje.sweet :refer :all]
              [spacewar.game-logic.config :refer :all]
              [spacewar.game-logic.klingons :as k]
              [clojure.spec.alpha :as s]))

(facts
  "about klingons"
  (fact
    "make-random-klingon"
    (s/explain-data ::k/klingon (k/make-random-klingon)) => nil)
  (fact
    "initialize"
    (let [klingons (k/initialize)]
      (s/explain-data ::k/klingons klingons) => nil
      (count klingons) => number-of-klingons)))
