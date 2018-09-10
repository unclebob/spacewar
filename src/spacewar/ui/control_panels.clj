(ns spacewar.ui.control-panels
  (:require [quil.core :as q]
            [spacewar.ui.protocols :as p]
            [spacewar.ui.buttons :as b]))

(def banner-width 50)
(def stringer-width 15)

(defn draw-lcars [state]
  (let [{:keys [x y w h]} state
        a [x y]
        b [(+ x w (- banner-width)) y]
        c [(+ x w) (+ y banner-width)]
        d [(+ x w) (+ y h)]
        e [(+ x w (- stringer-width)) (+ y h)]
        f [(+ x w (- stringer-width)) (+ y banner-width stringer-width)]
        g [(+ x w (- stringer-width) (- stringer-width)) (+ y banner-width)]
        h [x (+ y banner-width)]
        c1 [(+ x w) y]
        c2 [(+ x w (- stringer-width)) (+ y banner-width)]]
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
    (q/fill 0 0 0)
    (q/text-size 24)
    (q/text-font (:lcars (q/state :fonts)))
    (q/text-align :left :top)
    (q/text "SCAN" (+ x 10) (+ y 10))))

(deftype button-panel [state]
  p/Drawable
  (draw [_]
    (let [{:keys [strategic tactical]} state]
      (draw-lcars state)
      (p/draw strategic)
      (p/draw tactical)))

  (setup [_]
    (let [{:keys [x y w]} state]
      (button-panel.
        (assoc state :strategic (p/setup (b/->button
                                           {:x x
                                            :y (+ y banner-width 10)
                                            :w (- w stringer-width 10)
                                            :h 40
                                            :name "STRAT"
                                            :color [200 100 255]}))
                     :tactical (p/setup (b/->button
                                          {:x x
                                           :y (+ y banner-width 10 40 10)
                                           :w (- w stringer-width 10)
                                           :h 40
                                           :name "TACT"
                                           :color [200 100 255]}))
                     :elements [:strategic :tactical]))))

  (update-state [_] (button-panel. (p/update-elements state))))
