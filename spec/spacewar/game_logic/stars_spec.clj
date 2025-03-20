(ns spacewar.game-logic.stars-spec
  (:require [clojure.spec.alpha :as spec]
            [spacewar.game-logic.stars :as s]
            [speclj.core :refer [describe it should-be-nil]]))

(describe "stars"
  (it "make-random-star"
    (should-be-nil (spec/explain-data ::s/star (s/make-random-star))))

  (it "initialize"
    (let [stars (s/initialize)]
      (should-be-nil (spec/explain-data ::s/stars stars)))))
