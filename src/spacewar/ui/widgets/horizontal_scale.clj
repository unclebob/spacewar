(ns spacewar.ui.widgets.horizontal-scale
  (:require [quil.core :as q]
              [spacewar.ui.protocols :as p]
              [spacewar.geometry :refer :all]
              [spacewar.ui.config :refer :all]))

(deftype h-scale [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h name min max value color mercury-color]} state
          name-gap 150
          scale-w (- w name-gap)
          scale-x (+ x name-gap)
          range (- max min)
          mercury (* scale-w (/ (- value min) range))]
      (apply q/fill black)
      (q/text-align :left :top)
      (q/text-font (:lcars (q/state :fonts)) 18)
      (q/text name x y)
      (q/no-stroke)
      (apply q/fill color)
      (q/rect-mode :corner)
      (q/rect scale-x y scale-w h h)
      (apply q/fill mercury-color)
      (q/rect (+ scale-x (- scale-w mercury)) (+ 2 y) mercury (- h 4) h)
      ))


  (setup [this] this)

  (update-state [_ global-state]
    (let [[new-state events] (p/update-elements state global-state)]
      (p/pack-update
        (h-scale. new-state)
        events))))
