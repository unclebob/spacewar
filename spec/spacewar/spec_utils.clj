(ns spacewar.spec-utils)

(defn roughly=
  "Returns true if a and b are approximately equal within the given tolerance,
   false otherwise. Tolerance defaults to 0.0001 if not provided."
  ([a b]
   (roughly= a b 0.0001))
  ([a b epsilon]
   (cond (zero? a) (< (abs b) epsilon)
         (zero? b) (> (abs a) epsilon)
         :else (<= (abs (- (/ a b) 1)) epsilon))))

(defn roughly-v
  "Returns true if vectors v1 and v2 are approximately equal within the given tolerance,
   false otherwise. Vectors must have the same length. Tolerance defaults to 0.0001."
  ([v1 v2]
   (roughly-v v1 v2 0.0001))
  ([[x1 y1] [x2 y2] tolerance]
   (and (roughly= x1 x2 tolerance)
        (roughly= y1 y2 tolerance))))