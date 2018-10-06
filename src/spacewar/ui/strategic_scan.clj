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
  (let [{:keys [stars pixel-width sector-top-left]} state]
    (when stars
      (apply q/fill grey)
      (q/no-stroke)
      (q/ellipse-mode :center)
      (doseq [{:keys [x y]} stars]
        (let [[ox oy] sector-top-left
              x (- x ox)
              y (- y oy)]
          (q/ellipse (* x pixel-width) (* y pixel-width) 4 4))))
    )
  )

(defn- draw-klingons [state]
  (let [{:keys [klingons pixel-width sector-top-left]} state]
    (when klingons
      (apply q/fill black)
      (apply q/stroke klingon-color)
      (q/stroke-weight 2)
      (q/ellipse-mode :center)
      (doseq [{:keys [x y]} klingons]
        (let [[ox oy] sector-top-left
                      x (- x ox)
                      y (- y oy)]
        (q/with-translation
          [(* x pixel-width)
           (* y pixel-width)]
          (q/line 0 0 10 -6)
          (q/line 10 -6 14 -3)
          (q/line 0 0 -10 -6)
          (q/line -10 -6 -14 -3)
          (q/ellipse 0 0 6 6)))))))

(defn- draw-ship [state]
  (let [{:keys [ship pixel-width sector-top-left]} state
        heading (or (->> state :ship :heading) 0)
        velocity (or (->> state :ship :velocity) [0 0])
        [vx vy] (v/scale velocity-vector-scale velocity)
        radians (->radians heading)
        [ox oy] sector-top-left
        x (- (:x ship) ox)
        y (- (:y ship) oy)]
    (q/with-translation
      [(* x pixel-width)
       (* y pixel-width)]
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
  (let [{:keys [bases pixel-width sector-top-left]} state]
    (when bases
      (q/no-fill)
      (apply q/stroke base-color)
      (q/stroke-weight 2)
      (q/ellipse-mode :center)
      (doseq [{:keys [x y]} bases]
        (let [[ox oy] sector-top-left
                      x (- x ox)
                      y (- y oy)]
        (q/with-translation
          [(* x pixel-width)
           (* y pixel-width)]
          (q/ellipse 0 0 12 12)
          (q/ellipse 0 0 20 20)
          (q/line 0 -6 0 6)
          (q/line -6 0 6 0)))))))

(defn- draw-sectors [state]
  (let [{:keys [pixel-width]} state]
    (q/stroke-weight 1)
    (apply q/stroke (conj white 100))
    (doseq [x (range 0 known-space-x strategic-range)]
      (q/line (* x pixel-width) 0 (* x pixel-width) known-space-y))
    (doseq [y (range 0 known-space-y strategic-range)]
      (q/line 0 (* y pixel-width) known-space-x (* y pixel-width)))))

(deftype strategic-scan [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y]} state]
      (q/with-translation
        [x y]
        (draw-background state)
        (draw-stars state)
        (draw-klingons state)
        (draw-ship state)
        (draw-bases state)
        (draw-sectors state))))

  (setup [_]
    (strategic-scan.
      (assoc state :pixel-width (/ (:h state) strategic-range))))

  (update-state [_ world]
    (let [ship (:ship world)
          scale (:strat-scale ship)
          range (* scale strategic-range)
          sector-top-left [(- (:x ship) (rem (:x ship) range))
                           (- (:y ship) (rem (:y ship) range))]
          ]
      (p/pack-update
        (strategic-scan.
          (assoc state :stars (:stars world)
                       :klingons (:klingons world)
                       :ship ship
                       :bases (:bases world)
                       :pixel-width (/ (:h state) range)
                       :sector-top-left sector-top-left
                       :range range))))))