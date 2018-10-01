(ns spacewar.game-logic.explosions-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.explosions :refer :all]))

(fact
  "update-explosions"
  (update-explosions 10 {:explosions [{:age 10}]}) => {:explosions [{:age 20}]}
  )

(defn valid-explosions [n x y velocity]
  (let [valid-explosion (fn [e] (and (= x (:x e))
                                     (= y (:y e))
                                     (= velocity (:velocity e))
                                     (<= 0 (:direction e) 360)))]
    (fn [explosions]
      (and (= n (count explosions))
           (every? identity (map valid-explosion explosions))))))

(fact
  "make-fragments"
  (make-fragments 0 {:x 1 :y 1} 10) => []
  (make-fragments 1 {:x 1 :y 2} 10) => (valid-explosions 1 1 2 10)
  (make-fragments 6 {:x 1 :y 2} 10) => (valid-explosions 6 1 2 10)
  )

