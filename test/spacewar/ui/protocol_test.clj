(ns spacewar.ui.protocol-test
  (:require [midje.sweet :refer :all]
            [spacewar.ui.protocols :refer :all]))

(deftype mock-drawable [state]
  Drawable
  (draw [_])
  (setup [this] this)
  (update-state [_ _] [(assoc state :updated true)
                       [{:event :e :state state}]])
  (get-state [_] state))

(deftype mock-drawable-no-events [state]
  Drawable
  (draw [_])
  (setup [this] this)
  (update-state [_ _] [(assoc state :updated true) []])
  (get-state [_] state))

(deftype mock-command-handler [state]
  Drawable
  (draw [_])
  (setup [this] this)
  (update-state [_ commands]
    (if (some? (get-command :c (:commands commands)))
      [(assoc state :commanded true) []]
      [state []]))
  (get-state [_] state))

(facts
  "update-elements"
  (fact
    "degenerate"
    (update-elements {} {:commands []
                         :global-state {}}) => [{} []])
  (fact
    "no elements"
    (update-elements {:x 1} {:commands []
                             :global-state {}}) => [{:x 1} []])
  (fact
    "one element"
    (let [mock (->mock-drawable {:y 1})
          state {:x 1 :element mock :elements [:element]}
          [new-state events] (update-elements
                               state
                               {:commands []
                                :global-state {}})]
      new-state => {:x 1
                    :element {:y 1 :updated true}
                    :elements [:element]}
      events => [{:event :e :state {:y 1}}]))
  (fact
    "two elements"
    (let [mock1 (->mock-drawable {:y 1})
          mock2 (->mock-drawable {:y 2})
          state {:x 1
                 :element1 mock1
                 :element2 mock2
                 :elements [:element1 :element2]}
          [new-state events] (update-elements
                               state
                               {:commands []
                                :global-state {}})]
      new-state => {:x 1
                    :element1 {:y 1 :updated true}
                    :element2 {:y 2 :updated true}
                    :elements [:element1 :element2]}
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
          [new-state events] (update-elements
                               state
                               {:commands []
                                :global-state {}})]
      new-state => {:x 1
                    :element1 {:y 1 :updated true}
                    :element2 {:y 2 :updated true}
                    :elements [:element1 :element2]}
      events => []))
  (fact
    "one element with a command"
    (let [mock (->mock-command-handler {:y 1})
          state {:x 1 :element mock :elements [:element]}
          [new-state events] (update-elements
                               state
                               {:commands
                                [{:command :c}]
                                :global-state {}})]
      new-state => {:x 1
                    :element {:y 1 :commanded true}
                    :elements [:element]}
      events => []))

  (fact
    "one element with wrong command"
    (let [mock (->mock-command-handler {:y 1})
          state {:x 1 :element mock :elements [:element]}
          [new-state events] (update-elements
                               state
                               {:commands [{:command :wrong}]
                                :global-state {}})]
      new-state => {:x 1
                    :element {:y 1}
                    :elements [:element]}
      events => []))
  )


(facts
  "about commands"
  (fact
    "degenerate command list has no commands"
    (get-command :my-command []) => nil?)

  (fact
    "command not found"
    (get-command
      :my-command
      [{:command :x} {:command :y}]) => nil?)

  (fact
    "can find command in list of one command"
    (get-command
      :my-command
      [{:command :my-command}]) => {:command :my-command})

  (fact
    "can find command in list of many commands"
    (get-command
      :my-command
      [{:command :x}
       {:command :y}
       {:command :my-command}]) => {:command :my-command})
  )
