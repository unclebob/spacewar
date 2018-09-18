(ns spacewar.ui.widgets
  (:require [quil.core :as q]
            [spacewar.ui.protocols :as p]
            [spacewar.geometry :refer :all]))

(def white [255 255 255])
(def black [0 0 0])
(def dark-grey [50 50 50])
(def grey [128 128 128])
(def light-grey [200 200 200])

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
  (q/stroke-weight 2)
  (apply q/stroke ring-color)
  (q/ellipse-mode :radius)
  (q/ellipse cx cy radius radius))

(defn draw-ticks [[x y radius]]
  (apply q/stroke black)
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
  (let [[tip-x tip-y] (rotate-vector length direction)
        base-width 10
        base-offset 15
        [xb1 yb1] (rotate-vector base-offset (- direction base-width))
        [xb2 yb2] (rotate-vector base-offset (+ direction base-width))]
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
    (let [{:keys [x y diameter direction color mouse-in left-down mouse-pos]} state
          circle (->circle x y diameter)
          [cx cy radius] circle
          label-radius (- radius 18)
          pointer-length (- radius 25)
          ring-color (if mouse-in white color)
          mouse-angle (if left-down (angle [cx cy] mouse-pos) 0)
          direction-text (str (q/round (if left-down mouse-angle direction)))
          text-color (if left-down grey black)]
      (draw-bezel-ring circle ring-color color)
      (when mouse-in
        (draw-ticks circle)
        (draw-labels circle label-radius))
      (draw-pointer cx cy pointer-length direction black)
      (when left-down
        (draw-pointer cx cy pointer-length mouse-angle grey))
      (draw-direction-text direction-text cx cy color text-color)))

  (setup [_] (direction-selector. (assoc state :mouse-pos [0 0])))

  (update-state [_ _]
    (let [{:keys [x y diameter]} state
          last-left-down (:left-down state)
          [cx cy _ :as circle] (->circle x y diameter)
          mouse-pos [(q/mouse-x) (q/mouse-y)]
          mouse-in (inside-circle circle mouse-pos)
          left-down (and mouse-in (q/mouse-pressed?) (= :left (q/mouse-button)))
          left-up (and (not left-down) last-left-down mouse-in)
          event (if left-up
                  (assoc (:left-up-event state) :angle (q/round (angle [cx cy] mouse-pos)))
                  nil)
          new-state (assoc state
                      :mouse-pos mouse-pos
                      :mouse-in mouse-in
                      :left-down left-down)]
      (p/pack-update
        (direction-selector. new-state) event))))

(defn draw-slider-bezel [state]
  (let [{:keys [w h color mouse-in]} state]
    (q/stroke-weight 2)
    (apply q/stroke (if mouse-in white color))
    (apply q/fill color)
    (q/rect-mode :corner)
    (q/rect 0 0 w h w)))

(defn draw-slider-thumb [{:keys [stroke thumb-color thumb-x thumb-y w thumb-h value]}]
  (q/stroke-weight 1)
  (apply q/stroke stroke)
  (apply q/fill thumb-color)
  (q/rect-mode :center)
  (q/rect thumb-x thumb-y w thumb-h thumb-h)
  (apply q/fill black)
  (q/text-align :center :center)
  (q/text-font (:lcars-small (q/state :fonts)) 12)
  (q/text (str value) thumb-x thumb-y))

(defn slider-thumb-val [state]
  (let [{:keys [y min-val max-val mouse-pos margin increment]} state
        [_ my] mouse-pos
        rel-my (- my margin y)
        m-pos (max min-val (min max-val (q/round (/ rel-my increment))))
        m-val (- max-val m-pos)]
    m-val))

(defn draw-slider-labels [state]
  (let [{:keys [min-val max-val thumb-x min-y max-y]} state]
    (apply q/fill black)
    (q/text-align :center :center)
    (q/text-font (:lcars-small (q/state :fonts)) 12)
    (q/text (str min-val) thumb-x min-y)
    (q/text (str max-val) thumb-x max-y)))

(deftype slider [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w max-val value thumb-color mouse-in left-down
                  margin increment thumb-x thumb-h]} state
          relative-pos (- max-val value)                    ;top is max
          thumb-y (+ margin (* relative-pos increment))]
      (q/with-translation
        [x y]
        (draw-slider-bezel state)
        (when (not left-down)
          (draw-slider-labels state))

        (draw-slider-thumb {:stroke (if mouse-in black light-grey)
                            :thumb-color thumb-color
                            :thumb-x thumb-x
                            :thumb-y thumb-y
                            :w w
                            :thumb-h thumb-h
                            :value value})
        (when left-down
          (let [m-val (slider-thumb-val state)
                m-pos (- max-val m-val)                     ; top is max
                mouse-thumb-y (+ margin (* m-pos increment))]
            (draw-slider-thumb {:stroke light-grey
                                :thumb-color light-grey
                                :thumb-x thumb-x
                                :thumb-y mouse-thumb-y
                                :w w
                                :thumb-h thumb-h
                                :value m-val}))))))

  (setup [_]
    (let [{:keys [w h min-val max-val]} state
          range (- max-val min-val)
          margin 10
          increment (/ (- h margin margin) range)
          thumb-x (/ w 2)
          max-y margin
          min-y (+ margin (* increment range))
          thumb-h 20]
      (slider. (assoc state :range range
                            :margin margin
                            :increment increment
                            :thumb-x thumb-x
                            :max-y max-y
                            :min-y min-y
                            :thumb-h thumb-h))))

  (update-state [_ _]
    (let [{:keys [x y w h last-left-down]} state
          mouse-pos [(q/mouse-x) (q/mouse-y)]
          mouse-in (inside-rect [x y w h] mouse-pos)
          left-down (and mouse-in (q/mouse-pressed?) (= :left (q/mouse-button)))
          new-state (assoc state
                      :mouse-in mouse-in
                      :left-down left-down
                      :mouse-pos mouse-pos)
          event (if (and (not left-down) last-left-down mouse-in)
                  (assoc (:left-up-event state) :value (slider-thumb-val state))
                  nil)]
      (p/pack-update (slider. new-state) event))))