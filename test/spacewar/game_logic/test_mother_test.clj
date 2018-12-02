(ns spacewar.game-logic.test-mother-test
  (:require [midje.sweet :refer [fact]]
            [spacewar.game-logic.test-mother :refer [make-world
                                                     valid-world?
                                                     make-ship
                                                     valid-ship?
                                                     make-klingon
                                                     valid-klingon?
                                                     make-shot
                                                     valid-shot?
                                                     make-star
                                                     valid-star?
                                                     make-base
                                                     valid-base?
                                                     make-romulan
                                                     valid-romulan?
                                                     set-pos
                                                     set-ship
                                                     set-klingons]]))

(fact "make-world"
      (make-world) => valid-world?)

(fact "make-ship"
      (make-ship) => valid-ship?)

(fact "make-klingon"
      (make-klingon) => valid-klingon?)

(fact "make-shot"
      (make-shot) => valid-shot?)

(fact "make-star"
      (make-star) => valid-star?)

(fact "make-base"
      (make-base) => valid-base?
      (make-base 1 1 :dilithium-factory 10 10) => valid-base?)

(fact "make-romulan"
      (make-romulan) => valid-romulan?
      (make-romulan 99 99) => valid-romulan?
      )

(fact "set-pos"
      (let [object (make-klingon)
            new-obj (set-pos object [1 2])]
        (:x new-obj) => 1
        (:y new-obj) => 2))

(fact "set-ship"
      (let [ship (make-ship)
            world (make-world)
            new-world (set-ship world ship)]
        (:ship new-world) => ship))

(fact "set-klingons"
      (let [world (make-world)
            klingons [(make-klingon)]
            new-world (set-klingons world klingons)]
        (:klingons new-world) => klingons))