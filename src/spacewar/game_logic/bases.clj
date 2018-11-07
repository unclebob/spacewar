(ns spacewar.game-logic.bases
  (:require [clojure.spec.alpha :as s]
            [spacewar.geometry :refer :all]
            [spacewar.game-logic.config :refer :all]))

(s/def ::x number?)
(s/def ::y number?)
(s/def ::type #{:antimatter-factory :dilithium-factory :weapon-factory})
(s/def ::antimatter number?)
(s/def ::dilithium number?)
(s/def ::kinetics int?)
(s/def ::torpedos int?)
(s/def ::age number?)

(s/def ::base (s/keys :req-un [::x ::y ::type ::antimatter ::dilithium ::age]
                      :opt-un [::kinetics ::torpedos]))
(s/def ::bases (s/coll-of ::base))

(defn- random-base-type []
  (nth [:antimatter-factory :weapon-factory :dilithium-factory]
       (rand-int 3)))

(defn make-base [[x y] type]
  {:x x
   :y y
   :age 0
   :type type
   :antimatter 0
   :dilithium 0
   :torpedos 0
   :kinetics 0})

(defn make-random-base []
  (let [x (int (rand known-space-x))
        y (int (rand known-space-y))]
    (make-base [x y] (random-base-type))))

(defn initialize []
  (repeatedly number-of-bases make-random-base))

(defn- age-base [ms base]
  (let [age (:age base)
        age (min base-maturity-age (+ age ms))]
    (assoc base :age age)))

(defn age-bases [ms bases]
  (map #(age-base ms %) bases))

(defn- manufacture [base ms commodity rate maximum]
  (let [inventory (commodity base)
        deficit (max 0 (- maximum inventory))
        production (* ms rate)
        increase (min deficit production)]
    (update base commodity + increase)))

(defn- update-base-manufacturing [ms base]
  (if (>= (:age base) base-maturity-age)
    (condp = (:type base)
      :antimatter-factory (manufacture base ms :antimatter antimatter-factory-production-rate base-antimatter-maximum)
      :dilithium-factory (manufacture base ms :dilithium dilithium-factory-production-rate base-dilithium-maximum)
      :weapon-factory (-> base
                          (manufacture ms :torpedos weapon-factory-torpedo-production-rate base-torpedos-maximum)
                          (manufacture ms :kinetics weapon-factory-kinetic-production-rate base-kinetics-maximum)))
    base))

(defn update-bases-manufacturing [ms bases]
  (map #(update-base-manufacturing ms %) bases)
  )

(defn update-transport-readiness-for [ms base]
  (let [readiness (:transport-readiness base)
        deficit (- transport-ready readiness)
        adjustment (min deficit ms)]
    (update base :transport-readiness + adjustment)))

(defn- update-transport-readiness [ms bases]
  (map #(update-transport-readiness-for ms %) bases))

(defn- transportable-target? [source-base target-base]
  (let [dist (distance [(:x source-base) (:y source-base)]
                       [(:x target-base) (:y target-base)])]
    (and (< dist trade-route-limit) (> dist 0))))

(defn find-transport-targets-for [base bases]
  (filter #(transportable-target? base %) bases))

(defn transport-ready? [base]
  (= (:transport-readiness base) transport-ready))

(defn should-transport-antimatter? [source dest]
  (let [source-type (:type source)
        dest-type (:type dest)
        source-antimatter (:antimatter source)
        dest-antimatter (:antimatter dest)]
    (or
      (and (= source-type :antimatter-factory)
           (= dest-type :weapon-factory)
           (>= source-antimatter antimatter-cargo-size)
           (< dest-antimatter weapon-factory-antimatter-reserve)))))


  (defn update-bases [ms world]
  (let [bases (:bases world)
        bases (->> bases
                   (age-bases ms)
                   (update-bases-manufacturing ms)
                   (update-transport-readiness ms))]
    (assoc world :bases bases)))



