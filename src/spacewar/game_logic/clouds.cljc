(ns spacewar.game-logic.clouds
  (:require [clojure.spec.alpha :as s]
            [spacewar.geometry :as geo]
            [spacewar.game-logic.config :as glc]))

(s/def ::x number?)
(s/def ::y number?)
(s/def ::concentration number?)

(s/def ::cloud (s/keys :req-un [::x ::y ::concentration]))
(s/def ::clouds (s/coll-of ::cloud))

(defn valid-cloud? [cloud]
  (let [valid (s/valid? ::cloud cloud)]
    (when (not valid)
      (println (s/explain-str ::cloud cloud)))
    valid))

(defn make-cloud
  ([]
   (make-cloud 0 0 0))
  ([x y concentration]
  {:x x
   :y y
   :concentration concentration}))

(defn harvest-dilithium [ms ship cloud]
  (let [ship-pos [(:x ship) (:y ship)]
        cloud-pos [(:x cloud) (:y cloud)]]
    (if (< (geo/distance ship-pos cloud-pos) glc/dilithium-harvest-range)
      (let [max-harvest (* ms glc/dilithium-harvest-rate)
            need (- glc/ship-dilithium (:dilithium ship))
            cloud-content (:concentration cloud)
            harvest (min max-harvest cloud-content need)
            ship (update ship :dilithium + harvest)
            cloud (update cloud :concentration - harvest)]
        [ship cloud])
      [ship cloud])))

(defn update-dilithium-harvest [ms world]
  (let [{:keys [clouds ship]} world]
    (loop [clouds clouds ship ship harvested-clouds []]
      (if (empty? clouds)
        (assoc world :ship ship :clouds harvested-clouds)
        (let [[ship cloud] (harvest-dilithium ms ship (first clouds))]
          (recur (rest clouds) ship (conj harvested-clouds cloud)))))))

(defn update-clouds-age [ms world]
  (let [clouds (:clouds world)
        decay (Math/pow glc/cloud-decay-rate ms)
        clouds (map #(update % :concentration * decay) clouds)
        clouds (filter #(> (:concentration %) 1) clouds)
        clouds (doall clouds)]
    (assoc world :clouds clouds)))

(defn update-clouds [ms world]
  (->> world
       (update-clouds-age ms)
       (update-dilithium-harvest ms)))
