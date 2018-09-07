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

(deftype indicator-light [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h]} state]
      (q/stroke 0 0 0)
      (q/stroke-weight 1)
      (q/fill 255 255 0)
      (q/rect x y w h)))

  (setup [_] (indicator-light. state))
  (update-state [_] (indicator-light. state)))

(deftype bottom-lights [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h indicators]} state]
      (q/fill 150 150 150)
      (q/no-stroke)
      (q/rect x y w h)
      (doseq [indicator indicators] (p/draw indicator))))

  (setup [_]
    (let [{:keys [x y w h]} state
          number 14
          gap 20
          indicator-height 10
          indicator-width 20
          spacing (/ (- w gap gap indicator-width) (dec number))
          indicator-y (+ y (/ (- h indicator-height) 2))
          indicators (map #(->indicator-light {:x (+ x gap (* spacing %))
                                               :y indicator-y
                                               :w indicator-width
                                               :h indicator-height})
                          (range 0 number))
          new-state (assoc state :indicators indicators)]
      (bottom-lights. new-state)))

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
          frame (p/setup
                  (->frame {:x (+ x left-margin)
                            :y y
                            :h frame-height
                            :w frame-width}))

          bottom-row-width (/ frame-width 2)
          bottom-row-left-offset (/ (- frame-width bottom-row-width) 2)
          bottom-row (p/setup
                       (->bottom-lights {:x (+ x left-margin bottom-row-left-offset)
                                         :y (+ y (- h bottom-margin) panel-gap)
                                         :h 40
                                         :w bottom-row-width}))

          side-panel-height (/ frame-height 2.5)
          side-panel-width 120
          side-panel-y (+ y (/ frame-height 5))
          left-lights (p/setup
                        (->side-lights {:x (- (+ x left-margin) panel-gap side-panel-width)
                                      :y side-panel-y
                                      :h side-panel-height
                                      :w side-panel-width}))

          right-lights (p/setup
                         (->side-lights {:x (+ x left-margin frame-width panel-gap)
                                       :y side-panel-y
                                       :w side-panel-width
                                       :h side-panel-height}))
          new-state (assoc state :frame frame
                                 :bottom-row bottom-row
                                 :left-lights left-lights
                                 :right-lights right-lights)]
      (complex. new-state)))

  (update-state [_] (complex. state)))




