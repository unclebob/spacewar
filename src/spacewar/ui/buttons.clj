(ns spacewar.ui.buttons
  (:require [quil.core :as q]
            [spacewar.ui.protocols :as p]))

(deftype button [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h name color]} state]
      (q/no-stroke)
      (apply q/fill color)
      (q/rect x y w h h)
      (q/text-align :right :bottom)
      (q/text-font (:lcars (q/state :fonts)) 18)
      (q/fill 0 0 0)
      (q/text name (+ x w -10) (+ y h))))

  (setup [this] this)
  (update-state [this] this))
