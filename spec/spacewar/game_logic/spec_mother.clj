(ns spacewar.game-logic.spec-mother
  (:require
    [clojure.spec.alpha :as spec]
    [spacewar.core :as core]
    [spacewar.game-logic.klingons :as klingons]
    [spacewar.game-logic.romulans :as romulans]
    [spacewar.game-logic.shots :as shots]
    [spacewar.game-logic.ship :as ship]
    [spacewar.game-logic.stars :as stars]
    [spacewar.game-logic.bases :as bases]
    [spacewar.game-logic.config :refer [klingon-shields
                                        ship-antimatter
                                        ship-dilithium
                                        ship-kinetics
                                        ship-shields
                                        ship-torpedos]]))

(defn valid-world? [world]
  (let [explanation (spec/explain-data ::core/world world)]
    (if (nil? explanation) true explanation)))

(defn valid-ship? [ship]
    (nil? (spec/explain-data ::ship/ship ship)))

(defn valid-klingon? [klingon]
    (nil? (spec/explain-data ::klingons/klingon klingon)))

(defn valid-shot? [shot]
    (nil? (spec/explain-data ::shots/shot shot)))

(defn valid-star? [star]
    (nil? (spec/explain-data ::stars/star star)))

(defn valid-base? [base]
    (nil? (spec/explain-data ::bases/base base)))

(defn valid-romulan? [romulan]
    (nil? (spec/explain-data ::romulans/romulan romulan)))

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
   :corbomite-device-installed false
   :saved-weapon-settings {:phaser {:number-setting 1
                                       :spread-setting 1}
                              :torpedo {:number-setting 1
                                        :spread-setting 1}
                              :kinetic {:number-setting 1
                                        :spread-setting 1}}
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
   :cruise-state :patrol
   :mission :seek-and-destroy
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
   :game-over-timer 0
   :minutes 0
   :version "version"
   :deaths 0
   :klingons-killed 0
   :romulans-killed 0
   })

(defn make-shot []
  {:x 0
   :y 0
   :bearing 0
   :range 0
   :type :phaser
   :corbomite false})

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
               :corbomite 0
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
