(ns spacewar.ui.strategic-scan
  (:require [quil.core :as q]
            [spacewar.geometry :refer :all]
            [spacewar.ui.config :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.ui.protocols :as p]
            [spacewar.vector :as v]))

(defn- draw-background [state]
  (let [{:keys [w h]} state]
    (q/fill 0 0 0)
    (q/rect-mode :corner)
    (q/rect 0 0 w h)))

(defn- draw-stars [state]
  (let [{:keys [stars pixel-width ship]} state
        sx (:x ship)
        sy (:y ship)]
    (when stars
      (q/no-stroke)
      (q/ellipse-mode :center)
      (doseq [{:keys [x y class]} stars]
        (apply q/fill (class star-colors))
        (q/ellipse (* (- x sx) pixel-width) (* (- y sy) pixel-width) (class star-sizes) (class star-sizes))))))


(defn- draw-klingons [state]
  (let [{:keys [klingons pixel-width ship]} state
        sx (:x ship)
        sy (:y ship)]
    (when klingons
      (apply q/fill black)
      (apply q/stroke klingon-color)
      (q/stroke-weight 2)
      (q/ellipse-mode :center)
      (doseq [{:keys [x y]} klingons]
        (q/with-translation
          [(* (- x sx) pixel-width)
           (* (- y sy) pixel-width)]
          (q/line 0 0 10 -6)
          (q/line 10 -6 14 -3)
          (q/line 0 0 -10 -6)
          (q/line -10 -6 -14 -3)
          (q/ellipse 0 0 6 6))))))

(defn- draw-ship [state]
  (let [heading (or (->> state :ship :heading) 0)
        velocity (or (->> state :ship :velocity) [0 0])
        [vx vy] (v/scale velocity-vector-scale velocity)
        radians (->radians heading)]
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
      (q/line -5 -9 -15 -9))))

(defn- draw-bases [state]
  (let [{:keys [bases pixel-width ship]} state
        sx (:x ship)
        sy (:y ship)]
    (when bases
      (q/no-fill)
      (apply q/stroke base-color)
      (q/stroke-weight 2)
      (q/ellipse-mode :center)
      (doseq [{:keys [x y]} bases]
        (q/with-translation
          [(* (- x sx) pixel-width)
           (* (- y sy) pixel-width)]
          (q/ellipse 0 0 12 12)
          (q/ellipse 0 0 20 20)
          (q/line 0 -6 0 6)
          (q/line -6 0 6 0))))))

(defn- draw-sectors [state]
  (let [{:keys [pixel-width ship]} state
        sx (:x ship)
        sy (:y ship)
        x->frame (fn [x] (* pixel-width (- x sx)))
        y->frame (fn [y] (* pixel-width (- y sy)))]
    (q/stroke-weight 1)
    (apply q/stroke (conj white 100))
    (doseq [x (range 0 known-space-x strategic-range)]
      (q/line (x->frame x) (y->frame 0) (x->frame x) (y->frame known-space-y)))
    (doseq [y (range 0 known-space-y strategic-range)]
      (q/line (x->frame 0) (y->frame y) (x->frame known-space-x) (y->frame y)))))

(deftype strategic-scan [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h]} state]
      (q/with-translation
        [(+ x (/ w 2)) (+ y (/ h 2))]
        (draw-background state)
        (draw-stars state)
        (draw-klingons state)
        (when (not (-> state :game-over))
          (draw-ship state))
        (draw-bases state)
        (draw-sectors state))))

  (setup [_]
    (strategic-scan.
      (assoc state :pixel-width (/ (:h state) strategic-range))))

  (update-state [_ world]
    (let [ship (:ship world)
          scale (:strat-scale ship)
          range (* scale strategic-range)]
      (p/pack-update
        (strategic-scan.
          (assoc state :game-over (:game-over world)
                       :stars (:stars world)
                       :klingons (:klingons world)
                       :ship ship
                       :bases (:bases world)
                       :pixel-width (/ (:h state) range)
                       :sector-top-left [0 0]))))))