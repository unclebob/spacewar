(ns spacewar.ui.widgets.direction-selector
  (:require [quil.core :as q #?@(:cljs [:include-macros true])]
            [spacewar.ui.protocols :as p]
            [spacewar.geometry :as geo]
            [spacewar.ui.config :as uic]
            [spacewar.vector :as vector]))

(defn degree-tick [radius angle]
  (let [tick-length (if (zero? (rem angle 30)) 10 5)
        tick-radius (- radius tick-length)
        radians (geo/->radians angle)
        sin-r (Math/sin radians)
        cos-r (Math/cos radians)]
    (map #(geo/round %)
         [(* sin-r radius) (* cos-r radius)
          (* sin-r tick-radius) (* cos-r tick-radius)])))

(defn draw-bezel-ring [[cx cy radius] ring-color fill-color]
  (apply q/fill fill-color)
  (q/stroke-weight 2)
  (apply q/stroke ring-color)
  (q/ellipse-mode :radius)
  (q/ellipse cx cy radius radius))

(defn draw-ticks [[x y radius]]
  (apply q/stroke uic/black)
  (doseq [angle-tenth (range 36)]
    (q/with-translation
      [x y]
      (apply q/line (degree-tick radius (* 10 angle-tenth))))))

(defn draw-labels [[x y _] radius]
  (doseq [angle-thirtieth (range 12)]
    (let [angle-tenth (* 3 angle-thirtieth)
          angle (* 10 angle-tenth)
          radians (geo/->radians angle)
          [label-x label-y] (vector/from-angular radius radians)]
      (q/with-translation
        [x y]
        (apply q/fill uic/black)
        (q/text-align :center :center)
        (q/text-font (:lcars-small (q/state :fonts)) 12)
        (q/text (str angle-tenth) label-x label-y)))))

(defn draw-pointer [x y length direction color]
  (let [radians (geo/->radians direction)
        [tip-x tip-y] (vector/from-angular length radians)
        base-width (geo/->radians 10)
        base-offset 15
        [xb1 yb1] (vector/from-angular base-offset (- radians base-width))
        [xb2 yb2] (vector/from-angular base-offset (+ radians base-width))]
    (q/no-stroke)
    (apply q/fill color)
    (q/with-translation
      [x y]
      (q/triangle tip-x tip-y xb1 yb1 xb2 yb2)
      )))

(defn draw-direction-text [text cx cy dial-color text-color]
  (q/rect-mode :center)
  (q/no-stroke)
  (apply q/fill dial-color)
  (q/rect cx cy 20 20)
  (apply q/fill text-color)
  (q/text-align :center :center)
  (q/text-font (:lcars-small (q/state :fonts)) 12)
  (q/text text cx cy))

(defn- ->circle [x y diameter]
  (let [radius (/ diameter 2)
        center-x (+ x radius)
        center-y (+ y radius)]
    [center-x center-y radius]))

(deftype direction-selector [state]
  p/Drawable
  (get-state [_] state)
  (clone [_ clone-state]
    (direction-selector. clone-state))

  (draw [_]
    (let [{:keys [x y diameter direction color mouse-in left-down mouse-pos pointer2]} state
          circle (->circle x y diameter)
          [cx cy radius] circle
          label-radius (- radius 18)
          pointer-length (- radius 25)
          ring-color (if mouse-in uic/white color)
          mouse-angle (if left-down (geo/angle-degrees [cx cy] mouse-pos) 0)
          direction-text (str (geo/round (if left-down mouse-angle direction)))
          text-color (if left-down uic/grey uic/black)]
      (draw-bezel-ring circle ring-color color)
      (when mouse-in
        (draw-ticks circle)
        (draw-labels circle label-radius))
      (when pointer2
        (draw-pointer cx cy pointer-length pointer2 uic/light-grey))
      (draw-pointer cx cy pointer-length direction uic/black)
      (when left-down
        (draw-pointer cx cy pointer-length mouse-angle uic/grey))
      (draw-direction-text direction-text cx cy color text-color)))

  (setup [_] (direction-selector. (assoc state :mouse-pos [0 0])))

  (update-state [_ _]
    (let [{:keys [x y diameter]} state
          last-left-down (:left-down state)
          [cx cy _ :as circle] (->circle x y diameter)
          mouse-pos [(q/mouse-x) (q/mouse-y)]
          mouse-in (geo/inside-circle circle mouse-pos)
          left-down (and mouse-in (q/mouse-pressed?) (= :left (q/mouse-button)))
          left-up (and (not left-down) last-left-down mouse-in)
          event (if left-up
                  (assoc (:left-up-event state) :angle (geo/round (geo/angle-degrees [cx cy] mouse-pos)))
                  nil)
          new-state (assoc state
                      :mouse-pos mouse-pos
                      :mouse-in mouse-in
                      :left-down left-down)]
      (p/pack-update
        (direction-selector. new-state) event))))
