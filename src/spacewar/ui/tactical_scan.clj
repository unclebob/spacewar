(ns spacewar.ui.tactical-scan
  (:require [quil.core :as q]
            [spacewar.ui.config :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.ui.protocols :as p]
            [spacewar.geometry :refer :all]
            [spacewar.vector :as v]))

(defn- draw-background [state]
  (let [{:keys [w h]} state]
    (q/fill 0 0 0)
    (q/rect-mode :corner)
    (q/rect 0 0 w h)))

(defn- in-range [x y ship]
  (< (distance [x y] [(:x ship) (:y ship)]) (/ sector-size 2)))

(defn- present-objects [state objects]
  (let [{:keys [w h ship]} state
        scale-x (/ w sector-size)
        scale-y (/ h sector-size)]
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

(deftype tactical-scan [state]
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
    (tactical-scan. state))

  (update-state [_ {:keys [global-state]}]
    (p/pack-update
      (tactical-scan.
        (assoc state :stars (:stars global-state)
                     :klingons (:klingons global-state)
                     :ship (:ship global-state)
                     :bases (:bases global-state))))))