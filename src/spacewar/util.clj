(ns spacewar.util)

(defn get-named-map [key id maps]
  (loop [maps maps]
    (if (empty? maps)
      nil
      (let [map (first maps)]
        (if (= id (key map))
          map
          (recur (rest maps)))))))

(defn get-event [event-id events]
  (get-named-map :event event-id events))

(defn handle-event [event handler [events state :as input]]
  (if-let [e (get-event event events)]
    (let [new-state (handler e state)]
      [events new-state])
    input))
