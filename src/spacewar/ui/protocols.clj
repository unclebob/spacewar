(ns spacewar.ui.protocols
  (:require [clojure.spec.alpha :as s]))

;update-state returns [new-drawable [events]]
(defprotocol Drawable
  (draw [this])
  (setup [this])
  (update-state [this commands])
  (get-state [this]))

(s/def ::elements (s/coll-of keyword?))
(s/def ::drawable-state (s/keys :opt-un [::elements]))
(s/def ::event keyword?)
(s/def ::event-map (s/keys :req-un [::event]))
(s/def ::updated-elements-and-events (s/tuple ::drawable-state (s/coll-of ::event-map)))
(s/def ::command keyword?)
(s/def ::command-map (s/keys :req-un [::command]))
(s/def ::commands (s/coll-of ::command-map))

(defn update-elements [container-state commands]
  {:pre [
         (s/valid? ::drawable-state container-state)
         (s/valid? ::commands commands)
         ]
   :post [
          (s/valid? ::updated-elements-and-events %)
          ]}
  (let [elements (:elements container-state)]
    (if (nil? elements)
      [container-state []]
      (loop [elements elements
             key-vals []
             cum-events []]
        (if (empty? elements)
          [(apply assoc container-state (flatten key-vals))
           (flatten cum-events)]
          (let [element-tag (first elements)
                element (element-tag container-state)
                [updated-drawable events] (update-state element commands)]
            (recur (rest elements)
                   (conj key-vals [element-tag updated-drawable])
                   (conj cum-events events))))))))

(defn draw-elements [state]
  (doseq [e (:elements state)]
    (draw (e state))))

(defn pack-update
  ([new-drawable]
   [new-drawable []])
  ([new-drawable event]
   (if (some? event)
     [new-drawable [event]]
     [new-drawable []])))

(defn get-command [command-id commands]
  nil)