(ns spacewar.ui.control-panels
  (:require [quil.core :as q]
              [spacewar.ui.protocols :as p]))

(deftype button-panel [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h]} state]
      (q/no-stroke)
      (q/fill 255 255 255)
      (q/rect x y w h)))
  (setup [this] this)
  (update-state [this] this))
