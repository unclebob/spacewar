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
  (let [commodity (:commodity transport)]
    (q/ellipse-mode :center)
    (q/no-stroke)
    (apply q/fill (transport-color commodity))
    (condp = commodity
      :dilithium (q/triangle 0 -8 -6 6 6 6)
      :antimatter (q/triangle 0 8 -6 -6 6 -6))))

(defn- age-angle [age]
  (let [maturity (min 1 (/ age glc/base-maturity-age))]
    (* (- 1 maturity) 2 Math/PI)))

(defn- draw-base-age [age]
  (q/fill 0 0 0 150)
  (q/no-stroke)
  (q/ellipse-mode :center)
  (q/arc 0 0 30 30 0 (age-angle age) #?(:clj :pie)))

(defn- draw-inventory-circle [commodity maximum color radius]
  (let [angle (* 2 Math/PI (/ commodity maximum))]
    (q/stroke-weight 3)
    (q/no-fill)
    (when (> angle 0.01)
      (apply q/stroke color)
      (q/arc 0 0 radius radius 0 angle))))

(defn- draw-base-contents [antimatter dilithium corbomite]
  (draw-inventory-circle dilithium glc/base-dilithium-maximum uic/yellow 30)
  (draw-inventory-circle antimatter glc/base-antimatter-maximum uic/orange 38)
  (draw-inventory-circle corbomite glc/corbomite-maximum uic/green 46))

(defn- draw-base-counts [base]
  (apply q/fill uic/white)
  (apply q/stroke uic/white)
  (q/stroke-weight 1)

  (q/text-align :right :center)
  (q/text-font (:lcars-small (q/state :fonts)) 12)
  (q/text (str "T-" (int (:torpedos base))) -30 0)
  (q/text-align :left :center)
  (q/text (str "K-" (int (:kinetics base))) 30 0)
  )

(defn- draw-base-adornments [base]
  (draw-base-age (:age base))
  (draw-base-contents (:antimatter base) (:dilithium base) (:corbomite base))
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

(defmethod draw-base-icon :corbomite-factory [base]
  (q/no-fill)
  (apply q/stroke uic/corbomite-factory-color)
  (q/stroke-weight 2)
  (q/line -10 10 10 -10)
  (q/line 10 10 -10 -10)
  (q/line -10 10 10 10)
  (q/line -10 -10 10 -10)
  (apply q/fill (if (< 200 (mod (q/millis) 400))
                  uic/corbomite-factory-color
                  uic/black))
  (q/ellipse 0 0 10 10)
  (draw-base-adornments base))

(defmethod draw-base-icon :corbomite-device [_base]
  (q/no-fill)
  (apply q/stroke uic/corbomite-factory-color)
  (q/stroke-weight 2)
  (q/line -10 10 10 -10)
  (q/line 10 10 -10 -10)
  (q/line -10 10 10 10)
  (q/line -10 -10 10 -10)
  (apply q/stroke uic/red)
  (q/stroke-weight 4)
  (q/line -10 0 10 0)
  (apply q/fill (if (< 200 (mod (q/millis) 400))
                  uic/red
                  uic/black))
  (q/ellipse 0 0 10 10))

(defn- klingon-state [{:keys [cruise-state battle-state mission]}]
  (let [cruise-state (condp = cruise-state
                       :patrol "P"
                       :refuel "R"
                       :guard "G"
                       :mission "M"
                       "X")
        mission (condp = mission
                  :blockade "B"
                  :seek-and-destroy "A"
                  :escape-corbomite "E"
                  "-")
        battle-state (condp = battle-state
                       :no-battle "n"
                       :flank-right "fr"
                       :flank-left "fl"
                       :retreating "r"
                       :advancing "a"
                       :kamikazee "K")]
    (str mission ":" cruise-state "-" battle-state))
  )

(defn draw-klingon-counts [klingon]
  (when glc/klingon-stats
    (apply q/fill uic/white)
    (q/text-align :right :center)
    (q/text-font (:lcars-small (q/state :fonts)) 12)
    (q/text (str "T-" (int (/ (:torpedos klingon) glc/klingon-torpedos 0.01)) "%") -30 0)
    (q/text-align :left :center)
    (q/text (str "A-" (int (/ (:antimatter klingon) glc/klingon-antimatter 0.01)) "%") 30 0)
    (q/text-align :center :bottom)
    (q/text (klingon-state klingon) 0 -30)
    (q/text-align :center :top)
    (q/text (str "K-" (int (:kinetics klingon))) 0 30)
    ))

(defn draw-klingon-icon [klingon]
  (apply q/fill uic/black)
  (apply q/stroke uic/klingon-color)
  (q/stroke-weight (if (and (= :kamikazee (:battle-state klingon))
                            (< 250 (mod (q/millis) 500)))
                     5
                     2))
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

(defn draw-ship-icon [[vx vy] radians ship]
  (apply q/stroke uic/enterprise-vector-color)
  (q/stroke-weight 2)
  (q/line 0 0 vx vy)
  (q/with-rotation
    [radians]
    (apply q/stroke uic/enterprise-color)
    (q/stroke-weight (if (and (:corbomite-device-installed ship)
                              (< 250 (mod (q/millis) 500)))
                       4
                       2))
    (q/ellipse-mode :center)
    (apply q/fill uic/black)
    (q/line -9 -9 0 0)
    (q/line -9 9 0 0)
    (q/ellipse 0 0 9 9)
    (q/line -5 9 -15 9)
    (q/line -5 -9 -15 -9)))

(defn draw-star-icon [star]
  (let [class (:class star)
        pulsar? (= class :pulsar)
        pulsar-on? (< (mod (q/millis) 500) 250)]
    (apply q/fill (class uic/star-colors))
    (when (or (not pulsar?)
              pulsar-on?)
      (q/ellipse 0 0 (class uic/star-sizes) (class uic/star-sizes)))))

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