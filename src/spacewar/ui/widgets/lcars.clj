(ns spacewar.ui.widgets.lcars
  (:require [quil.core :as q]
            [spacewar.geometry :refer :all]
            [spacewar.ui.config :refer :all]))

(defn- lcars-points [state]
  (let [{:keys [x y w h]} state
        inverted (:inverted state false)]
    (if inverted
      {:a [(+ x w) y]
       :b [(+ x banner-width) y]
       :c [x (+ y banner-width)]
       :d [x (+ y h)]
       :e [(+ x stringer-width) (+ y h)]
       :f [(+ x stringer-width) (+ y banner-width stringer-width)]
       :g [(+ x stringer-width stringer-width) (+ y banner-width)]
       :h [(+ x w) (+ y banner-width)]
       :c1 [x y]
       :c2 [(+ x stringer-width) (+ y banner-width)]
       :label-position [(+ x 10 stringer-width) (+ y 10)]}
      {:a [x y]
       :b [(+ x w (- banner-width)) y]
       :c [(+ x w) (+ y banner-width)]
       :d [(+ x w) (+ y h)]
       :e [(+ x w (- stringer-width)) (+ y h)]
       :f [(+ x w (- stringer-width)) (+ y banner-width stringer-width)]
       :g [(+ x w (- stringer-width) (- stringer-width)) (+ y banner-width)]
       :h [x (+ y banner-width)]
       :c1 [(+ x w) y]
       :c2 [(+ x w (- stringer-width)) (+ y banner-width)]
       :label-position [(+ x 10) (+ y 10)]})))

(defn draw-lcars [state]
  (let [color (:color state)
        {:keys [a b c d e f g h c1 c2 label-position]} (lcars-points state)]
    (q/no-stroke)
    (apply q/fill color)
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
    (apply q/text (:name state) label-position)))

(defn draw-bottom-lcars [state]
  (let [{:keys [x y w h name color]} state]
    (q/no-stroke)
    (apply q/fill color)
    (q/rect-mode :corner)
    (q/rect x (+ y h (- banner-width)) w banner-width)
    (q/fill 0 0 0)
    (q/text-size 24)
    (q/text-font (:lcars (q/state :fonts)))
    (q/text-align :center :center)
    (q/text name (+ x (/ w 2)) (+ y h (/ banner-width -2)))
    )
  )