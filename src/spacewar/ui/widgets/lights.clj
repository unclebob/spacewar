(ns spacewar.ui.widgets.lights
  (:require [quil.core :as q]
            [spacewar.ui.protocols :as p]
            [spacewar.geometry :refer :all]
            [spacewar.ui.config :refer :all]))

(defn rectangle-light [x y w h]
  (q/rect-mode :corner)
  (q/rect x y w h))

(defn round-rectangle-light [x y w h]
  (q/rect-mode :corner)
  (q/no-stroke)
  (q/rect x y w h h))

(defn round-light [x y w h]
  (q/ellipse-mode :corner)
  (q/ellipse x y w h))

(deftype indicator-light [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h level draw-func colors]} state]
      (apply q/stroke black)
      (q/stroke-weight 1)
      (apply q/fill (nth colors level))
      (draw-func x y w h)))

  (setup [_] (indicator-light. (assoc state :level 0)))
  (update-state [this _] (p/pack-update this))
  (get-state [_] state))

(deftype named-indicator [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y name]} state]
      (p/draw-elements state)
      (apply q/fill black)
      (q/text-align :left :top)
      (q/text-font (:lcars (q/state :fonts)) 18)
      (q/text name (+ x 25) y)))

  (setup [_] (named-indicator.
               (assoc state :indicator (p/setup
                                         (->indicator-light state))
                            :elements [:indicator])))

  (update-state [_ global-state]
    (let [[new-state events] (p/update-elements state global-state)]
      (p/pack-update
        (named-indicator. new-state)
        events))))