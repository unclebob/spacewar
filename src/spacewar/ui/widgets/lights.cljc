(ns spacewar.ui.widgets.lights
  (:require [quil.core :as q #?@(:cljs [:include-macros true])]
            [spacewar.ui.protocols :as p]
            [spacewar.ui.config :as uic]))

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
  (get-state [_] state)
  (clone [_ clone-state] (indicator-light. clone-state))
  (draw [_]
    (let [{:keys [x y w h level draw-func colors]} state]
      (apply q/stroke uic/black)
      (q/stroke-weight 1)
      (apply q/fill (nth colors level))
      (draw-func x y w h)))

  (setup [_] (indicator-light. (assoc state :level 0)))
  (update-state [this _] (p/pack-update this))
  )

