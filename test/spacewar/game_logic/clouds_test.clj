(ns spacewar.game-logic.clouds-test
  (:require [midje.sweet :refer [fact
                                 roughly]]
            [spacewar.game-logic.config :refer [cloud-decay-rate
                                                dilithium-harvest-range
                                                dilithium-harvest-rate
                                                ship-dilithium]]
            [spacewar.game-logic.clouds :refer [make-cloud
                                                valid-cloud?
                                                update-clouds-age
                                                harvest-dilithium
                                                update-dilithium-harvest]]
            [spacewar.game-logic.test-mother :as mom]))

(fact
  "constructor"
  (make-cloud) => valid-cloud?)

(fact
  "clouds age"
  (let [world (mom/make-world)
        cloud (make-cloud 0 0 100)
        world (assoc world :clouds [cloud])
        world (update-clouds-age 2 world)
        cloud (first (:clouds world))]
    (:concentration cloud) => (roughly (* 100 cloud-decay-rate cloud-decay-rate) 1e-8)))

(fact
  "Clouds disappear when concentration goes below 1"
  (let [world (mom/make-world)
        cloud (make-cloud 0 0 1)
        world (assoc world :clouds [cloud])
        world (update-clouds-age 1 world)
        clouds (:clouds world)]
    clouds => empty?))

(fact
  "can't harvest dilithium from rich cloud when out of range"
  (let [world (mom/make-world)
        ship (:ship world)
        ship (assoc ship :dilithium 0)
        cloud (make-cloud 0 (inc dilithium-harvest-range) 300)
        [ship cloud] (harvest-dilithium 10 ship cloud)]
    (:concentration cloud) => 300
    (:dilithium ship) => 0))

(fact
  "can harvest dilithium from rich cloud when in range"
  (let [world (mom/make-world)
        ship (:ship world)
        ship (assoc ship :dilithium 0)
        cloud (make-cloud 0 (dec dilithium-harvest-range) 300)
        [ship cloud] (harvest-dilithium 10 ship cloud)]
    (:concentration cloud) => (- 300 (* 10 dilithium-harvest-rate))
    (:dilithium ship) => (* 10 dilithium-harvest-rate)))

(fact
  "cannot harvest more dilithium than cloud contains"
  (let [world (mom/make-world)
        ship (:ship world)
        ship (assoc ship :dilithium 0)
        cloud (make-cloud 0 (dec dilithium-harvest-range) 1)
        [ship cloud] (harvest-dilithium 1e10 ship cloud)]
    (:concentration cloud) => 0
    (:dilithium ship) => 1))

(fact
  "cannot harvest more dilithium than ship needs"
  (let [world (mom/make-world)
        ship (:ship world)
        ship (assoc ship :dilithium (dec ship-dilithium))
        cloud (make-cloud 0 (dec dilithium-harvest-range) 100)
        [ship cloud] (harvest-dilithium 1e10 ship cloud)]
    (:concentration cloud) => 99
    (:dilithium ship) => ship-dilithium))

(fact
  "can harvest from more than one cloud"
  (let [world (mom/make-world)
        ship (:ship world)
        ship (assoc ship :dilithium 0)
        cloud1 (make-cloud 0 (dec dilithium-harvest-range) 1)
        cloud2 (make-cloud (dec dilithium-harvest-range) 0 1)
        clouds [cloud1 cloud2]
        world (assoc world :ship ship :clouds clouds)
        world (update-dilithium-harvest 1e10 world)
        ship (:ship world)
        [cloud1 cloud2] (:clouds world)]
    (:dilithium ship) => 2
    (:concentration cloud1) => 0
    (:concentration cloud2) => 0))




