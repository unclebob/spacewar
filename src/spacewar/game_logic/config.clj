(ns spacewar.game-logic.config)

(def number-of-stars 1000)
(def number-of-klingons 100)
(def known-space-sectors [15 10])
(def sector-size 1000)

(def known-space-x (* (first known-space-sectors) sector-size))
(def known-space-y (* (second known-space-sectors) sector-size))
