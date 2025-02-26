(ns spacewar.game-logic.explosions-spec
  (:require
    [spacewar.game-logic.explosions :as explosions]
    [speclj.core :refer :all]))

(defn valid-fragments [n x y velocity]
  (let [valid-fragment (fn [e] (and (= x (:x e))
                                    (= y (:y e))
                                    (<= (/ velocity 2) (:velocity e) (* 2 velocity))
                                    (<= 0 (:direction e) 360)))]
    (fn [fragments]
      (and (= n (count fragments))
           (every? identity (map valid-fragment fragments))))))

(describe "explosion behavior"
  (it "updates explosions age"
    (should= {:explosions [{:age 20 :type :phaser}]}
             (explosions/update-explosions 10 {:explosions [{:age 10 :type :phaser}]})))

  (it "makes fragments"
    (should= [] (explosions/make-fragments 0 {:x 1 :y 1} 10))
    (should ((valid-fragments 1 1 2 10) (explosions/make-fragments 1 {:x 1 :y 2} 10)))
    (should ((valid-fragments 6 1 2 10) (explosions/make-fragments 6 {:x 1 :y 2} 10)))))
