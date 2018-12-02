(ns spacewar.game-logic.explosions-test
  (:require [midje.sweet :refer [fact]]
            [spacewar.game-logic.explosions :refer [update-explosions
                                                    make-fragments]]))

(fact
  "update-explosions"
  (update-explosions 10 {:explosions [{:age 10 :type :phaser}]}) => {:explosions [{:age 20 :type :phaser}]}
  )

(defn valid-fragments [n x y velocity]
  (let [valid-fragment (fn [e] (and (= x (:x e))
                                     (= y (:y e))
                                     (<= (/ velocity 2) (:velocity e) (* 2 velocity))
                                     (<= 0 (:direction e) 360)))]
    (fn [fragments]
      (and (= n (count fragments))
           (every? identity (map valid-fragment fragments))))))

(fact
  "make-fragments"
  (make-fragments 0 {:x 1 :y 1} 10) => []
  (make-fragments 1 {:x 1 :y 2} 10) => (valid-fragments 1 1 2 10)
  (make-fragments 6 {:x 1 :y 2} 10) => (valid-fragments 6 1 2 10)
  )

