(ns spacewar.game-logic.bases
  (:require [clojure.spec.alpha :as s]
            [spacewar.game-logic.config :as glc]
            [spacewar.game-logic.explosions :as explosions]
            [spacewar.ui.messages :as messages]
            [spacewar.geometry :as geo]
            [spacewar.util :as util]
            [spacewar.vector :as vector]))

(s/def ::x number?)
(s/def ::y number?)
(s/def ::type #{:antimatter-factory :dilithium-factory :weapon-factory :corbomite-factory :corbomite-device})
(s/def ::antimatter number?)
(s/def ::dilithium number?)
(s/def ::corbomite number?)
(s/def ::kinetics int?)
(s/def ::torpedos int?)
(s/def ::age number?)
(s/def ::transport-readiness number?)

(s/def ::base (s/keys :req-un [::x ::y ::type ::antimatter ::dilithium ::corbomite ::age ::transport-readiness]
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
   :corbomite 0
   :torpedos 0
   :kinetics 0
   :transport-readiness 0})

(defn make-random-antimatter-factory [star]
  (if (< (rand) (/ 30 glc/number-of-stars))
    (let [base (make-base [(:x star) (:y star)] :antimatter-factory)
          base (assoc base :age glc/base-maturity-age
                           :antimatter (rand (/ glc/base-antimatter-maximum 2)))]
      base)
    nil)
  )

(defn make-random-weapons-factory [star]
  (if (< (rand) (/ 5 glc/number-of-stars))
    (let [base (make-base [(:x star) (:y star)] :weapon-factory)
          base (assoc base :age glc/base-maturity-age
                           :torpedos (rand-int (/ glc/base-torpedos-maximum 2))
                           :kinetics (rand-int (/ glc/base-kinetics-maximum 2)))]
      base)
    nil))

(defn make-random-dilithium-factory [star]
  (if (< (rand) (/ 10 glc/number-of-stars))
    (let [base (make-base [(:x star) (:y star)] :dilithium-factory)
          base (assoc base :age glc/base-maturity-age
                           :antimatter (rand (/ glc/base-antimatter-maximum 2))
                           :dilithium (rand (/ glc/base-dilithium-maximum 2)))]
      base)
    nil))

(defn make-random-base-for-star [star]
  (condp = (:class star)
    :o (make-random-antimatter-factory star)
    :b (make-random-antimatter-factory star)
    :a (make-random-antimatter-factory star)
    :f (make-random-weapons-factory star)
    :g (make-random-weapons-factory star)
    :k (make-random-dilithium-factory star)
    :m (make-random-dilithium-factory star)
    :pulsar nil))

(defn make-random-bases [stars]
  (remove nil?
          (map make-random-base-for-star stars)))

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
  (let [x (int (rand glc/known-space-x))
        y (int (rand glc/known-space-y))]
    (make-base [x y] (random-base-type))))

(defn- age-base [ms base]
  (let [age (:age base)
        age (min glc/base-maturity-age (+ age ms))]
    (assoc base :age age)))

