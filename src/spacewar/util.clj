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
