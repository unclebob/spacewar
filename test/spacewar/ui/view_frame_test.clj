(ns spacewar.ui.view-frame-test
  (:require [midje.sweet :refer [facts fact =>]]
            [spacewar.game-logic.world :refer [add-message]]
            [spacewar.ui.view-frame :refer [update-messages]]
            [spacewar.game-logic.test-mother :as mom]))

(facts
  "about messages"
  (let [world (mom/make-world)]
    (fact
      "messages can be added to the world."
      (let [new-world (add-message world "message" 99)
            messages (:messages new-world)]
        (first messages) => {:text "message" :duration 99}
        (count messages) => 1
        new-world => mom/valid-world?))

    (fact
      "can add many messages to the world"
      (let [new-world (add-message world "message1" 99)
            new-world (add-message new-world "message2" 88)
            messages (:messages new-world)]
        (first messages) => {:text "message1" :duration 99}
        (second messages) => {:text "message2" :duration 88}
        (count messages) => 2))

    (fact
      "messages age over time"
      (let [new-world (add-message world "message" 100)
            new-world (update-messages 30 new-world)
            messages (:messages new-world)]
        (first messages) => {:text "message" :duration 70}
        (count messages) => 1))

    (fact
      "messages expire"
      (let [new-world (add-message world "message" 1)
                  new-world (update-messages 1 new-world)
                  messages (:messages new-world)]
              (count messages) => 0))
    )
  )

