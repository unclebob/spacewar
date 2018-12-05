(ns spacewar.ui.widgets.horizontal-scale
  (:require [quil.core :as q #?@(:cljs [:include-macros true])]
            [spacewar.ui.protocols :as p]
            [spacewar.ui.config :as uic]))

(defn mercury-color [value thresholds]
  (let [max-value (first (last thresholds))
        value (min value max-value)]
    (some #(if (>= (first %) value)
             (second %)
             nil)
          thresholds)))

(deftype h-scale [state]
  p/Drawable
  (get-state [_] state)
  (clone [_ clone-state] (h-scale. clone-state))

  (draw [_]
    (let [{:keys [x y w h name min max value color mercury-colors]} state
          name-gap 150
          scale-w (- w name-gap)
          scale-x (+ x name-gap)
          range (- max min)
          mercury (* scale-w (/ (- value min) range))]
      (apply q/fill uic/black)
      (q/text-align :left :top)
      (q/text-font (:lcars (q/state :fonts)) 18)
      (q/text name x y)
      (q/no-stroke)
      (apply q/fill color)
      (q/rect-mode :corner)
      (q/rect scale-x y scale-w h h)
      (apply q/fill (mercury-color value mercury-colors))
      (q/rect (+ scale-x (- scale-w mercury)) (+ 2 y) mercury (- h 4) h)
      ))


  (setup [this] this)

  (update-state [_ world]
    (let [[new-state events] (p/update-elements state world)]
      (p/pack-update
        (h-scale. new-state)
        events))))
