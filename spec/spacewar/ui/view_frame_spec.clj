(ns spacewar.ui.view-frame-spec
  (:require [spacewar.game-logic.spec-mother :as mom]
            [spacewar.ui.view-frame :refer [add-message!
                                            message-queue
                                            update-messages]]
            [speclj.core :refer [before context describe it should=]]))

(describe "messages"
  (let [world (mom/make-world)]
    (context "message handling"
      (before (reset! message-queue []))

      (it "can add messages to the queue"
        (add-message! "message" 99)
        (should= {:text "message" :duration 99} (first @message-queue))
        (should= 1 (count @message-queue)))

      (it "can add multiple messages to the queue"
        (add-message! "message1" 99)
        (add-message! "message2" 88)
        (should= {:text "message1" :duration 99} (first @message-queue))
        (should= {:text "message2" :duration 88} (second @message-queue))
        (should= 2 (count @message-queue)))

      (it "ages messages over time"
        (add-message! "message" 100)
        (update-messages 30 world)
        (should= {:text "message" :duration 70} (first @message-queue))
        (should= 1 (count @message-queue)))

      (it "expires messages"
        (add-message! "message" 1)
        (update-messages 1 world)
        (should= 0 (count @message-queue))))))