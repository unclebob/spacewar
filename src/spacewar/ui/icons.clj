(ns spacewar.ui.icons
  (:require [quil.core :as q]
            [spacewar.util :refer :all]
            [spacewar.ui.config :refer :all]
            [spacewar.geometry :refer :all]
            [spacewar.vector :as vector]))

(defn prepare-to-draw-bases []
  (q/no-fill)
  (apply q/stroke base-color)
  (q/stroke-weight 2)
  (q/ellipse-mode :center))

(defn draw-base []
  (q/ellipse 0 0 12 12)
  (q/ellipse 0 0 20 20)
  (q/line 0 -6 0 6)
  (q/line -6 0 6 0))