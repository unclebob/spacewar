(ns spacewar.game-logic.ship
  (:require
    [spacewar.game-logic.config :refer :all]))

(defn initialize []
  {:x (int (rand known-space-x))
   :y (int (rand known-space-y))
   :antimatter 100
   :core-temp 0
   :dilithium 100}
  )
