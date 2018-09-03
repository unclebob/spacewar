(ns spacewar.core
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(defn setup []
  (q/frame-rate 60)
  (q/color-mode :hsb)
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

(defn update-state [{:keys [hue sat brt hv sv bv angle r w v]}]
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

(defn draw-state [{:keys [hue sat brt angle r w v]}]
  (q/background 0)
  (q/fill hue sat brt)
  (let [x (* r (q/cos angle))
        y (* r (q/sin angle))]
    (q/with-translation [(/ (q/width) 2)
                         (/ (q/height) 2)]
                        (q/ellipse x y w v))))

(defn -main [& args]
  (q/defsketch space-war
               :title "Space War"
               :size [(q/screen-width) (q/screen-height)]
               :setup setup
               :update update-state
               :draw draw-state
               :features [:keep-on-top]
               :middleware [m/fun-mode]))
