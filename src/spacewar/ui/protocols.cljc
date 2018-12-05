(ns spacewar.ui.protocols
  #?(:cljs (:refer-clojure :exclude [clone]))
  (:require [clojure.spec.alpha :as s]))

;update-state returns [new-drawable [events]]
(defprotocol Drawable
  (draw [this])
  (setup [this])
  (update-state [this world])
  (get-state [this])
  (clone [this state]))


(s/def ::elements (s/coll-of keyword?))
(s/def ::drawable-state (s/keys :opt-un [::elements]))
(s/def ::event keyword?)
(s/def ::event-map (s/keys :req-un [::event]))
(s/def ::updated-elements-and-events (s/tuple ::drawable-state (s/coll-of ::event-map)))
(s/def ::world map?)
(s/def ::game-state (s/keys :req-un [::world]))

(defn update-elements [container-state world]
  ;{:pre [
  ;       (s/valid? ::drawable-state container-state)
  ;       ]
  ; :post [
  ;        (s/valid? ::updated-elements-and-events %)
  ;        ]}
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
                [updated-drawable events] (update-state element world)]
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

(defn change-element [container element key value]
  (let [drawable-element (element container)
        element-state (get-state drawable-element)]
    (assoc container
      element
      (clone drawable-element (assoc element-state key value)))))

(defn change-elements [container changes]
  (loop [container container changes changes]
    (if (empty? changes)
      container
      (let [[element key value] (first changes)]
        (recur (change-element container element key value)
               (rest changes))))))