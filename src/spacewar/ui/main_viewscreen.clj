(ns spacewar.ui.main-viewscreen
  (:require [quil.core :as q]
            [spacewar.ui.protocols :as p]))

(deftype frame [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h]} state]
      (q/stroke 0 0 255)
      (q/stroke-weight 5)
      (q/rect x y w h 5)))
  (setup [_] (frame. state))
  (update-state [_] (frame. state)))

(deftype complex [state]
  p/Drawable
  (draw [_]
    (p/draw (:frame state)))

  (setup [_]
    (let [{:keys [x y h w]} state
          frame (->frame {:x x :y y :h h :w w})
          new-state (assoc state :frame frame)]
      (complex. new-state)))

  (update-state [_] (complex. state)))




