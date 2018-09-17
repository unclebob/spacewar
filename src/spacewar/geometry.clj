(ns spacewar.geometry)

(defn- square [x] (* x x))

(defn distance [[x1 y1] [x2 y2]]
  (Math/sqrt
    (+ (square (- x1 x2))
       (square (- y1 y2)))))


(defn inside-rect [[rx ry rw rh] [px py]]
  (and (>= px rx)
       (>= py ry)
       (< px (+ rx rw))
       (< py (+ ry rh))))

(defn inside-circle [[cx cy radius] [px py]]
  (< (distance [cx cy] [px py]) radius))

(defn angle [[x1 y1] [x2 y2]]
  (let [a (- y2 y1)
        b (- x2 x1)
        c (Math/sqrt (+ (square a) (square b)))
        radians (Math/asin (/ (Math/abs a) c))
        degrees (/ (* 360 radians) (* 2 Math/PI))]

    (cond
      (and (zero? a) (zero? b)) :bad-angle
      (and (>= a 0) (>= b 0)) degrees
      (and (>= a 0) (neg? b)) (- 180 degrees)
      (and (neg? a) (neg? b)) (+ 180 degrees)
      (and (neg? a) (>= b 0)) (- 360 degrees))
      ))

(defn rotate-vector [length angle]
  (let [radians (* 2 Math/PI (/ angle 360))]
    [(* length (Math/cos radians))
     (* length (Math/sin radians))]))