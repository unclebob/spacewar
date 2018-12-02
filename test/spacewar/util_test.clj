(ns spacewar.util-test
  (:require [midje.sweet :refer [facts fact => ]]
            [spacewar.util :refer [get-event]]))

(facts
  "about events"
  (fact
    "degenerate event list has no events"
    (get-event :my-event []) => nil?)

  (fact
    "event not found"
    (get-event
      :my-event
      [{:event :x} {:event :y}]) => nil?)

  (fact
    "can find event in list of one event"
    (get-event
      :my-event
      [{:event :my-event}]) => {:event :my-event})

  (fact
    "can find event in list of many events"
    (get-event
      :my-event
      [{:event :x}
       {:event :y}
       {:event :my-event}]) => {:event :my-event})
  )



