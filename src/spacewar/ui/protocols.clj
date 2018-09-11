(ns spacewar.ui.protocols)

;update-state returns [new-drawable [events]]
(defprotocol Drawable
  (draw [this])
  (setup [this])
  (update-state [this commands])
  (get-state [this]))

(defn update-elements [container-state commands]
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

(defn update-drawable
  ([new-drawable]
   [new-drawable []])
  ([new-drawable event]
   (if (some? event)
     [new-drawable [event]]
     [new-drawable []])))
