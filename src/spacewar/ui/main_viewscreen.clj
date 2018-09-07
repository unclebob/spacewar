(ns spacewar.ui.main-viewscreen
  (:require [quil.core :as q]
            [spacewar.ui.protocols :as p]))

(deftype frame [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h]} state]
      (q/stroke 0 0 255)
      (q/stroke-weight 5)
      (q/fill 0 0 0)
      (q/rect x y w h 5)))
  (setup [_] (frame. state))
  (update-state [_] (frame. state)))

(deftype bottom-lights [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h]} state]
      (q/fill 150 150 150)
      (q/no-stroke)
      (q/rect x y w h)))

  (setup [_] (bottom-lights. state))

  (update-state [_] (bottom-lights. state)))

(deftype side-lights [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y h w]} state]
      (q/no-stroke)
      (q/fill 200 50 50)
      (q/rect x y w h)))

  (setup [_] (side-lights. state))

  (update-state [_] (side-lights. state)))

(deftype complex [state]
  p/Drawable
  (draw [_]
    (p/draw (:frame state))
    (p/draw (:bottom-row state))
    (p/draw (:left-lights state))
    (p/draw (:right-lights state)))

  (setup [_]
    (let [{:keys [x y h w]} state
          left-margin 200
          right-margin 200
          bottom-margin 100
          panel-gap 50
          frame-width (- w left-margin right-margin)
          frame-height (- h bottom-margin)
          frame (->frame {:x (+ x left-margin)
                          :y y
                          :h frame-height
                          :w frame-width})

          bottom-row-width (/ frame-width 2)
          bottom-row-left-offset (/ (- frame-width bottom-row-width) 2)
          bottom-row (->bottom-lights {:x (+ x left-margin bottom-row-left-offset)
                                       :y (+ y (- h bottom-margin) panel-gap)
                                       :h 40
                                       :w bottom-row-width})

          side-panel-height (/ frame-height 2.5)
          side-panel-width 120
          side-panel-y (+ y (/ frame-height 5))
          left-lights (->side-lights {:x (- (+ x left-margin) panel-gap side-panel-width)
                                      :y side-panel-y
                                      :h side-panel-height
                                      :w side-panel-width})
          right-lights (->side-lights {:x (+ x left-margin frame-width panel-gap)
                                       :y side-panel-y
                                       :w side-panel-width
                                       :h side-panel-height})
          new-state (assoc state :frame frame
                                 :bottom-row bottom-row
                                 :left-lights left-lights
                                 :right-lights right-lights)]
      (complex. new-state)))

  (update-state [_] (complex. state)))




