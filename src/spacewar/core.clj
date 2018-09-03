(ns spacewar.core
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(defn setup []
  ; Set frame rate to 30 frames per second.
  (q/frame-rate 60)
  ; Set color mode to HSB (HSV) instead of default RGB.
  (q/color-mode :hsb)
  ; setup function returns initial state. It contains
  ; circle color and position.
  {:hue 128
   :sat 128
   :brt 128
   :hv 0
   :sv 0
   :bv 0
   :angle 0
   :r 50
   :w 50
   :v 50})

(defn rnd [x]
  (let [r (rand (* 2 x))]
    (- r x)))

(defn update-state [{:keys [hue sat brt hv sv bv angle r w v] :as state}]
  ; Update sketch state by changing circle color and position.
  {:hv (+ hv (rnd 0.1))
   :sv (+ sv (rnd 0.1))
   :bv (+ bv (rnd 0.1))
   :hue (mod (+ hue hv) 255)
   :sat (mod (+ sat sv) 255)
   :brt (mod (+ brt bv) 255)
   :angle (+ angle (rnd 0.1))
   :r (+ r (rnd 4))
   :w (+ w (rnd 4))
   :v (+ v (rnd 4))})

(defn draw-state [{:keys [hue sat brt hv sv bv angle r w v] :as state}]
  ; Clear the sketch by filling it with light-grey color.
  (q/background 0)
  ; Set circle color.
  (q/fill (:hue state) (:sat state) (:brt state))
  ; Calculate x and y coordinates of the circle.
  (let [angle (:angle state)
        x (* r (q/cos angle))
        y (* r (q/sin angle))]
    ; Move origin point to the center of the sketch.
    (q/with-translation [(/ (q/width) 2)
                         (/ (q/height) 2)]
      ; Draw the circle.
      (q/ellipse x y w v))))

(defn -main [& args]
  (q/defsketch x
    :title "Space War"
    :size [(q/screen-width) (q/screen-height)]
    ; setup function called only once, during sketch initialization.
    :setup setup
    ; update-state is called on each iteration before draw-state.
    :update update-state
    :draw draw-state
    :features [:keep-on-top]
    ; This sketch uses functional-mode middleware.
    ; Check quil wiki for more info about middlewares and particularly
    ; fun-mode.
    :middleware [m/fun-mode]))
