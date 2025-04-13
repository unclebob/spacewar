(ns spacewar.core-spec
  (:require [spacewar.core :refer [add-frame-time
                                   frames-per-second
                                   make-initial-world
                                   process-events
                                   update-world]]
            [spacewar.game-logic.spec-mother :as mom]
            [speclj.core :refer [describe it should should-not-be-nil should= should> with]]))

(describe "initial world"
  (it "is created correctly"
    (should-not-be-nil (make-initial-world))
    (should (mom/valid-world? (make-initial-world)))))

(describe "view selection events"
  (it "handles strategic scan"
    (should= :strat-view
             (-> (process-events [{:event :strategic-scan}] {:ship {}})
                 :ship :selected-view)))
  (it "handles tactical scan"
    (should= :tact-view
             (-> (process-events [{:event :tactical-scan}] {:ship {}})
                 :ship :selected-view)))
  (it "handles front view"
    (should= :front-view
             (-> (process-events [{:event :front-view}] {:ship {}})
                 :ship :selected-view))))

(declare ship world)
(describe "engine panel events"
  (with ship (mom/make-ship))
  (with world (assoc (mom/make-world) :ship @ship))


  (describe "engine selection"
    (it "selects warp from none"
      (let [world (assoc-in @world [:ship :selected-engine] :none)]
        (should= :warp (->> world
                            (process-events [{:event :select-warp}])
                            :ship :selected-engine)))))
  (it "selects warp from impulse"
    (let [world (assoc-in @world [:ship :selected-engine] :impulse)]
      (should= :warp
               (->> world
                    (process-events [{:event :select-warp}])
                    :ship :selected-engine))))
  (it "deselects warp"
    (let [world (assoc-in @world [:ship :selected-engine] :warp)]
      (should= :none
               (->> world
                    (process-events [{:event :select-warp}])
                    :ship :selected-engine))))

  (it "handles engine direction"
    (should= 45
             (-> (process-events [{:event :engine-direction :angle 45}] {:ship {}})
                 :ship :heading-setting)))

  (it "handles engine power"
    (should= 75
             (-> (process-events [{:event :engine-power :value 75}] {:ship {}})
                 :ship :engine-power-setting)))

  (it "handles engine engage"
    (should= 0
             (->> {:ship {:engine-power-setting 50 :selected-engine :warp}}
                  (process-events [{:event :engine-engage}])
                  :ship :engine-power-setting))))

(describe "weapons panel events"
  (with ship (mom/make-ship))
  (with world (assoc (mom/make-world) :ship @ship))

  (describe "weapon selection"
    (it "selects phaser from none"
      (let [world (assoc-in @world [:ship :selected-weapon] :none)]
        (should= :phaser
                 (->> world
                      (process-events [{:event :select-phaser}])
                      :ship :selected-weapon)))))

  (it "handles weapon direction"
    (should= 90
             (->> @world
                  (process-events [{:event :weapon-direction :angle 90}])
                  :ship :target-bearing)))

  (it "handles weapon number"
    (let [new-world (process-events [{:event :weapon-number :value 2}] @world)]
      (should= 2 (:weapon-number-setting (:ship new-world)))))

  (it "handles weapon spread"
    (should= 15
             (->> @world
                  (process-events [{:event :weapon-spread :value 15}])
                  :ship :weapon-spread-setting)))

  (it "fires one phaser"
    (let [ship (assoc @ship :x 100 :y 200 :selected-weapon :phaser
                            :weapon-number-setting 1 :target-bearing 90 :weapon-spread-setting 0)
          world (assoc @world :ship ship)
          new-world (process-events [{:event :weapon-fire}] world)]
      (should= [{:x 100 :y 200 :bearing 90 :range 0 :type :phaser :corbomite false}]
               (:shots new-world)))))

(describe "updating the world"
  (with world (mom/make-world))
  (with ship (mom/make-ship))
  (it "rotates towards heading"
    (let [ship (assoc @ship :heading-setting 90 :heading 70)
          world (assoc @world :ship ship)
          world (update-world 1000 world)]
      (should= 90 (int (:heading (:ship world))))))

  (it "impulse moves ship"
    (let [ship (assoc @ship :impulse 1 :y 1000)
          world (assoc @world :ship ship)
          world (update-world 1000 world)]
      (should> (first (:velocity (:ship world))) 0)))

  (it "warp charges warp field"
    (let [ship (assoc @ship :warp 1)
          world (assoc @world :ship ship)
          world (update-world 1000 world)]
      (should= 2000 (int (:warp-charge (:ship world)))))))

(describe "frame rate"
  (it "adds to empty frame time list"
    (should= [10] (:frame-times (add-frame-time 10 {:frame-times []}))))

  (it "accumulates up to 10 times"
    (let [context (reduce #(add-frame-time %2 %1)
                          {:frame-times []}
                          [1 2 3 4 5 6 7 8 9 10 11 12])]
      (should= [3 4 5 6 7 8 9 10 11 12] (:frame-times context))))

  (it "calculates frame rates"
    (should= 0 (frames-per-second []))
    (should= 1 (frames-per-second [1000]))
    (should= 22 (int (frames-per-second [30 40 50 60])))))