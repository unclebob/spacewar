(ns spacewar.game-logic.bases
  (:require [clojure.spec.alpha :as s]
            [spacewar.geometry :refer :all]
            [spacewar.vector :as vector]
            [spacewar.game-logic.config :refer :all]
            [clojure.math.combinatorics :as combo]))

(s/def ::x number?)
(s/def ::y number?)
(s/def ::type #{:antimatter-factory :dilithium-factory :weapon-factory})
(s/def ::antimatter number?)
(s/def ::dilithium number?)
(s/def ::kinetics int?)
(s/def ::torpedos int?)
(s/def ::age number?)
(s/def ::transport-readiness number?)

(s/def ::base (s/keys :req-un [::x ::y ::type ::antimatter ::dilithium ::age ::transport-readiness]
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
   :kinetics 0
   :transport-readiness 0})

(s/def ::velocity (s/tuple number? number?))
(s/def ::commodity #{:antimatter :dilithium})
(s/def ::amount number?)
(s/def ::destination (s/tuple number? number?))

(s/def ::transport (s/keys :req-un [::x ::y ::velocity ::commodity ::amount ::destination]))
(s/def ::transports (s/coll-of ::transport))

(defn make-transport [commodity amount destination]
  {:x 0
   :y 0
   :velocity [0 0]
   :commodity commodity
   :amount amount
   :destination destination})

(defn make-random-base []
  (let [x (int (rand known-space-x))
        y (int (rand known-space-y))]
    (make-base [x y] (random-base-type))))

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
    (and (< dist transport-range) (> dist 0))))

(defn find-transport-targets-for [base bases]
  (filter #(transportable-target? base %) bases))

(defn transport-ready? [base]
  (= (:transport-readiness base) transport-ready))

(defn- sufficient-antimatter [type]
  (condp = type
    :antimatter-factory antimatter-factory-sufficient-antimatter
    :dilithium-factory dilithium-factory-sufficient-antimatter
    :weapon-factory weapon-factory-sufficient-antimatter))

(defn- antimatter-reserve [type]
  (condp = type
    :antimatter-factory antimatter-factory-antimatter-reserve
    :dilithium-factory dilithium-factory-antimatter-reserve
    :weapon-factory weapon-factory-antimatter-reserve))

(defn- sufficient-dilithium [type]
  (condp = type
    :antimatter-factory antimatter-factory-sufficient-dilithium
    :dilithium-factory dilithium-factory-sufficient-dilithium
    :weapon-factory weapon-factory-sufficient-dilithium))

(defn- dilithium-reserve [type]
  (condp = type
    :antimatter-factory antimatter-factory-dilithium-reserve
    :dilithium-factory dilithium-factory-dilithium-reserve
    :weapon-factory weapon-factory-dilithium-reserve))

(defn- get-promised-commodity [commodity dest transports]
  (let [transports (filter #(= commodity (:commodity %)) transports)
        transports (filter #(= [(:x dest) (:y dest)] (:destination %)) transports)]
    (reduce + (map :amount transports))))

(defn should-transport-antimatter? [source dest transports]
  (let [source-type (:type source)
        dest-type (:type dest)
        source-antimatter (:antimatter source)
        dest-antimatter (:antimatter dest)
        promised-antimatter (get-promised-commodity :antimatter dest transports)]
    (and
      (< (+ promised-antimatter dest-antimatter) (sufficient-antimatter dest-type))
      (>= source-antimatter (+ antimatter-cargo-size (antimatter-reserve source-type))))))

(defn should-transport-dilithium? [source dest transports]
  (let [source-type (:type source)
        dest-type (:type dest)
        source-dilithium (:dilithium source)
        dest-dilithium (:dilithium dest)
        promised-dilithium (get-promised-commodity :dilithium dest transports)]
    (and
      (< (+ promised-dilithium dest-dilithium) (sufficient-dilithium dest-type))
      (>= source-dilithium (+ dilithium-cargo-size (dilithium-reserve source-type))))))

(defn- cargo-size [commodity]
  (condp = commodity
    :dilithium dilithium-cargo-size
    :antimatter antimatter-cargo-size))

(defn random-transport-velocity-magnitude []
  (* transport-velocity (+ 0.8 (rand 0.2))))

(defn- launch-transport [commodity source dest]
  (let [dest-pos [(:x dest) (:y dest)]
        transport {:x (:x source)
                   :y (:y source)
                   :destination dest-pos
                   :commodity commodity
                   :amount (cargo-size commodity)}
        angle (angle-degrees [(:x source) (:y source)] dest-pos)
        radians (->radians angle)
        v-magnitude (random-transport-velocity-magnitude)
        velocity (vector/from-angular v-magnitude radians)
        transport (assoc transport :velocity velocity)]
    transport))

(defn- launch-new-transports [[source dest] transports]
  (let [should-antimatter? (should-transport-antimatter? source dest transports)
        should-dilithium? (should-transport-dilithium? source dest transports)
        source-ready? (transport-ready? source)
        dist (distance [(:x source) (:y source)]
                       [(:x dest) (:y dest)])
        in-range? (<= dist transport-range)
        new-transports []
        new-transports (if (and in-range? source-ready? should-antimatter?)
                         (conj new-transports (launch-transport :antimatter source dest))
                         new-transports)
        new-transports (if (and in-range? source-ready? should-dilithium?)
                         (conj new-transports (launch-transport :dilithium source dest))
                         new-transports)]
    new-transports)
  )

(defn- transport-launched-from [base transport]
  (and (= (:x base) (:x transport))
       (= (:y base) (:y transport))))

(defn deduct-transported-cargo-from-bases [transports bases]
  (loop [bases bases transports transports deducted-bases []]
    (if (empty? bases)
      deducted-bases
      (let [base (first bases)
            launched-from (filter #(transport-launched-from base %) transports)]
            (if (empty? launched-from)
              (recur (rest bases) transports (conj deducted-bases base))
              (let [transport (first launched-from)
                    base (update base (:commodity transport) - (:amount transport))]
                (recur (rest bases) transports (conj deducted-bases base))))))))


(defn check-new-transports [world]
  (let [{:keys [bases transports]} world
        pairs (combo/combinations bases 2)
        pairs (concat pairs (map reverse pairs))
        new-transports (flatten (map #(launch-new-transports % transports) pairs))
        bases (deduct-transported-cargo-from-bases new-transports bases)
        transports (concat transports new-transports)
        world (assoc world :transports transports
                           :bases bases)]
    world))

(defn check-new-transport-time [world]
  (let [{:keys [update-time transport-check-time]} world
        check-time? (>= update-time (+ transport-check-time transport-check-period))
        world (if check-time? (update world :transport-check-time + transport-check-period)
                              world)
        world (if check-time? (check-new-transports world)
                              world)]
    world))

(defn- move-transport [ms transport]
  (let [{:keys [x y velocity]} transport
        displacement (vector/scale ms velocity)
        [x y] (vector/add displacement [x y])
        transport (assoc transport :x x :y y)]
    transport))

(defn update-transports [ms world]
  (let [transports (:transports world)
        transports (map #(move-transport ms %) transports)]
    (assoc world :transports transports)))

(defn update-bases [ms world]
  (let [bases (:bases world)
        bases (->> bases
                   (age-bases ms)
                   (update-bases-manufacturing ms)
                   (update-transport-readiness ms))
        world (assoc world :bases bases)
        world (->> world
                   (update-transports ms)
                   (check-new-transport-time))]
    world))



