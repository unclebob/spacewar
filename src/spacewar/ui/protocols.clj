(ns spacewar.ui.protocols)

(defprotocol Drawable
  (draw [this])
  (setup [this])
  (update-state [this]) ; returns [new-drawable [events]]
  (get-state [this]))

(defn update-elements [container-state]
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
                [updated-drawable events] (update-state element)]
            (recur (rest elements)
                   (conj key-vals [element-tag updated-drawable])
                   (conj cum-events events))))))))

(defn draw-elements [state]
  (doseq [e (:elements state)]
    (draw (e state))))

(defn update-drawable [new-drawable]
  [new-drawable []])
