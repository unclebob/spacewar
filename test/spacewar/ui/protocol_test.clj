(ns spacewar.ui.protocol-test
  (:require [midje.sweet :refer [facts fact =>]]
            [spacewar.ui.protocols :refer [draw
                                           setup
                                           update-state
                                           get-state
                                           update-elements
                                           change-elements]])
  (:import (spacewar.ui.protocols Drawable)))

(deftype mock-drawable [state]
  Drawable
  (draw [_])
  (setup [this] this)
  (update-state [_ _] [(mock-drawable. (assoc state :updated true))
                       [{:event :e :state state}]])
  (get-state [_] state))

(deftype mock-drawable-no-events [state]
  Drawable
  (draw [_])
  (setup [this] this)
  (update-state [_ _] [(mock-drawable-no-events. (assoc state :updated true)) []])
  (get-state [_] state))

(facts
  "update-elements"
  (fact
    "degenerate"
    (update-elements {} {}) => [{} []])
  (fact
    "no elements"
    (update-elements {:x 1} {}) => [{:x 1} []])
  (fact
    "one element"
    (let [mock (->mock-drawable {:y 1})
          state {:x 1 :element mock :elements [:element]}
          [new-state events] (update-elements state {})
          element (:element new-state)]
      (get-state element) => {:y 1 :updated true}
      events => [{:event :e :state {:y 1}}]))
  (fact
    "two elements"
    (let [mock1 (->mock-drawable {:y 1})
          mock2 (->mock-drawable {:y 2})
          state {:x 1
                 :element1 mock1
                 :element2 mock2
                 :elements [:element1 :element2]}
          [new-state events] (update-elements state {})
          element1 (:element1 new-state)
          element2 (:element2 new-state)]
      (get-state element1) => {:y 1 :updated true}
      (get-state element2) => {:y 2 :updated true}
      events => [{:event :e :state {:y 1}}
                 {:event :e :state {:y 2}}]))

  (fact
    "two elements but no events"
    (let [mock1 (->mock-drawable-no-events {:y 1})
          mock2 (->mock-drawable-no-events {:y 2})
          state {:x 1
                 :element1 mock1
                 :element2 mock2
                 :elements [:element1 :element2]}
          [new-state events] (update-elements state {})
          element1 (:element1 new-state)
          element2 (:element2 new-state)]
      (get-state element1) => {:y 1 :updated true}
      (get-state element2) => {:y 2 :updated true}
      events => []))
  )

(deftype test-drawable [state]
  Drawable
  (draw [_])
  (setup [this] this)
  (update-state [this _] this)
  (get-state [_] state)
  (clone [_ clone-state] (test-drawable. clone-state))
  )

(fact
  "change elements"
  (let [element1 (test-drawable. {:attr1 0})
        element2 (test-drawable. {:attr2 0})
        container {:element1 element1 :element2 element2}
        changed (change-elements container [[:element1 :attr1 1]
                                            [:element2 :attr2 2]])]
    (-> changed :element1 get-state :attr1) => 1
    (-> changed :element2 get-state :attr2) => 2))
