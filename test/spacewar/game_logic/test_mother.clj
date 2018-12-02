(ns spacewar.game-logic.test-mother
  (:require [midje.sweet :refer [chatty-checker]]
            [clojure.spec.alpha :as spec]
            [spacewar.game-logic.config :refer [ship-antimatter
                                                ship-dilithium
                                                ship-shields
                                                ship-kinetics
                                                ship-torpedos
                                                klingon-shields]]
            [spacewar.core :as core]
            [spacewar.game-logic.ship :as ship]
            [spacewar.game-logic.klingons :as klingons]
            [spacewar.game-logic.shots :as shots]
            [spacewar.game-logic.stars :as stars]
            [spacewar.game-logic.bases :as bases]
            [spacewar.game-logic.romulans :as romulans]))

(def valid-world?
  (chatty-checker
    [world]
    (nil? (spec/explain-data ::core/world world))))

(def valid-ship?
  (chatty-checker
    [ship]
    (nil? (spec/explain-data ::ship/ship ship))))

(def valid-klingon?
  (chatty-checker
    [klingon]
    (nil? (spec/explain-data ::klingons/klingon klingon))))

(def valid-shot?
  (chatty-checker
    [shot]
    (nil? (spec/explain-data ::shots/shot shot))))

(def valid-star?
  (chatty-checker
    [star]
    (nil? (spec/explain-data ::stars/star star))))

(def valid-base?
  (chatty-checker
    [base]
    (nil? (spec/explain-data ::bases/base base))))

(def valid-romulan?
  (chatty-checker
    [romulan]
    (nil? (spec/explain-data ::romulans/romulan romulan))))

(defn make-ship []
  {
   :x 0
   :y 0
   :warp 0
   :warp-charge 0
   :impulse 0
   :heading 0
   :velocity [0 0]
   :selected-view :front-view
   :selected-weapon :none
   :selected-engine :none
   :target-bearing 0
   :engine-power-setting 0
   :weapon-number-setting 1
   :weapon-spread-setting 0
   :heading-setting 0
   :antimatter ship-antimatter
   :core-temp 0
   :dilithium ship-dilithium
   :shields ship-shields
   :kinetics ship-kinetics
   :torpedos ship-torpedos
   :life-support-damage 0
   :hull-damage 0
   :sensor-damage 0
   :impulse-damage 0
   :warp-damage 0
   :weapons-damage 0
   :strat-scale 1
   :destroyed false
   })

(defn make-klingon []
  {
   :x 0
   :y 0
   :shields klingon-shields
   :antimatter 0
   :kinetics 0
   :torpedos 0
   :weapon-charge 0
   :velocity [0 0]
   :thrust [0 0]
   :battle-state-age 0
   :battle-state :no-battle
   })

(defn make-world []
  {:explosions []
   :ship (make-ship)
   :klingons []
   :stars []
   :bases []
   :transports []
   :clouds []
   :romulans []
   :update-time 0
   :transport-check-time 0
   :ms 0
   :shots []
   :messages []
   :game-over false
   })

(defn make-shot []
  {:x 0
   :y 0
   :bearing 0
   :range 0
   :type :phaser})

(defn make-star
  ([]
   (make-star 0 0 :o))
  ([x y class]
   {:x x :y y :class class}))

(defn make-base
  ([]
   (make-base 0 0 :weapon-factory 0 0 0 0))
  ([x y type antimatter dilithium & weapons]
   (let [base {:x x
               :y y
               :age 0
               :type type
               :antimatter antimatter
               :dilithium dilithium
               :transport-readiness 0}]
     (if (= type :weapon-factory)
       (assoc base :kinetics (first weapons)
                   :torpedos (second weapons))
       base))))

(defn make-romulan
  ([]
   (make-romulan 0 0))
  ([x y]
   {:x x
    :y y
    :age 0
    :state :invisible
    :fire-weapon false}))

(defn set-pos [obj [x y]]
  (assoc obj :x x :y y))

(defn set-ship [world ship]
  (assoc world :ship ship))

(defn set-klingons [world klingons]
  (assoc world :klingons klingons))
