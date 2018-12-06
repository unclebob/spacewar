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

(defn color-diff [[r1 g1 b1] [r2 g2 b2]]
  [(- r1 r2) (- g1 g2) (- b1 b2)])

(defn color-scale [[r g b] s]
  [(* s r) (* s g) (* s b)])

(defn color-add [[r1 g1 b1] [r2 g2 b2]]
  [(+ r1 r2) (+ g1 g2) (+ b1 b2)])

(defn color-normalize [[r g b]]
  [(mod r 255) (mod g 255) (mod b 255)])

(defn color-shift [color1 color2 factor]
  (let [diff (color-diff color2 color1)
        delta (color-scale diff factor)]
    (color-add color1 delta)))

(defn pos [object]
  [(:x object) (:y object)])