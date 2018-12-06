(ns spacewar.geometry)

(defn sign [x]
  (cond (zero? x) 0
        (pos? x) 1
        :else -1))

(defn abs [x]
  (if (neg? x)
    (- x)
    x))

(defn round [x]
  (long (Math/round (double x))))

(defn ->degrees [radians]
  (mod (* 360 (/ radians (* 2 Math/PI))) 360))

(defn ->radians [degrees]
  (mod (* 2 Math/PI (/ degrees 360)) (* 2 Math/PI)))

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

(defn angle-degrees [[x1 y1] [x2 y2]]
  (let [a (- y2 y1)
        b (- x2 x1)]
    (if (and (zero? a) (zero? b))
      :bad-angle
      (let [c (Math/sqrt (+ (square a) (square b)))
            radians (Math/asin (/ (abs a) c))
            degrees (->degrees radians)]
        (cond
          (and (>= a 0) (>= b 0)) degrees
          (and (>= a 0) (neg? b)) (- 180 degrees)
          (and (neg? a) (neg? b)) (+ 180 degrees)
          (and (neg? a) (>= b 0)) (- 360 degrees))))))


