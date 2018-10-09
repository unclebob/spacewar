(ns spacewar.game-logic.test-mother
  (:require [midje.sweet :refer :all]
            [clojure.spec.alpha :as spec]
            [spacewar.core :as core]
            [spacewar.game-logic.ship :as ship]
            [spacewar.game-logic.klingons :as klingons]
            [spacewar.game-logic.shots :as shots]))

(def valid-world? (chatty-checker [world]
  (nil? (spec/explain-data ::core/world world))))

(def valid-ship? (chatty-checker [ship]
  (nil? (spec/explain-data ::ship/ship ship))))

(def valid-klingon? (chatty-checker [klingon]
  (nil? (spec/explain-data ::klingons/klingon klingon))))

(def valid-shot? (chatty-checker [shot]
  (nil? (spec/explain-data ::shots/shot shot))))

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
   :antimatter 0
   :core-temp 0
   :dilithium 0
   :strat-scale 1
   })

(defn make-klingon []
  {
   :x 0
   :y 0
   :shields 0
   :antimatter 0
   :kinetics 0
   :kinetic-charge 0
   })

(defn make-world []
  {:explosions []
   :ship (make-ship)
   :klingons []
   :stars []
   :bases []
   :update-time 0
   :shots []
   })

(defn set-pos [obj [x y]]
  (assoc obj :x x :y y))

(defn set-ship [world ship]
  (assoc world :ship ship))

(defn set-klingons [world klingons]
  (assoc world :klingons klingons))
