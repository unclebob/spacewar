(ns spacewar.ui.widgets.button
  (:require [quil.core :as q]
              [spacewar.ui.protocols :as p]
              [spacewar.geometry :refer :all]
              [spacewar.ui.config :refer :all]))

(deftype button [state]
  p/Drawable
  (get-state [_] state)

  (clone [_ clone-state]
    (button. clone-state))

  (draw [_]
    (let [{:keys [x y w h name color mouse-in left-down status]} state]
      (q/with-translation
        [x y]
        (q/stroke-weight 2)
        (apply q/stroke (if mouse-in white color))
        (apply q/fill (if left-down white color))
        (q/rect-mode :corner)
        (q/rect 0 0 w h h)
        (q/text-align :right :bottom)
        (q/text-font (:lcars (q/state :fonts)) 18)
        (apply q/fill black)
        (q/text name (+ w -10) (+ h))
        (when status
          (do (q/text-size 14)
              (q/text-align :left :top)
              (q/text status 10 10))))))

  (setup [this] this)
  (update-state [_ _]
    (let [{:keys [x y w h]} state
          last-left-down (:left-down state)
          mx (q/mouse-x)
          my (q/mouse-y)
          mouse-in (inside-rect [x y w h] [mx my])
          left-down (and mouse-in (q/mouse-pressed?) (= :left (q/mouse-button)))
          new-state (assoc state :mouse-in mouse-in :left-down left-down)
          event (if (and (not left-down) last-left-down mouse-in)
                  (:left-up-event state)
                  nil)]
      (p/pack-update (button. new-state) event))))
