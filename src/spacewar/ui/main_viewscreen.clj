(ns spacewar.ui.main-viewscreen
  (:require [quil.core :as q]
            [spacewar.ui.protocols :as p]))


(deftype frame [state]
  p/Drawable
  (draw [this]
    (let [{:keys [x y w h]} state]
      (q/background 200 200 200)
      (q/stroke 0 0 255)
      (q/stroke-weight 5)
      (q/rect x y w h 5)))
  (setup [this])
  (update-state [this] this))

