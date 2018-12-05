(ns spacewar.ui.widgets.lcars
  (:require [quil.core :as q #?@(:cljs [:include-macros true])]
            [spacewar.ui.config :as uic]))

(defn- lcars-points [state]
  (let [{:keys [x y w h]} state
        inverted (:inverted state false)]
    (if inverted
      {:a [(+ x w) y]
       :b [(+ x uic/banner-width) y]
       :c [x (+ y uic/banner-width)]
       :d [x (+ y h)]
       :e [(+ x uic/stringer-width) (+ y h)]
       :f [(+ x uic/stringer-width) (+ y uic/banner-width uic/stringer-width)]
       :g [(+ x uic/stringer-width uic/stringer-width) (+ y uic/banner-width)]
       :h [(+ x w) (+ y uic/banner-width)]
       :c1 [x y]
       :c2 [(+ x uic/stringer-width) (+ y uic/banner-width)]
       :label-position [(+ x 10 uic/stringer-width) (+ y 10)]}
      {:a [x y]
       :b [(+ x w (- uic/banner-width)) y]
       :c [(+ x w) (+ y uic/banner-width)]
       :d [(+ x w) (+ y h)]
       :e [(+ x w (- uic/stringer-width)) (+ y h)]
       :f [(+ x w (- uic/stringer-width)) (+ y uic/banner-width uic/stringer-width)]
       :g [(+ x w (- uic/stringer-width) (- uic/stringer-width)) (+ y uic/banner-width)]
       :h [x (+ y uic/banner-width)]
       :c1 [(+ x w) y]
       :c2 [(+ x w (- uic/stringer-width)) (+ y uic/banner-width)]
       :label-position [(+ x 10) (+ y 10)]})))

(defn quadratic-vertex [x1 y1 cx cy x3 y3]
  (q/bezier-vertex
    (+ x1 (* (/ 2 3) (- cx x1)))
    (+ y1 (* (/ 2 3) (- cy y1)))
    (+ x3 (* (/ 2 3) (- cx x3)))
    (+ y3 (* (/ 2 3) (- cy y3)))
    x3
    y3))

(defn draw-banner [state]
  (let [color (:color state)
        {:keys [a b c d e f g h c1 c2 label-position]} (lcars-points state)]
    (q/no-stroke)
    (apply q/fill color)
    (q/begin-shape)
    (apply q/vertex a)
    (apply q/vertex b)
    #?(:clj  (apply q/quadratic-vertex (concat c1 c))
       :cljs (apply quadratic-vertex (concat b c1 c)))
    (apply q/vertex d)
    (apply q/vertex e)
    (apply q/vertex f)
    #?(:clj  (apply q/quadratic-vertex (concat c2 g))
       :cljs (apply quadratic-vertex (concat f c2 g)))
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
    (q/rect x (+ y h (- uic/banner-width)) w uic/banner-width)
    (q/fill 0 0 0)
    (q/text-size 24)
    (q/text-font (:lcars (q/state :fonts)))
    (q/text-align :center :center)
    (q/text name (+ x (/ w 2)) (+ y h (/ uic/banner-width -2)))
    )
  )