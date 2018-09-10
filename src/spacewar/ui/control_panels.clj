(ns spacewar.ui.control-panels
  (:require [quil.core :as q]
            [spacewar.ui.protocols :as p]))

(deftype button-panel [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h]} state
          label-width 50
          a [x y]
          b [(+ x w (- 50)) y]
          c [(+ x w) (+ y 50)]
          d [(+ x w) (+ y h)]
          e [(+ x w (- 20)) (+ y h)]
          f [(+ x w (- 20)) (+ y 50 20)]
          g [(+ x w (- 20) (- 20)) (+ y 50)]
          h [x (+ y 50)]
          c1 [(+ x w) y]
          c2 [(+ x w (- 20)) (+ y 50)]]
      (q/no-stroke)
      (q/fill 150 150 255)
      (q/begin-shape)
      (apply q/vertex a)
      (apply q/vertex b)
      (apply q/quadratic-vertex (concat c1 c))
      (apply q/vertex d)
      (apply q/vertex e)
      (apply q/vertex f)
      (apply q/quadratic-vertex (concat c2 g))
      (apply q/vertex h)
      (apply q/vertex a)
      (q/end-shape)
      ))
  (setup [this] this)
  (update-state [this] this))
