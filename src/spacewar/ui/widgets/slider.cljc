(ns spacewar.ui.widgets.slider
  (:require [quil.core :as q #?@(:cljs [:include-macros true])]
            [spacewar.ui.protocols :as p]
            [spacewar.geometry :as geo]
            [spacewar.ui.config :as uic]))

(defn draw-slider-bezel [state]
  (let [{:keys [w h color mouse-in]} state]
    (q/stroke-weight 2)
    (apply q/stroke uic/light-grey)
    (apply q/fill uic/light-grey)
    (q/rect-mode :corner)
    (q/rect 0 0 w h)
    (apply q/stroke (if mouse-in uic/white color))
    (apply q/fill color)
    (q/rect 0 0 w h w)))

(defn draw-slider-thumb [{:keys [stroke thumb-color thumb-x thumb-y w thumb-h value]}]
  (q/stroke-weight 1)
  (apply q/stroke stroke)
  (apply q/fill thumb-color)
  (q/rect-mode :center)
  (q/rect thumb-x thumb-y w thumb-h thumb-h)
  (apply q/fill uic/black)
  (q/text-align :center :center)
  (q/text-font (:lcars-small (q/state :fonts)) 12)
  (q/text (str value) thumb-x thumb-y))

(defn slider-thumb-val [state]
  (let [{:keys [y min-val max-val mouse-pos margin range increment]} state
        [_ my] mouse-pos
        rel-my (- my margin y)
        m-pos (/ rel-my increment)
        m-val (max min-val (min max-val (+ min-val (q/floor (- range m-pos)))))]
    m-val))

(defn draw-slider-labels [state]
  (let [{:keys [min-val max-val thumb-x min-y max-y]} state]
    (apply q/fill uic/black)
    (q/text-align :center :center)
    (q/text-font (:lcars-small (q/state :fonts)) 12)
    (q/text (str min-val) thumb-x min-y)
    (q/text (str max-val) thumb-x max-y)))

(defn- set-slider-state [state]
  (let [{:keys [w h min-val max-val]} state
        range (- max-val min-val)
        margin 20
        increment (/ (- h margin margin) range)
        thumb-x (/ w 2)
        max-y margin
        min-y (+ margin (* increment range))
        thumb-h 20]
    (assoc state :range range
                 :margin margin
                 :increment increment
                 :thumb-x thumb-x
                 :max-y max-y
                 :min-y min-y
                 :thumb-h thumb-h)))

(deftype slider [state]
  p/Drawable
  (get-state [_] state)
  (clone [_ clone-state] (slider. clone-state))
  (draw [_]
    (let [{:keys [x y w max-val increment value thumb-color mouse-in left-down
                  margin thumb-x thumb-h disabled]} state
          relative-pos (- max-val value)                    ;top is max
          thumb-y (+ margin (* relative-pos increment))]
      (when (not disabled)
        (q/with-translation
          [x y]
          (draw-slider-bezel state)
          (when (not left-down)
            (draw-slider-labels state))

          (draw-slider-thumb {:stroke (if mouse-in uic/black uic/light-grey)
                              :thumb-color thumb-color
                              :thumb-x thumb-x
                              :thumb-y thumb-y
                              :w w
                              :thumb-h thumb-h
                              :value value})
          (when left-down
            (let [m-val (slider-thumb-val state)
                  m-pos (- max-val m-val)                   ; top is max
                  mouse-thumb-y (+ margin (* m-pos increment))]
              (draw-slider-thumb {:stroke uic/light-grey
                                  :thumb-color uic/light-grey
                                  :thumb-x thumb-x
                                  :thumb-y mouse-thumb-y
                                  :w w
                                  :thumb-h thumb-h
                                  :value m-val})))))))

  (setup [_]
      (slider. (set-slider-state state)))

  (update-state [_ _]
    (let [{:keys [x y w h]} state
          last-left-down (:left-down state)
          mouse-pos [(q/mouse-x) (q/mouse-y)]
          mouse-in (geo/inside-rect [x y w h] mouse-pos)
          left-down (and mouse-in (q/mouse-pressed?) (= :left (q/mouse-button)))
          state (set-slider-state state)
          state (assoc state
                      :mouse-in mouse-in
                      :left-down left-down
                      :mouse-pos mouse-pos)
          button-released? (and (not left-down) last-left-down mouse-in)
          event (if button-released?
                  (assoc (:left-up-event state) :value (slider-thumb-val state))
                  nil)]
      (p/pack-update (slider. state) event))))
