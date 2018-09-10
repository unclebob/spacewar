(ns spacewar.ui.protocols)

(defprotocol Drawable
  (draw [this])
  (setup [this])
  (update-state [this])
  (get-state [this]))

(defn update-elements [state]
  (apply assoc state
         (flatten
           (for [e (:elements state)]
             [e (update-state (e state))]))))

(defn draw-elements [state]
  (doseq [e (:elements state)]
    (draw (e state))))
