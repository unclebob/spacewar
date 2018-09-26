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
  (let [{:keys [stars x-pixel-width y-pixel-width]} state]
    (when stars
      (apply q/fill grey)
      (q/no-stroke)
      (q/ellipse-mode :center)
      (doseq [{:keys [x y]} stars]
        (q/ellipse (* x x-pixel-width) (* y y-pixel-width) 4 4)))
    )
  )

(defn- draw-klingons [state]
  (let [{:keys [klingons x-pixel-width y-pixel-width]} state]
    (when klingons
      (apply q/fill black)
      (apply q/stroke klingon-color)
      (q/stroke-weight 2)
      (q/ellipse-mode :center)
      (doseq [{:keys [x y]} klingons]
        (q/with-translation
          [(* x x-pixel-width)
           (* y y-pixel-width)]
          (q/line 0 0 10 -6)
          (q/line 10 -6 14 -3)
          (q/line 0 0 -10 -6)
          (q/line -10 -6 -14 -3)
          (q/ellipse 0 0 6 6)
          )))))

(defn- draw-ship [state]
  (let [{:keys [ship x-pixel-width y-pixel-width]} state
        heading (or (->> state :ship :heading) 0)
        velocity (or (->> state :ship :velocity) [0 0])
        [vx vy] (v/scale velocity-vector-scale velocity)
        radians (->radians heading)]
    (q/with-translation
      [(* (:x ship) x-pixel-width)
       (* (:y ship) y-pixel-width)]
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
  (let [{:keys [bases x-pixel-width y-pixel-width]} state]
    (when bases
      (q/no-fill)
      (apply q/stroke base-color)
      (q/stroke-weight 2)
      (q/ellipse-mode :center)
      (doseq [{:keys [x y]} bases]
        (q/with-translation
          [(* x x-pixel-width)
           (* y y-pixel-width)]
          (q/ellipse 0 0 12 12)
          (q/ellipse 0 0 20 20)
          (q/line 0 -6 0 6)
          (q/line -6 0 6 0))))))

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
        (draw-bases state))))

  (setup [_]
    (strategic-scan.
      (assoc state :x-pixel-width (/ (:w state) known-space-x)
                   :y-pixel-width (/ (:h state) known-space-y))))

  (update-state [_ world]
    (p/pack-update
      (strategic-scan.
        (assoc state :stars (:stars world)
                     :klingons (:klingons world)
                     :ship (:ship world)
                     :bases (:bases world))))))