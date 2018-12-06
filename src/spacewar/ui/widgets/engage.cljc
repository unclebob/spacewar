(ns spacewar.ui.widgets.engage
  (:require [quil.core :as q #?@(:cljs [:include-macros true])]
            [spacewar.ui.protocols :as p]
            [spacewar.geometry :as geo]
            [spacewar.ui.config :as uic]))

(deftype engage [state]
  p/Drawable
  (get-state [_] state)
  (clone [_ clone-state] (engage. clone-state))

  (draw [_]
    (let [{:keys [x y w h color activation-color name button-time disabled]} state
          cx (/ w 2)
          cy (/ h 2)]
      (when (not disabled)
        (q/with-translation
          [x y]
          (q/no-stroke)
          (q/rect-mode :center)
          (apply q/fill (if (= button-time h) uic/white activation-color))
          (q/rect cx cy w h w)
          (apply q/fill color)
          (q/rect cx cy w (- h button-time) w)
          (apply q/fill uic/black)
          (q/text-align :center :center)
          (q/text-font (:lcars (q/state :fonts)) 18)
          (q/text name cx cy)))))


  (setup [_]
    (engage. (assoc state :button-time 0)))

  (update-state [_ _]
    (let [{:keys [x y w h disabled]} state
          last-button-time (:button-time state)
          last-left-down (:left-down state)
          mouse-pos [(q/mouse-x) (q/mouse-y)]
          mouse-in (geo/inside-rect [x y w h] mouse-pos)
          left-down (and (not disabled) mouse-in (q/mouse-pressed?) (= :left (q/mouse-button)))
          button-time (if left-down (min h (+ 10 last-button-time)) 0)
          new-state (assoc state
                      :mouse-in mouse-in
                      :left-down left-down
                      :mouse-pos mouse-pos
                      :button-time button-time)
          event (if (and (not left-down) last-left-down mouse-in (= last-button-time h))
                  (:left-up-event state)
                  nil)]
      (p/pack-update (engage. new-state) event))))
