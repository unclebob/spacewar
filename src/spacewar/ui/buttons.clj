(ns spacewar.ui.buttons
  (:require [quil.core :as q]
            [spacewar.ui.protocols :as p]))

(deftype button [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h name color mouse-in left-down]} state]
      (q/stroke-weight 2)
      (if mouse-in
        (q/stroke 255 255 255)
        (apply q/stroke color))
      (if left-down
        (q/fill 255 255 255)
        (apply q/fill color))
      (q/rect x y w h h)
      (q/text-align :right :bottom)
      (q/text-font (:lcars (q/state :fonts)) 18)
      (q/fill 0 0 0)
      (q/text name (+ x w -10) (+ y h))))

  (setup [this] this)
  (update-state [_]
    (let [{:keys [x y w h]} state
          mx (q/mouse-x)
          my (q/mouse-y)
          mouse-in (and (>= mx x) (< mx (+ x w)) (>= my y) (< my (+ y h)))
          left-down (and mouse-in (q/mouse-pressed?) (= :left (q/mouse-button)))]
      (button.
        (assoc state :mouse-in mouse-in
                     :left-down left-down)))))
