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


