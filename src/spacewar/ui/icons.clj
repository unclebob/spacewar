(ns spacewar.ui.icons
  (:require [quil.core :as q]
            [spacewar.util :refer :all]
            [spacewar.ui.config :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.geometry :refer :all]
            [spacewar.vector :as vector]))

(defn prepare-to-draw-bases []
  (q/no-fill)
  (apply q/stroke base-color)
  (q/stroke-weight 2)
  (q/ellipse-mode :center))

(defn draw-base-icon []
  (q/ellipse 0 0 12 12)
  (q/ellipse 0 0 20 20)
  (q/line 0 -6 0 6)
  (q/line -6 0 6 0))

(defn draw-klingon-icon []
  (apply q/fill black)
  (apply q/stroke klingon-color)
  (q/stroke-weight 2)
  (q/ellipse-mode :center)
  (q/line 0 0 10 -6)
  (q/line 10 -6 14 -3)
  (q/line 0 0 -10 -6)
  (q/line -10 -6 -14 -3)
  (q/ellipse 0 0 6 6))

(defn draw-klingon-shields [shields]
  (when (< shields klingon-shields)
    (let [pct (/ shields klingon-shields)
          flicker (< (rand 3) pct)
          color [255 (* pct 255) 0 (if flicker (* pct 100) 100)]
          radius (+ 35 (* pct 20))]
      (apply q/fill color)
      (q/ellipse-mode :center)
      (q/no-stroke)
      (q/ellipse 0 0 radius radius))))