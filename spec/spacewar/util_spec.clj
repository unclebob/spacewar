(ns spacewar.util-spec
  (:require [speclj.core :refer [describe it should=]]
            [spacewar.util :refer [get-event]]))

(describe "events"
  (it "returns nil for degenerate event list"
    (should= nil (get-event :my-event [])))

  (it "returns nil when event not found"
    (should= nil (get-event :my-event [{:event :x} {:event :y}])))

  (it "can find event in list of one event"
    (should= {:event :my-event}
             (get-event :my-event [{:event :my-event}])))

  (it "can find event in list of many events"
    (should= {:event :my-event}
             (get-event :my-event [{:event :x}
                                  {:event :y}
                                  {:event :my-event}]))))
