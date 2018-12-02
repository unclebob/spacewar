(ns spacewar.game-logic.stars-test
  (:require [midje.sweet :refer [facts fact]]
            [spacewar.game-logic.config :refer [number-of-stars]]
            [spacewar.game-logic.stars :as s]
            [clojure.spec.alpha :as spec]))

(facts
  "about stars"
  (fact
    "make-random-star"
    (spec/explain-data ::s/star (s/make-random-star)) => nil)
  (fact
    "initialize"
    (let [stars (s/initialize)]
      (spec/explain-data ::s/stars stars) => nil
      (count stars) => number-of-stars)))

