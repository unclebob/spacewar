(ns spacewar.ui.front-view
  (:require [quil.core :as q #?@(:cljs [:include-macros true])]
            [spacewar.geometry :as geo]
            [spacewar.ui.strategic-scan :refer [->strategic-scan]]
            [spacewar.ui.protocols :as p]
            [spacewar.vector :as vector]))

; Stars
;                                 * star
;                                 |
;                                 | v-distance
;                                 |
;       ---> Direction of ship    |
;----------------------------------
;        h-distance
;
; luminosity is the absolute brightness of the star.
; Angle (in radians) is the angle of the star from the center of the
; screen.  (right is zero, left is pi, etc.)
;
; All units are arbitrary except the angle.
; The constants you see scattered around are all tweaks
; done by hand.  Fiddle with them to see how they work.


(def star-count 200)
(def f-lum 200)                                             ; luminosity factor

(defn- move-star [star]
  (let [h-distance (dec (:h-distance star))]
    (if (pos? h-distance)
      (assoc star :h-distance h-distance)
      nil)))

(defn- move-stars [stars]
  (filter some? (map move-star stars)))

(defn- star-in-frame [state sx sy]
  (let [{:keys [x y w h]} state
        margin 10
        xmin (+ x margin)
        ymin (+ y margin)
        xmax (- (+ x w) margin)
        ymax (- (+ y h) margin)]
    (and (< sx xmax)
         (< sy ymax)
         (> sx xmin)
         (> sy ymin))))

(defn- star-size [m]
  (let [mm (* f-lum m)]
    (cond
      (< mm 1) 1
      (< mm 3) 2
      (< mm 5) 3
      (< mm 10) 4
      (< mm 20) 5
      :else 6)))

(defn- star-color [m]
  (let [mm (* f-lum m)]
    (if (>= mm 0.5)
      [255 255 255]
      (repeat 3 (* 2 mm 256)))))

; magic numbers are tweaks that affect the star pattern.
(defn- make-random-star []
  (let [luminosity (+ 1 (rand 5))
        h-distance (rand (* luminosity 200))]
    {:h-distance h-distance
     :v-distance (+ -20 (rand 200) (/ h-distance 20))
     :angle (geo/->radians (rand 360))
     :luminosity luminosity}))

(defn- make-stars [n]
  (repeatedly n make-random-star))

(defn- add-stars [state]
  (let [stars (:stars state)]
    (if (< (count stars) star-count)
      (assoc state :stars (conj stars (make-random-star)))
      state)))

(deftype front-view [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y h w stars]} state]
      (q/no-stroke)
      (doseq [star stars]
        (let [{:keys [h-distance v-distance luminosity angle]} star
              ; rd is radial distance of star from center of screen
              rd (* h (/ v-distance h-distance))
              [rx ry] (vector/from-angular rd angle)
              sx (+ rx x (/ w 2))
              sy (+ ry y (/ h 2))
              ; m is relative brightness, inversely proportional to distance.
              m (/ luminosity
                   (Math/sqrt (+ (* h-distance h-distance) (* v-distance v-distance))))
              sz (star-size m)]
          (when (star-in-frame state sx sy)
            (do
              (apply q/fill (star-color m))
              (q/ellipse-mode :corner)
              (q/ellipse sx sy sz sz)))))))

  (setup [_] (front-view. (assoc state :stars (make-stars star-count))))

  (update-state [_ _]
    (p/pack-update
      (front-view. (add-stars (update state :stars move-stars))))))
