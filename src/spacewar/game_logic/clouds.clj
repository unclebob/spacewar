(ns spacewar.game-logic.clouds
  (:require [clojure.spec.alpha :as s]
            [spacewar.game-logic.config :refer :all]))

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

(defn update-clouds [ms world]
  (let [clouds (:clouds world)
        decay (Math/pow cloud-decay-rate ms)
        clouds (map #(update % :concentration * decay) clouds)
        clouds (filter #(> (:concentration %) 1) clouds)
        clouds (doall clouds)]
    (assoc world :clouds clouds)))
