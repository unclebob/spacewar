(ns spacewar.game-logic.bases
  (:require [clojure.spec.alpha :as s]
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
