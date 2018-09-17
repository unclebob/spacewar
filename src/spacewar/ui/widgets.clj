(ns spacewar.ui.widgets
  (:require [quil.core :as q]
            [spacewar.ui.protocols :as p]
            [spacewar.geometry :refer :all]))

(def white [255 255 255])
(def black [0 0 0])

(deftype button [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h name color mouse-in left-down]} state]
      (q/stroke-weight 2)
        (apply q/stroke (if mouse-in white color))
        (apply q/fill (if left-down white color))
      (q/rect-mode :corner)
      (q/rect x y w h h)
      (q/text-align :right :bottom)
      (q/text-font (:lcars (q/state :fonts)) 18)
      (apply q/fill black)
      (q/text name (+ x w -10) (+ y h))))

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

(defn rectangle-light [x y w h]
  (q/rect-mode :corner)
  (q/rect x y w h))

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

  (update-state [_ commands]
    (let [[new-state events] (p/update-elements state commands)]
      (p/pack-update
        (named-indicator. new-state)
        events))))

(deftype h-scale [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h name min max value]} state
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
      (apply q/fill black)
      (q/rect-mode :corner)
      (q/rect scale-x y scale-w h)
      (q/fill 255 255 0)
      (q/rect (+ scale-x (- scale-w mercury)) (+ 2 y) mercury (- h 4))
      ))


  (setup [this] this)

  (update-state [_ commands]
    (let [[new-state events] (p/update-elements state commands)]
      (p/pack-update
        (h-scale. new-state)
        events))))

(defn degree-tick [radius angle]
  (let [tick-length (if (zero? (rem angle 30)) 10 5)
        tick-radius (- radius tick-length)
        radians (* 2 Math/PI (/ angle 360))
        sin-r (Math/sin radians)
        cos-r (Math/cos radians)]
    (map #(Math/round %)
         [(* sin-r radius) (* cos-r radius)
          (* sin-r tick-radius) (* cos-r tick-radius)])))

(defn draw-bezel-ring [[cx cy radius] ring-color fill-color]
  (apply q/fill fill-color)
  (q/stroke-weight 1)
  (apply q/stroke ring-color)
  (q/ellipse-mode :radius)
  (q/ellipse cx cy radius radius))

(defn draw-ticks [[x y radius]]
  (doseq [angle-tenth (range 36)]
    (q/with-translation
      [x y]
      (apply q/line (degree-tick radius (* 10 angle-tenth))))))

(defn draw-labels [[x y _] radius]
  (doseq [angle-thirtieth (range 12)]
    (let [angle-tenth (* 3 angle-thirtieth)
          angle (* 10 angle-tenth)
          radians (* Math/PI 2 (/ angle 360))
          label-x (* radius (Math/cos radians))
          label-y (* radius (Math/sin radians))]
      (q/with-translation
        [x y]
        (apply q/fill black)
        (q/text-align :center :center)
        (q/text-font (:lcars-small (q/state :fonts)) 12)
        (q/text (str angle-tenth) label-x label-y)))))

(defn draw-pointer [x y length direction color]
  (let [radians (* 2 Math/PI (/ direction 360))
        indicator-x (* length (Math/cos radians))
        indicator-y (* length (Math/sin radians))]
    (q/no-stroke)
    (apply q/fill color)
    (q/ellipse-mode :center)
    (q/with-translation
      [x y]
      (q/ellipse indicator-x indicator-y 5 5)
      (q/stroke-weight 1)
      (apply q/stroke color)
      (q/line 0 0 indicator-x indicator-y))))

(defn- ->circle [x y diameter]
  (let [radius (/ diameter 2)
        center-x (+ x radius)
        center-y (+ y radius)]
    [center-x center-y radius]))

(deftype direction-selector [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y diameter direction color mouse-in]} state
          circle (->circle x y diameter)
          [center-x center-y radius] circle
          label-radius (- radius 20)
          pointer-length (- radius 35)
          ring-color (if mouse-in white black)]
      (draw-bezel-ring circle ring-color color)
      (draw-ticks circle)
      (draw-labels circle label-radius)
      (draw-pointer center-x center-y pointer-length direction black)
      (q/rect-mode :center)
      (q/no-stroke)
      (apply q/fill color)
      (q/rect center-x center-y 20 20)
      (apply q/fill black)
      (q/text-align :center :center)
      (q/text-font (:lcars-small (q/state :fonts)) 12)
      (q/text (str direction) center-x center-y)))

  (setup [this] this)

  (update-state [_ commands]
    (let [{:keys [x y diameter]} state
          [updated-state events] (p/update-elements state commands)
          circle (->circle x y diameter)
          mouse-pos [(q/mouse-x) (q/mouse-y)]
          mouse-in (inside-circle circle mouse-pos)
          left-down (and mouse-in (q/mouse-pressed?) (= :left (q/mouse-button)))
          new-state (assoc updated-state
                      :mouse-in mouse-in
                      :left-down left-down)]
      (p/pack-update
        (direction-selector. new-state) events))))


