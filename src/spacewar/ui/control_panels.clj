(ns spacewar.ui.control-panels
  (:require [quil.core :as q]
            [spacewar.ui.protocols :as p]
            [spacewar.ui.buttons :as b]))

(def banner-width 40)
(def stringer-width 15)

(defn lcars-points [state]
  (let [{:keys [x y w h]} state
        inverted (:inverted state false)]
    (if inverted
      {:a [(+ x w) y]
       :b [(+ x banner-width) y]
       :c [x (+ y banner-width)]
       :d [x (+ y h)]
       :e [(+ x stringer-width) (+ y h)]
       :f [(+ x stringer-width) (+ y banner-width stringer-width)]
       :g [(+ x stringer-width stringer-width) (+ y banner-width)]
       :h [(+ x w) (+ y banner-width)]
       :c1 [x y]
       :c2 [(+ x stringer-width) (+ y banner-width)]
       :label-position [(+ x w -10) (+ y 10)]}
      {:a [x y]
       :b [(+ x w (- banner-width)) y]
       :c [(+ x w) (+ y banner-width)]
       :d [(+ x w) (+ y h)]
       :e [(+ x w (- stringer-width)) (+ y h)]
       :f [(+ x w (- stringer-width)) (+ y banner-width stringer-width)]
       :g [(+ x w (- stringer-width) (- stringer-width)) (+ y banner-width)]
       :h [x (+ y banner-width)]
       :c1 [(+ x w) y]
       :c2 [(+ x w (- stringer-width)) (+ y banner-width)]
       :label-position [(+ x 10) (+ y 10)]})))

(defn draw-lcars [state]
  (let [inverted (:inverted state false)
        {:keys [a b c d e f g h c1 c2 label-position]} (lcars-points state)]
    (q/no-stroke)
    (q/fill 150 150 255)
    (q/begin-shape)
    (apply q/vertex a)
    (apply q/vertex b)
    (apply q/quadratic-vertex (concat c1 c))
    (apply q/vertex d)
    (apply q/vertex e)
    (apply q/vertex f)
    (apply q/quadratic-vertex (concat c2 g))
    (apply q/vertex h)
    (apply q/vertex a)
    (q/end-shape)
    (q/fill 0 0 0)
    (q/text-size 24)
    (q/text-font (:lcars (q/state :fonts)))
    (q/text-align (if inverted :right :left) :top)
    (apply q/text (:name state) label-position)))

(deftype scan-panel [state]
  p/Drawable
  (draw [_]
    (draw-lcars state)
    (p/draw-elements state))

  (setup [_]
    (let [{:keys [x y w]} state]
      (scan-panel.
        (assoc state
          :strategic (p/setup
                       (b/->button
                         {:x x
                          :y (+ y banner-width 10)
                          :w (- w stringer-width 10)
                          :h 40
                          :name "STRAT"
                          :color [200 100 255]
                          :left-up-event {:event :strategic-scan}}))
          :tactical (p/setup
                      (b/->button
                        {:x x
                         :y (+ y banner-width 10 40 10)
                         :w (- w stringer-width 10)
                         :h 40
                         :name "TACT"
                         :color [200 100 255]
                         :left-up-event {:event :tactical-scan}}))
          :elements [:strategic :tactical]))))

  (update-state [_ commands]
    (let [[new-state events] (p/update-elements state commands)]
      (p/pack-update
        (scan-panel. new-state)
        events))))

(deftype engine-panel [state]
  p/Drawable
  (draw [_]
    (draw-lcars state)
    (p/draw-elements state))

  (setup [_]
    (let [{:keys [x y w]} state]
      (engine-panel. state)
      ;  (assoc state
      ;    :strategic (p/setup
      ;                 (b/->button
      ;                   {:x x
      ;                    :y (+ y banner-width 10)
      ;                    :w (- w stringer-width 10)
      ;                    :h 40
      ;                    :name "STRAT"
      ;                    :color [200 100 255]
      ;                    :left-up-event {:event :strategic-scan}}))
      ;    :tactical (p/setup
      ;                (b/->button
      ;                  {:x x
      ;                   :y (+ y banner-width 10 40 10)
      ;                   :w (- w stringer-width 10)
      ;                   :h 40
      ;                   :name "TACT"
      ;                   :color [200 100 255]
      ;                   :left-up-event {:event :tactical-scan}}))
      ;    :elements [:strategic :tactical]))
      ))

  (update-state [_ commands]
    (let [[new-state events] (p/update-elements state commands)]
      (p/pack-update
        (engine-panel. new-state)
        events))))
