(ns spacewar.game-logic.spec-mother-spec
  (:require [speclj.core :refer [describe it should=]]
            [spacewar.game-logic.spec-mother :refer [make-world
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

(describe "make-world"
  (it "creates a valid world"
    (should= true (valid-world? (make-world)))))

(describe "make-ship"
  (it "creates a valid ship"
    (should= true (valid-ship? (make-ship)))))

(describe "make-klingon"
  (it "creates a valid klingon"
    (should= true (valid-klingon? (make-klingon)))))

(describe "make-shot"
  (it "creates a valid shot"
    (should= true (valid-shot? (make-shot)))))

(describe "make-star"
  (it "creates a valid star"
    (should= true (valid-star? (make-star)))))

(describe "make-base"
  (it "creates a valid base with default parameters"
    (should= true (valid-base? (make-base))))
  (it "creates a valid base with specific parameters"
    (should= true (valid-base? (make-base 1 1 :dilithium-factory 10 10)))))

(describe "make-romulan"
  (it "creates a valid romulan with default parameters"
    (should= true (valid-romulan? (make-romulan))))
  (it "creates a valid romulan with specific coordinates"
    (should= true (valid-romulan? (make-romulan 99 99)))))

(describe "set-pos"
  (it "sets position coordinates correctly"
    (let [object (make-klingon)
          new-obj (set-pos object [1 2])]
      (should= 1 (:x new-obj))
      (should= 2 (:y new-obj)))))

(describe "set-ship"
  (it "sets ship in world correctly"
    (let [ship (make-ship)
          world (make-world)
          new-world (set-ship world ship)]
      (should= ship (:ship new-world)))))

(describe "set-klingons"
  (it "sets klingons in world correctly"
    (let [world (make-world)
          klingons [(make-klingon)]
          new-world (set-klingons world klingons)]
      (should= klingons (:klingons new-world)))))