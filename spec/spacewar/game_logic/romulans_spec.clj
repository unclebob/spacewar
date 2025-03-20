(ns spacewar.game-logic.romulans-spec
  (:require [spacewar.game-logic.romulans :as r]
            [spacewar.game-logic.spec-mother :as mom]
            [speclj.core :refer [with describe it should should-be-nil should-not should=]]))
(declare ms romulan world)

(describe "romulans"
  (it "can make romulan"
    (should (mom/valid-romulan? (r/make-romulan 12 12))))

  (describe "romulans behavior"
    (with ms 10)
    (with romulan (mom/make-romulan))
    (with world (assoc (mom/make-world) :romulans [@romulan]))


      (it "romulans age"
        (let [updated-world (r/update-romulans-age @ms @world)
              updated-romulan (first (:romulans updated-world))]
          (should= @ms (:age updated-romulan))))

      (describe "romulans state transitions"
        (for [[current-state next-state fire-weapon]
              [[:invisible :appearing false]
               [:appearing :visible false]
               [:visible :firing false]
               [:firing :fading true]
               [:fading :disappeared false]]]
          (it (str "transitions from " current-state " to " next-state)
            (with-redefs [r/romulan-state-transition (fn [_ _ state] (= state current-state))]
              (let [romulan (assoc @romulan :state current-state :age 99)
                    world (assoc @world :romulans [romulan])
                    updated-world (r/update-romulans-state ms world)
                    updated-romulan (first (:romulans updated-world))]
                (should= next-state (:state updated-romulan))
                (should= 0 (:age updated-romulan))
                (should= fire-weapon (:fire-weapon updated-romulan)))))))

      (it "romulan does not create shot when firing-weapon is false"
        (let [romulan (assoc @romulan :fire-weapon false)
              world (assoc @world :romulans [romulan])
              updated-world (r/fire-romulan-weapons world)
              updated-romulan (first (:romulans updated-world))
              shots (:shots updated-world)]
          (should-not (:fire-weapon updated-romulan))
          (should= 0 (count shots))))

      (it "romulan creates shot when firing-weapon is true"
        (let [ship (assoc (mom/make-ship) :x 0 :y 0)
              romulan (assoc @romulan :fire-weapon true :x 100 :y 0)
              world (assoc @world :romulans [romulan] :ship ship)
              updated-world (r/fire-romulan-weapons world)
              updated-romulan (first (:romulans updated-world))
              shots (:shots updated-world)
              first-shot (first shots)]
          (should-not (:fire-weapon updated-romulan))
          (should= 1 (count shots))
          (should= :romulan-blast (:type first-shot))
          (should= (:x romulan) (:x first-shot))
          (should= (:y romulan) (:y first-shot))
          (should (<= 179.5 (:bearing first-shot) 180.5))))

      (it "disappeared romulans are deleted"
        (let [romulan1 (assoc (mom/make-romulan) :state :disappeared)
              romulan2 (assoc (mom/make-romulan) :state :invisible)
              world (assoc @world :romulans [romulan1 romulan2])
              updated-world (r/remove-disappeared-romulans world)
              romulans (:romulans updated-world)]
          (should= [romulan2] romulans)))

      (it "hit romulans are deleted"
        (let [romulan (assoc (mom/make-romulan) :hit :some-hit)
              world (assoc @world :romulans [romulan])
              updated-world (r/destroy-hit-romulans world)]
          (should-be-nil (seq (:romulans updated-world)))
          (should= 1 (:romulans-killed updated-world))))

      (it "update-romulans calls all intermediaries"
        (with-redefs [r/update-romulans-age (constantly @world)
                      r/update-romulans-state (constantly @world)
                      r/remove-disappeared-romulans (constantly @world)
                      r/destroy-hit-romulans (constantly @world)
                      r/fire-romulan-weapons (constantly @world)]
          (should= @world (r/update-romulans @ms @world))))

      (it "Romulans do not appear when ship is in warp"
        (let [ship (assoc (mom/make-ship) :x 0 :y 0 :warp 1)
              world (assoc @world :ship ship :romulans [])
              updated-world (r/add-romulan world)]
          (should-be-nil (seq (:romulans updated-world)))))))