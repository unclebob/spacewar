(ns spacewar.game-logic.config)

(def frame-rate 30)

(def drag-factor 0.001)
(def rotation-rate 0.01) ; degrees per millisecond.
(def impulse-thrust 0.01) ; per millisecond per power.
(def warp-leap 10000) ;spacial coordinates.
(def warp-charge-rate 1)
(def warp-threshold 2000)


(def number-of-stars 1000)
(def number-of-klingons 100)
(def number-of-bases 15)
(def sector-size 100000)

(def known-space-x 3000000)
(def known-space-y 2000000)