(defn age-bases [ms bases]
  (map #(age-base ms %) bases))

(def antimatter-production
  {:rate glc/antimatter-factory-production-rate
   :maximum glc/base-antimatter-maximum
   :antimatter-cost 0
   :dilithium-cost 0})

(def dilithium-production
  {:rate glc/dilithium-factory-production-rate
   :maximum glc/base-dilithium-maximum
   :antimatter-cost glc/dilithium-factory-dilithium-antimatter-cost
   :dilithium-cost 0})

(def torpedo-production
  {:rate glc/weapon-factory-torpedo-production-rate
   :maximum glc/base-torpedos-maximum
   :antimatter-cost glc/weapon-factory-torpedo-antimatter-cost
   :dilithium-cost glc/weapon-factory-torpedo-dilithium-cost})

(def kinetic-production
  {:rate glc/weapon-factory-kinetic-production-rate
   :maximum glc/base-kinetics-maximum
   :antimatter-cost glc/weapon-factory-kinetic-antimatter-cost
   :dilithium-cost 0})

(def corbomite-production
  {:rate glc/corbomite-factory-production-rate
   :maximum glc/corbomite-maximum
   :antimatter-cost glc/corbomite-factory-antimatter-cost
   :dilithium-cost glc/corbomite-factory-dilithium-cost})

(defn- manufacture [base ms commodity production]
  (let [{:keys [rate maximum antimatter-cost dilithium-cost]} production
        inventory (commodity base)
        deficit (max 0 (- maximum inventory))
        production (* ms rate)
        increase (min deficit production)
        max-for-antimatter (if (zero? antimatter-cost)
                             increase
                             (/ (:antimatter base) antimatter-cost))
        max-for-dilithium (if (zero? dilithium-cost)
                            increase
                            (/ (:dilithium base) dilithium-cost))
        increase (min increase max-for-antimatter max-for-dilithium)
        dilithium-decrease (* increase dilithium-cost)
        antimatter-decrease (* increase antimatter-cost)]
    (-> base
        (update commodity + increase)
        (update :antimatter - antimatter-decrease)
        (update :dilithium - dilithium-decrease))))

(defn- update-base-manufacturing [ms base]
  (if (>= (:age base) glc/base-maturity-age)
    (condp = (:type base)
      :antimatter-factory (manufacture base ms :antimatter antimatter-production)
      :dilithium-factory (manufacture base ms :dilithium dilithium-production)
      :weapon-factory (-> base
                          (manufacture ms :torpedos torpedo-production)
                          (manufacture ms :kinetics kinetic-production))
      :corbomite-factory (manufacture base ms :corbomite corbomite-production)
      :corbomite-device base)
    base))

(defn update-bases-manufacturing [ms bases]
  (let [bases (map #(update-base-manufacturing ms %) bases)]
    bases))

(defn update-transport-readiness-for [ms base]
  (let [readiness (:transport-readiness base)
        deficit (- glc/transport-ready readiness)
        adjustment (min deficit ms)]
    (update base :transport-readiness + adjustment)))

(defn- update-transport-readiness [ms bases]
  (map #(update-transport-readiness-for ms %) bases))


(defn transport-ready? [base]
  (= (:transport-readiness base) glc/transport-ready))

(defn- sufficient-antimatter [type]
  (condp = type
    :antimatter-factory glc/antimatter-factory-sufficient-antimatter
    :dilithium-factory glc/dilithium-factory-sufficient-antimatter
    :weapon-factory glc/weapon-factory-sufficient-antimatter
    :corbomite-factory glc/corbomite-factory-sufficient-antimatter
    :corbomite-device 0))

(defn- antimatter-reserve [type]
  (condp = type
    :antimatter-factory glc/antimatter-factory-antimatter-reserve
    :dilithium-factory glc/dilithium-factory-antimatter-reserve
    :weapon-factory glc/weapon-factory-antimatter-reserve
    :corbomite-factory glc/corbomite-factory-antimatter-reserve
    :corbomite-device 0))

(defn- sufficient-dilithium [type]
  (condp = type
    :antimatter-factory glc/antimatter-factory-sufficient-dilithium
    :dilithium-factory glc/dilithium-factory-sufficient-dilithium
    :weapon-factory glc/weapon-factory-sufficient-dilithium
    :corbomite-factory glc/corbomite-factory-sufficient-dilithium
    :corbomite-device 0))

(defn- dilithium-reserve [type]
  (condp = type
    :antimatter-factory glc/antimatter-factory-dilithium-reserve
    :dilithium-factory glc/dilithium-factory-dilithium-reserve
    :weapon-factory glc/weapon-factory-dilithium-reserve
    :corbomite-factory glc/corbomite-factory-dilithium-reserve
    :corbomite-device 0))

(defn- get-promised-commodity [commodity dest transports]
  (let [transports (filter #(= commodity (:commodity %)) transports)
        transports (filter #(= [(:x dest) (:y dest)] (:destination %)) transports)]
    (reduce + (map :amount transports))))

(defn- sufficient-commodity [commodity base-type]
  (condp = commodity
    :antimatter (sufficient-antimatter base-type)
    :dilithium (sufficient-dilithium base-type)
    nil))

(defn- commodity-cargo-size [commodity]
  (condp = commodity
    :antimatter glc/antimatter-cargo-size
    :dilithium glc/dilithium-cargo-size
    nil))

(defn- commodity-reserve [commodity base-type]
  (condp = commodity
    :antimatter (antimatter-reserve base-type)
    :dilithium (dilithium-reserve base-type)
    nil))

(defn should-transport-commodity? [commodity source dest transports]
  (let [source-type (:type source)
        dest-type (:type dest)
        source-commodity (commodity source)
        dest-commodity (commodity dest)
        promised-commodity (get-promised-commodity commodity dest transports)
        sufficient (sufficient-commodity commodity dest-type)
        cargo-size (commodity-cargo-size commodity)
        reserve (commodity-reserve commodity source-type)
        he-needs-it? (<= (+ promised-commodity dest-commodity) sufficient)
        ill-still-have-more-than-him? (> (- source-commodity cargo-size) dest-commodity)
        ill-still-have-my-reserve? (>= source-commodity (+ cargo-size reserve))]
    (or
      (and
        (not= :corbomite-device dest-type)
        he-needs-it?
        ill-still-have-more-than-him?
        ill-still-have-my-reserve?)
      (and
        (or (= :corbomite-factory dest-type)
            (= :weapon-factory dest-type))
        he-needs-it?
        (> source-commodity cargo-size))
      )))

(defn should-transport-antimatter? [source dest transports]
  (should-transport-commodity? :antimatter source dest transports))

(defn should-transport-dilithium? [source dest transports]
  (should-transport-commodity? :dilithium source dest transports))

(defn- cargo-size [commodity]
  (condp = commodity
    :dilithium glc/dilithium-cargo-size
    :antimatter glc/antimatter-cargo-size))

(defn random-transport-velocity-magnitude []
  (* glc/transport-velocity (+ 0.8 (rand 0.2))))

(defn- launch-transport [commodity source dest]
  (let [dest-pos [(:x dest) (:y dest)]
        transport {:x (:x source)
                   :y (:y source)
                   :destination dest-pos
                   :commodity commodity
                   :amount (cargo-size commodity)}
        angle (geo/angle-degrees (util/pos source) dest-pos)
        radians (geo/->radians angle)
        v-magnitude (random-transport-velocity-magnitude)
        velocity (vector/from-angular v-magnitude radians)
        transport (assoc transport :velocity velocity)]
    transport))

(defn- select-potential-transports [[source dest] transports]
  (let [should-antimatter? (should-transport-antimatter? source dest transports)
        should-dilithium? (should-transport-dilithium? source dest transports)
        source-ready? (transport-ready? source)
        new-transports []
        new-transports (if (and source-ready? should-antimatter?)
                         (conj new-transports (launch-transport :antimatter source dest))
                         new-transports)
        new-transports (if (and source-ready? should-dilithium?)
                         (conj new-transports (launch-transport :dilithium source dest))
                         new-transports)]
    new-transports)
  )

(defn- transport-launched-from [base transport]
  (and (= (:x base) (:x transport))
       (= (:y base) (:y transport))))

(defn launch-transports-from-bases [transports bases]
  (loop [bases bases transports transports deducted-bases []]
    (if (empty? bases)
      deducted-bases
      (let [base (first bases)
            launched-from (filter #(transport-launched-from base %) transports)]
        (if (empty? launched-from)
          (recur (rest bases) transports (conj deducted-bases base))
          (let [transport (first launched-from)
                base (update base (:commodity transport) - (:amount transport))
                base (assoc base :transport-readiness 0)]
            (recur (rest bases) transports (conj deducted-bases base))))))))

(defn- distance-to-dest [transport]
  (geo/distance (util/pos transport) (:destination transport)))

(defn base-from-coordinates [bases [x y]]
  (let [base (first (filter #(and (= x (:x %))
                                  (= y (:y %)))
                            bases))]
    base))

(defn- amount-at-destination [bases transport]
  (let [dest (base-from-coordinates bases (:destination transport))]
    ((:commodity transport) dest)))

(defn- select-best-transports [transports bases]
  (loop [bases bases candidates transports selected []]
    (if (empty? bases)
      selected
      (let [base (first bases)
            transports-from (filter #(transport-launched-from base %) candidates)]
        (if (empty? transports-from)
          (recur (rest bases) candidates selected)
          (let [neediest (first
                           (sort-by #(amount-at-destination bases %)
                                    (shuffle transports-from)))]
            (recur (rest bases) candidates (conj selected neediest))))))))

(defn- blockaded-transport? [transport klingons]
  (let [blockading-klingons (filter #(< (geo/distance (util/pos transport)
                                                      (util/pos %))
                                        glc/klingon-docking-distance)
                                    klingons)
        blockaded? (not (empty? blockading-klingons))]
    blockaded?))

(defn- remove-blockaded-transports [transports klingons]
  (remove #(blockaded-transport? % klingons) transports))

(defn check-new-transports [world]
  (let [{:keys [bases transports klingons transport-routes]} world
        coord-pairs (map vec transport-routes)
        base-pairs (map
                     (fn [coord-pair]
                       (map (fn [coord]
                              (base-from-coordinates bases coord))
                            coord-pair))
                     coord-pairs)
        base-pairs (concat base-pairs (map reverse base-pairs))
        candidate-transports (flatten (map #(select-potential-transports % transports) base-pairs))
        selected-transports (select-best-transports candidate-transports bases)
        selected-transports (remove-blockaded-transports selected-transports klingons)
        bases (launch-transports-from-bases selected-transports bases)
        transports (concat transports selected-transports)
        world (assoc world :transports transports
                           :bases bases)]
    world))

(defn check-new-transport-time [world]
  (let [{:keys [update-time transport-check-time]} world
        check-time? (>= update-time (+ transport-check-time glc/transport-check-period))
        world (if check-time? (update world :transport-check-time + glc/transport-check-period)
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

(defn- delivering? [transport]
  (<= (geo/distance (util/pos transport)
                    (:destination transport))
      glc/transport-delivery-range))

(defn- transport-going-to [base transport]
  (= (:destination transport) (util/pos base)))

(defn- accepting-delivery? [transports base]
  (let [delivery-transports (filter #(transport-going-to base %) transports)]
    (and
      (not= :corbomite-device (:type base))
      (not (empty? delivery-transports)))))

(def max-commodity
  {:antimatter glc/base-antimatter-maximum
   :dilithium glc/base-dilithium-maximum})

(defn receive-transports [world]
  (let [transports (:transports world)
        bases (:bases world)
        grouped-transports (group-by delivering? transports)
        delivering (grouped-transports true)
        in-transit (grouped-transports false)
        grouped-bases (group-by #(accepting-delivery? delivering %) bases)
        accepting (grouped-bases true)
        waiting (grouped-bases false)]
    (loop [accepting accepting delivering delivering adjusted-bases []]
      (if (empty? accepting)
        (let [new-bases (concat waiting adjusted-bases)]
          (assoc world :transports in-transit :bases new-bases))
        (let [base (first accepting)
              transport (first (filter #(transport-going-to base %) delivering))
              commodity (:commodity transport)
              new-total (min
                          (max-commodity commodity)
                          (+ (:amount transport) (get base commodity)))
              base (assoc base commodity new-total)]
          (recur (rest accepting) delivering (conj adjusted-bases base)))))))

(defn remove-routes-to-base [world base]
  (let [coord [(:x base) (:y base)]
        routes (:transport-routes world)
        routes (set (remove #(contains? % coord) routes))]
    (assoc world :transport-routes routes)))


(defn- check-corbomite-base [{:keys [bases explosions] :as world}]
  (let [base-map (group-by #(= :corbomite-factory (:type %)) bases)
        corbomite-base (first (get base-map true))
        other-bases (get base-map false)
        corbomite-incomplete? (or (nil? corbomite-base)
                                  (> glc/corbomite-maximum (:corbomite corbomite-base)))
        bases (if corbomite-incomplete?
                bases
                (conj other-bases (assoc corbomite-base
                                    :type :corbomite-device
                                    :antimatter 0
                                    :dilithium 0
                                    :corbomite 0)))
        explosions (if corbomite-incomplete?
                     explosions
                     (conj explosions (explosions/->explosion :corbomite-device corbomite-base)))
        world (if corbomite-incomplete?
                world
                (remove-routes-to-base world corbomite-base))]
    (when (not corbomite-incomplete?)
      (messages/send-message :corbomite-device))
    (assoc world :bases bases :explosions explosions)))

(defn update-bases [ms world]
  (let [bases (:bases world)
        bases (->> bases
                   (age-bases ms)
                   (update-bases-manufacturing ms)
                   (update-transport-readiness ms)
                   )
        world (assoc world :bases bases)
        world (->> world
                   (update-transports ms)
                   (check-new-transport-time)
                   (receive-transports)
                   (check-corbomite-base))]
    world))



