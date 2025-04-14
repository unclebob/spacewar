(ns spacewar.ui.strategic-scan
  (:require [quil.core :as q #?@(:cljs [:include-macros true])]
            [spacewar.geometry :as geo]
            [spacewar.ui.config :as uic]
            [spacewar.ui.icons :as icons]
            [spacewar.game-logic.config :as glc]
            [spacewar.ui.protocols :as p]
            [spacewar.vector :as vector]))

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
      (doseq [{:keys [x y] :as star} stars]
        (q/with-translation
          [(* (- x sx) pixel-width)
           (* (- y sy) pixel-width)]
          (icons/draw-star-icon star))))))

(defn- draw-klingons [state]
  (let [{:keys [klingons pixel-width ship]} state
        sx (:x ship)
        sy (:y ship)]
    (when klingons
      (doseq [{:keys [x y] :as klingon} klingons]
        (q/with-translation
          [(* (- x sx) pixel-width)
           (* (- y sy) pixel-width)]
          (icons/draw-klingon-icon klingon)
          (icons/draw-klingon-counts klingon)
          )))))

(defn- draw-romulans [state]
  (let [{:keys [romulans pixel-width ship]} state
        sx (:x ship)
        sy (:y ship)]
    (when romulans
      (doseq [{:keys [x y]} romulans]
        (q/with-translation
          [(* (- x sx) pixel-width)
           (* (- y sy) pixel-width)]
          (icons/draw-strategic-romulan))))))

(defn- draw-ship [state]
  (let [heading (or (->> state :ship :heading) 0)
        velocity (or (->> state :ship :velocity) [0 0])
        [vx vy] (vector/scale uic/velocity-vector-scale velocity)
        radians (geo/->radians heading)]
    (icons/draw-ship-icon [vx vy] radians (:ship state))))

(defn- draw-bases [state]
  (let [{:keys [bases pixel-width ship]} state
        sx (:x ship)
        sy (:y ship)]
    (when bases
      (doseq [{:keys [x y] :as base} bases]
        (q/with-translation
          [(* (- x sx) pixel-width)
           (* (- y sy) pixel-width)]
          (icons/draw-base-icon base))))))

(defn- draw-transport-routes [state]
  (apply q/stroke uic/transport-route-color)
  (q/stroke-weight 3)

  (let [{:keys [pixel-width ship transport-routes]} state
        sx (:x ship)
        sy (:y ship)]
    (doseq [route transport-routes]
      (let [b1 (first route)
            b2 (second route)
            b1x (* (- (first b1) sx) pixel-width)
            b1y (* (- (second b1) sy) pixel-width)
            b2x (* (- (first b2) sx) pixel-width)
            b2y (* (- (second b2) sy) pixel-width)]
        (q/line b1x b1y b2x b2y)))))

(defn- draw-transports [state]
  (let [{:keys [transports pixel-width ship]} state]
    (doseq [transport transports]
      (let [sx (:x ship)
            sy (:y ship)
            tx (:x transport)
            ty (:y transport)
            x (* (- tx sx) pixel-width)
            y (* (- ty sy) pixel-width)]
        (q/with-translation
          [x y]
          (icons/draw-transport-icon transport))))))

(defn- draw-sectors [state]
  (let [{:keys [pixel-width ship]} state
        sx (:x ship)
        sy (:y ship)
        x->frame (fn [x] (* pixel-width (- x sx)))
        y->frame (fn [y] (* pixel-width (- y sy)))]
    (q/stroke-weight 1)
    (apply q/stroke (conj uic/white 100))
    (doseq [x (range 0 (inc glc/known-space-x) glc/strategic-range)]
      (let [sector-x (x->frame x)
            sector-y-min (y->frame 0)
            sector-y-max (y->frame glc/known-space-y)]
        (q/line sector-x sector-y-min sector-x sector-y-max)))
    (doseq [y (range 0 (inc glc/known-space-y) glc/strategic-range)]
      (let [sector-y (y->frame y)
            sector-x-min (x->frame 0)
            sector-x-max (x->frame glc/known-space-x)]
        (q/line sector-x-min sector-y sector-x-max sector-y)))))

(defn- click->pos [strategic-scan ship click]
  (let [{:keys [x y w h pixel-width]} strategic-scan
        center (vector/add [(/ w 2) (/ h 2)] [x y])
        click-delta (vector/subtract click center)
        scale (/ 1.0 pixel-width)
        strategic-click-delta (vector/scale scale click-delta)]
    (vector/add strategic-click-delta [(:x ship) (:y ship)])))

(deftype strategic-scan [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h]} state]
      (q/with-translation
        [(+ x (/ w 2)) (+ y (/ h 2))]
        #?(:clj (draw-background state))
        (draw-stars state)
        (draw-transport-routes state)
        (draw-transports state)
        (draw-bases state)
        (draw-klingons state)
        (when (not (-> state :ship :destroyed))
          (draw-ship state))
        (draw-romulans state)
        (draw-sectors state))))

  (setup [_]
    (strategic-scan.
      (assoc state :pixel-width (/ (:h state) glc/strategic-range))))

  (update-state [_ world]
    (let [{:keys [x y w h]} state
          ship (:ship world)
          scale (:strat-scale ship)
          range (* scale glc/strategic-range)
          last-left-down (:left-down state)
          mx (q/mouse-x)
          my (q/mouse-y)
          mouse-in (geo/inside-rect [x y w h] [mx my])
          left-down (and mouse-in (q/mouse-pressed?) (= :left (q/mouse-button)))
          state (assoc state :mouse-in mouse-in :left-down left-down)
          left-up (and (not left-down) last-left-down mouse-in)
          key (and (q/key-pressed?) (q/key-as-keyword))
          event (if left-up
                  (condp = key
                    :p {:event :debug-position-ship :pos (click->pos state ship [mx my])}
                    :k {:event :debug-add-klingon :pos (click->pos state ship [mx my])}
                    :r {:event :debug-resupply-ship}
                    :K {:event :debug-new-klingon-from-praxis}
                    :c {:event :debug-corbomite-device-installed}
                    :f {:event :debug-klingon-stats}
                    nil)
                  nil)]
      (p/pack-update
        (strategic-scan.
          (assoc state :game-over-timer (:game-over-timer world)
                       :stars (:stars world)
                       :klingons (:klingons world)
                       :romulans (:romulans world)
                       :ship ship
                       :bases (:bases world)
                       :transport-routes (:transport-routes world)
                       :transports (:transports world)
                       :pixel-width (/ (:h state) range)
                       :sector-top-left [0 0]))
        event))))