(ns spacewar.game-logic.config)

(def frame-rate 30)

(def spectral-classes [:o :b :a :f :g :k :m])
(def drag-factor 0.001)
(def rotation-rate 0.01) ; degrees per millisecond.
(def impulse-thrust 0.01) ; per millisecond per power.
(def warp-leap 10000) ;spacial coordinates.
(def warp-charge-rate 1)
(def warp-threshold 2000)

(def phaser-range 30000)
(def phaser-velocity 10) ;per ms
(def phaser-proximity 1000)
(def phaser-damage 80)

(def torpedo-range 100000)
(def torpedo-velocity 5)
(def torpedo-proximity 2000)
(def torpedo-damage 100)

(def kinetic-range 1000000)
(def kinetic-velocity 2)
(def kinetic-proximity 500)
(def kinetic-damage 50)

(def number-of-stars 1000)
(def number-of-klingons 100)
(def number-of-bases 15)
(def tactical-range 200000)
(def strategic-range 1000000)

(def known-space-x 18000000)
(def known-space-y 10000000)

(def klingon-shields 200)
(def klingon-anti-matter 1000)
(def klingon-shield-recharge-rate 0.01)
(def klingon-tactical-range 150000)
(def klingon-kinetic-range 1000000)
