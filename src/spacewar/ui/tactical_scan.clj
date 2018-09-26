(ns spacewar.ui.tactical-scan
  (:require [quil.core :as q]
            [spacewar.ui.config :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.ui.protocols :as p]
            [spacewar.geometry :refer :all]
            [spacewar.vector :as v]
            [spacewar.vector :as vector]))

(defn- draw-background [state]
  (let [{:keys [w h]} state]
    (q/fill 0 0 0)
    (q/rect-mode :corner)
    (q/rect 0 0 w h)))

(defn- in-range [x y ship]
  (< (distance [x y] [(:x ship) (:y ship)]) (/ tactical-range 2)))

(defn- present-objects [state objects]
  (let [{:keys [w h ship]} state
        scale-x (/ w tactical-range)
        scale-y (/ h tactical-range)]
    (->> objects
         (filter #(in-range (:x %) (:y %) ship))
         (map #(assoc % :x (- (:x %) (:x ship))
                        :y (- (:y %) (:y ship))))
         (map #(assoc % :x (* (:x %) scale-x)
                        :y (* (:y %) scale-y))))))

(defn- draw-stars [state]
  (let [{:keys [w h stars]} state
        presentable-stars (present-objects state stars)]
    (apply q/fill grey)
    (q/no-stroke)
    (q/ellipse-mode :center)
    (q/with-translation
      [(/ w 2) (/ h 2)]
      (doseq [{:keys [x y]} presentable-stars]
        (q/ellipse x y 4 4)))))

(defn- draw-klingons [state]
  (let [{:keys [w h klingons]} state
        presentable-klingons (present-objects state klingons)]
    (when klingons
      (apply q/fill black)
      (apply q/stroke klingon-color)
      (q/stroke-weight 2)
      (q/ellipse-mode :center)
      (doseq [{:keys [x y]} presentable-klingons]
        (q/with-translation
          [(+ x (/ w 2)) (+ y (/ h 2))]
          (q/line 0 0 10 -6)
          (q/line 10 -6 14 -3)
          (q/line 0 0 -10 -6)
          (q/line -10 -6 -14 -3)
          (q/ellipse 0 0 6 6)
          )))))

(defn- draw-ship [state]
  (let [{:keys [w h]} state
        heading (or (->> state :ship :heading) 0)
        velocity (or (->> state :ship :velocity) [0 0])
        [vx vy] (v/scale velocity-vector-scale velocity)
        radians (->radians heading)]
    (q/with-translation
      [(/ w 2) (/ h 2)]
      (apply q/stroke enterprise-vector-color)
      (q/stroke-weight 2)
      (q/line 0 0 vx vy)
      (q/with-rotation
        [radians]
        (apply q/stroke enterprise-color)
        (q/stroke-weight 2)
        (q/ellipse-mode :center)
        (apply q/fill black)
        (q/line -9 -9 0 0)
        (q/line -9 9 0 0)
        (q/ellipse 0 0 9 9)
        (q/line -5 9 -15 9)
        (q/line -5 -9 -15 -9)))))

(defn- draw-bases [state]
  (let [{:keys [w h bases]} state
        presentable-bases (present-objects state bases)]
    (q/no-fill)
    (apply q/stroke base-color)
    (q/stroke-weight 2)
    (q/ellipse-mode :center)
    (doseq [{:keys [x y]} presentable-bases]
      (q/with-translation
        [(+ x (/ w 2)) (+ y (/ h 2))]
        (q/ellipse 0 0 12 12)
        (q/ellipse 0 0 20 20)
        (q/line 0 -6 0 6)
        (q/line -6 0 6 0)))))

(defn- phaser-intensity [range]
  (let [intensity (* 255 (- 1 (/ range phaser-range)))]
    [intensity intensity intensity]))

(defn- draw-torpedo-segment []
  (let [angle (rand 360)
        color (repeatedly 3 #(+ 128 (rand 127)))
        length (+ 5 (rand 5))
        radians (->radians angle)
        [tx ty] (vector/from-angular length radians)]
    (apply q/stroke color)
    (q/line 0 0 tx ty)))

(defn- draw-torpedo-shots [state]
  (let [{:keys [w h ship]} state
        torpedo-shots (:torpedo-shots ship)
        presentable-torpedo-shots (present-objects state torpedo-shots)]
    (doseq [{:keys [x y]} presentable-torpedo-shots]
      (q/with-translation
        [(+ x (/ w 2)) (+ y (/ h 2))]
        (draw-torpedo-segment)
        (draw-torpedo-segment)
        (draw-torpedo-segment)))))

(defn- draw-kinetic-shots [state])

(defn- draw-phaser-shots [state]
  (let [{:keys [w h ship]} state
        phaser-shots (:phaser-shots ship)
        presentable-phaser-shots (present-objects state phaser-shots)]
    (doseq [{:keys [x y bearing range]} presentable-phaser-shots]
      (q/with-translation
        [(+ x (/ w 2)) (+ y (/ h 2))]
        (let [radians (->radians bearing)
              [sx sy] (vector/from-angular phaser-length radians)
              beam-color (phaser-intensity range)]
          (apply q/stroke beam-color)
          (q/line 0 0 sx sy))))))

(defn- draw-shots [state]
  (draw-phaser-shots state)
  (draw-torpedo-shots state)
  (draw-kinetic-shots state))

(deftype tactical-scan [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y]} state]
      (q/with-translation
        [x y]
        (draw-background state)
        (draw-stars state)
        (draw-shots state)
        (draw-klingons state)
        (draw-ship state)
        (draw-bases state))))

  (setup [_]
    (tactical-scan. state))

  (update-state [_ global-state]
    (p/pack-update
      (tactical-scan.
        (assoc state :stars (:stars global-state)
                     :klingons (:klingons global-state)
                     :ship (:ship global-state)
                     :bases (:bases global-state))))))