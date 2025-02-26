(ns spacewar.game-logic.clouds-spec
  (:require
    [spacewar.game-logic.clouds :as clouds]
    [spacewar.game-logic.config :as glc]
    [spacewar.game-logic.spec-mother :as mom]
    [spacewar.spec-utils :as ut]
    [speclj.core :refer :all]))

(describe "cloud constructor"
  (it "creates a valid cloud"
    (should= true (clouds/valid-cloud? (clouds/make-cloud)))))

(describe "cloud behavior"
  (it "ages clouds"
    (let [world (mom/make-world)
          cloud (clouds/make-cloud 0 0 100)
          world (assoc world :clouds [cloud])
          world (clouds/update-clouds-age 2 world)
          cloud (first (:clouds world))]
      (should (ut/roughly= (* 100 glc/cloud-decay-rate glc/cloud-decay-rate)
                          (:concentration cloud)
                          1e-8))))

  (it "removes clouds when concentration goes below 1"
    (let [world (mom/make-world)
          cloud (clouds/make-cloud 0 0 1)
          world (assoc world :clouds [cloud])
          world (clouds/update-clouds-age 1 world)
          clouds (:clouds world)]
      (should= [] clouds)))
  )

(describe "dilithium harvesting"
  (it "can't harvest dilithium from rich cloud when out of range"
    (let [world (mom/make-world)
          ship (:ship world)
          ship (assoc ship :dilithium 0)
          cloud (clouds/make-cloud 0 (inc glc/dilithium-harvest-range) 300)
          [ship cloud] (clouds/harvest-dilithium 10 ship cloud)]
      (should= 300 (:concentration cloud))
      (should= 0 (:dilithium ship))))

  (it "can harvest dilithium from rich cloud when in range"
    (let [world (mom/make-world)
          ship (:ship world)
          ship (assoc ship :dilithium 0)
          cloud (clouds/make-cloud 0 (dec glc/dilithium-harvest-range) 300)
          [ship cloud] (clouds/harvest-dilithium 10 ship cloud)]
      (should= (- 300 (* 10 glc/dilithium-harvest-rate)) (:concentration cloud))
      (should= (* 10 glc/dilithium-harvest-rate) (:dilithium ship))))

  (it "cannot harvest more dilithium than cloud contains"
    (let [world (mom/make-world)
          ship (:ship world)
          ship (assoc ship :dilithium 0)
          cloud (clouds/make-cloud 0 (dec glc/dilithium-harvest-range) 1)
          [ship cloud] (clouds/harvest-dilithium 1e10 ship cloud)]
      (should= 0 (:concentration cloud))
      (should= 1 (:dilithium ship))))

  (it "cannot harvest more dilithium than ship needs"
    (let [world (mom/make-world)
          ship (:ship world)
          ship (assoc ship :dilithium (dec glc/ship-dilithium))
          cloud (clouds/make-cloud 0 (dec glc/dilithium-harvest-range) 100)
          [ship cloud] (clouds/harvest-dilithium 1e10 ship cloud)]
      (should= 99 (:concentration cloud))
      (should= glc/ship-dilithium (:dilithium ship))))

  (it "can harvest from more than one cloud"
    (let [world (mom/make-world)
          ship (:ship world)
          ship (assoc ship :dilithium 0)
          cloud1 (clouds/make-cloud 0 (dec glc/dilithium-harvest-range) 1)
          cloud2 (clouds/make-cloud (dec glc/dilithium-harvest-range) 0 1)
          clouds [cloud1 cloud2]
          world (assoc world :ship ship :clouds clouds)
          world (clouds/update-dilithium-harvest 1e10 world)
          ship (:ship world)
          [cloud1 cloud2] (:clouds world)]
      (should= 2 (:dilithium ship))
      (should= 0 (:concentration cloud1))
      (should= 0 (:concentration cloud2)))))