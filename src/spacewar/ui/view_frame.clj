(ns spacewar.ui.view-frame
  (:require [quil.core :as q]
            [spacewar.ui.config :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.ui.strategic-scan :refer :all]
            [spacewar.ui.tactical-scan :refer :all]
            [spacewar.ui.front-view :refer :all]
            [spacewar.ui.protocols :as p]))

(deftype view-frame [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h contents]} state]
      (q/no-stroke)
      (q/fill 0 0 255)
      (q/rect-mode :corner)
      (q/rect (- x 5) (- y 5) (+ w 10) (+ h 10))
      (q/fill 0 0 0)
      (q/rect x y w h 5)
      (p/draw contents)))

  (setup [_] (let [{:keys [x y w h]} state]
               (view-frame. (assoc state :contents (p/setup (->front-view {:x x :y y :h h :w w}))
                                    :elements [:contents]))))

  (update-state [_ commands-and-state]
    (let [{:keys [x y w h]} state
          commands (:commands commands-and-state)
          commanded-state (cond
                            (some? (p/get-command :strategic-scan commands))
                            (assoc state :contents (p/setup (->strategic-scan {:x x :y y :h h :w w})))

                            (some? (p/get-command :tactical-scan commands))
                            (assoc state :contents (p/setup (->tactical-scan {:x x :y y :h h :w w})))

                            (some? (p/get-command :front-view commands))
                            (assoc state :contents (p/setup (->front-view {:x x :y y :h h :w w})))

                            :else state)
          [new-state _] (p/update-elements commanded-state commands-and-state)]
      (p/pack-update
        (view-frame. new-state)))))