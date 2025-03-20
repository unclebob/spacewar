(ns spacewar.ui.view-frame-spec
  (:require [speclj.core :refer [describe context it should= should]]
            [spacewar.game-logic.world :refer [add-message]]
            [spacewar.ui.view-frame :refer [update-messages]]
            [spacewar.game-logic.spec-mother :as mom]))

(describe "messages"
  (let [world (mom/make-world)]
    (context "message handling"
      (it "can add messages to the world"
        (let [new-world (add-message world "message" 99)
              messages (:messages new-world)]
          (should= {:text "message" :duration 99} (first messages))
          (should= 1 (count messages))
          (should (mom/valid-world? new-world))))

      (it "can add multiple messages to the world"
        (let [new-world (-> world
                          (add-message "message1" 99)
                          (add-message "message2" 88))
              messages (:messages new-world)]
          (should= {:text "message1" :duration 99} (first messages))
          (should= {:text "message2" :duration 88} (second messages))
          (should= 2 (count messages))))

      (it "ages messages over time"
        (let [new-world (update-messages 30 (add-message world "message" 100 ))
              messages (:messages new-world)]
          (should= {:text "message" :duration 70} (first messages))
          (should= 1 (count messages))))

      (it "expires messages"
        (let [new-world (update-messages 1 (add-message world "message" 1))
              messages (:messages new-world)]
          (should= 0 (count messages)))))))