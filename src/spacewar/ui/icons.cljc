(ns spacewar.ui.icons
  (:require [quil.core :as q #?@(:cljs [:include-macros true])]
            [spacewar.ui.config :as uic]
            [spacewar.game-logic.config :as glc]
            [spacewar.geometry :as geo]
            [spacewar.vector :as vector]
            [spacewar.util :as util]))

(defn- transport-color [commodity]
  (condp = commodity
    :antimatter uic/orange
    :dilithium uic/yellow))

(defn draw-transport-icon [transport]
  (q/ellipse-mode :center)
  (q/no-stroke)
  (apply q/fill (transport-color (:commodity transport)))
  (q/triangle 0 -8 -6 6 6 6))

(defn- age-angle [age]
  (let [maturity (min 1 (/ age glc/base-maturity-age))]
    (* (- 1 maturity) 2 Math/PI)))

(defn- draw-base-age [age]
  (q/fill 0 0 0 150)
  (q/no-stroke)
  (q/ellipse-mode :center)
  (q/arc 0 0 30 30 0 (age-angle age) #?(:clj :pie)))

(defn- draw-base-contents [antimatter dilithium]
  (let [antimatter-angle (* 2 Math/PI (/ antimatter glc/base-antimatter-maximum))
        dilithium-angle (* 2 Math/PI (/ dilithium glc/base-dilithium-maximum))]
    (q/stroke-weight 3)
    (q/no-fill)
    (when (> dilithium-angle 0.01)
      (apply q/stroke uic/yellow)
      (q/arc 0 0 30 30 0 dilithium-angle))
    (when (> antimatter-angle 0.01)
      (apply q/stroke uic/orange)
      (q/arc 0 0 35 35 0 antimatter-angle))))

(defn- draw-base-counts [base]
  (apply q/fill uic/white)
  (q/text-align :right :center)
  (q/text-font (:lcars-small (q/state :fonts)) 12)
  (q/text (str "T-" (int (:torpedos base))) -30 0)
  (q/text-align :left :center)
  (q/text (str "K-" (int (:kinetics base))) 30 0)
  )

(defn- draw-base-adornments [base]
  (draw-base-age (:age base))
  (draw-base-contents (:antimatter base) (:dilithium base))
  (when (= (:type base) :weapon-factory)
    (draw-base-counts base)))

(defn draw-romulan-icon []
  (q/stroke-weight 2)
  (q/line -3 -6 3 -6)
  (q/line 3 -6 6 -3)
  (q/line 6 -3 6 3)
  (q/line 6 3 3 9)
  (q/line 3 9 -3 9)
  (q/line -3 9 -6 3)
  (q/line -6 3 -6 -3)
  (q/line -6 -3 -3 -6)
  (q/line 6 -3 12 -9)
  (q/line 12 -9 12 -6)
  (q/line 12 -6 6 3)
  (q/line -6 -3 -12 -9)
  (q/line -12 -9 -12 -6)
  (q/line -12 -6 -6 3)
  (q/line -12 -12 -12 3)
  (q/line 12 -12 12 3)

  )

(defmulti draw-romulan :state)

(defmethod draw-romulan :invisible [romulan]
  (q/no-stroke)
  (q/fill 255 255 255 (min 15 (* 15 (/ (:age romulan) glc/romulan-invisible-time))))
  (q/ellipse 0 0 30 30)
  )

(defmethod draw-romulan :appearing [romulan]
  (apply q/stroke (conj uic/white (min 255 (* 255 (/ (:age romulan) glc/romulan-appearing-time)))))
  (draw-romulan-icon)
  )

(defmethod draw-romulan :visible [romulan]
  (apply q/stroke (util/color-shift uic/white uic/orange (min 1 (/ (:age romulan) glc/romulan-visible-time))))
  (draw-romulan-icon)
  )

(defmethod draw-romulan :firing [romulan]
  (apply q/stroke (util/color-shift uic/orange uic/red (min 1 (/ (:age romulan) glc/romulan-firing-time))))
  (doseq [_ (range 10)]
    (q/line 0 0 (- 30 (rand 60)) (- 30 (rand 60))))
  (draw-romulan-icon)
  )

(defmethod draw-romulan :fading [romulan]
  (apply q/stroke (util/color-shift uic/red uic/black (min 1 (/ (:age romulan) glc/romulan-fading-time))))
  (draw-romulan-icon)
  )

(defmethod draw-romulan :disappeared [_]
  )

(defn draw-strategic-romulan []
  (apply q/stroke uic/orange)
  (draw-romulan-icon))

(defmulti draw-base-icon :type)

(defmethod draw-base-icon :weapon-factory [base]
  (q/no-fill)
  (apply q/stroke uic/weapon-factory-color)
  (q/stroke-weight 2)
  (q/ellipse-mode :center)
  (q/ellipse 0 0 12 12)
  (q/ellipse 0 0 20 20)
  (q/line 0 -6 0 6)
  (q/line -6 0 6 0)
  (draw-base-adornments base))

(defmethod draw-base-icon :antimatter-factory [base]
  (q/no-fill)
  (apply q/stroke uic/antimatter-factory-color)
  (q/stroke-weight 2)
  (q/ellipse-mode :center)
  (q/ellipse 0 0 12 12)
  (q/line 0 -6 0 6)
  (q/line -6 0 6 0)
  (q/ellipse 0 -8 5 5)
  (q/ellipse 0 8 5 5)
  (q/ellipse -8 0 5 5)
  (q/ellipse 8 0 5 5)
  (draw-base-adornments base))


(defmethod draw-base-icon :dilithium-factory [base]
  (q/no-fill)
  (apply q/stroke uic/dilithium-factory-color)
  (q/stroke-weight 2)
  (q/ellipse-mode :center)
  (q/quad 0 6 6 0 0 -6 -6 0)
  (q/quad 0 10 10 0 0 -10 -10 0)
  (q/line 0 -6 0 6)
  (q/line -6 0 6 0)
  (q/line 3 10 -3 10)
  (q/line 3 -10 -3 -10)
  (q/line 10 3 10 -3)
  (q/line -10 3 -10 -3)
  (draw-base-adornments base))

(defn draw-klingon-icon []
  (apply q/fill uic/black)
  (apply q/stroke uic/klingon-color)
  (q/stroke-weight 2)
  (q/ellipse-mode :center)
  (q/line 0 0 10 -6)
  (q/line 10 -6 14 -3)
  (q/line 0 0 -10 -6)
  (q/line -10 -6 -14 -3)
  (q/ellipse 0 0 6 6))

(defn draw-klingon-shields [shields]
  (when (< shields glc/klingon-shields)
    (let [pct (/ shields glc/klingon-shields)
          flicker (< (rand 3) pct)
          color [255 (* pct 255) 0 (if flicker (* pct 100) 100)]
          radius (+ 35 (* pct 20))]
      (apply q/fill color)
      (q/ellipse-mode :center)
      (q/no-stroke)
      (q/ellipse 0 0 radius radius))))

(defn draw-ship-icon [[vx vy] radians]
  (apply q/stroke uic/enterprise-vector-color)
  (q/stroke-weight 2)
  (q/line 0 0 vx vy)
  (q/with-rotation
    [radians]
    (apply q/stroke uic/enterprise-color)
    (q/stroke-weight 2)
    (q/ellipse-mode :center)
    (apply q/fill uic/black)
    (q/line -9 -9 0 0)
    (q/line -9 9 0 0)
    (q/ellipse 0 0 9 9)
    (q/line -5 9 -15 9)
    (q/line -5 -9 -15 -9)))

(defn draw-star-icon [star]
  (let [class (:class star)]
    (q/no-stroke)
    (q/ellipse-mode :center)
    (apply q/fill (class uic/star-colors))
    (q/ellipse 0 0 (class uic/star-sizes) (class uic/star-sizes))))

(defn- draw-blob [jitter half-jitter diameter]
  (q/ellipse (- half-jitter (rand jitter))
             (- half-jitter (rand jitter))
             diameter diameter)
  )

(defn- draw-spark [x y]
  (apply q/fill uic/yellow)
  (q/ellipse x y 1 1))

(defn- rand-sign []
  (if (< 0.5 (rand 1)) 1 -1))

(defn draw-cloud-icon [cloud]
  (let [diameter (* 0.3 (:concentration cloud))
        jitter (/ diameter 5)
        half-jitter (/ jitter 2)]
    (apply q/fill (conj uic/yellow 10))
    (q/no-stroke)
    (q/ellipse-mode :center)
    (doseq [_ (range 10)]
      (draw-blob jitter half-jitter (* diameter (- 0.5 (rand 1)))))
    (doseq [_ (range 20)]
      (let [r (rand (/ diameter 4))
            x (+ 2 (rand r))
            x-sqr (* x x)
            y (Math/sqrt (- (* r r) x-sqr))
            x (* (rand-sign) x)
            y (* (rand-sign) y)]
        (draw-spark x y)))))

(defn- romulan-blast-visual-intensity [range]
  (let [factor (* 255 (- 1.0 (/ range glc/romulan-blast-range)))]
    factor))

(defn- romulan-blast-weight []
  (- 10 (rand 8)))

(defn- romulan-blast-color []
  [255 (rand 255) (rand 50)])

(defn draw-romulan-shot [scale shot]
  (let [{:keys [range bearing]} shot
        scaled-range (* range scale)
        radians (geo/->radians bearing)
        radians-to-origin (+ radians Math/PI)
        shot-center (vector/from-angular scaled-range radians-to-origin)
        shot-x (first shot-center)
        shot-y (second shot-center)
        half-pi (/ Math/PI 2)]
    (apply q/stroke (conj (romulan-blast-color) (romulan-blast-visual-intensity range)))
    (q/stroke-weight (romulan-blast-weight))
    (q/ellipse-mode :radius)
    (q/no-fill)
    (q/arc shot-x shot-y scaled-range scaled-range (- radians half-pi) (+ radians half-pi)))
  )