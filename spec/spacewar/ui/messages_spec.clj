(ns spacewar.ui.messages-spec
  (:require
    [spacewar.ui.messages :refer :all]
    [speclj.core :refer :all]))

(declare thresholds)

(focus-describe
  "Messages"
  (with-stubs)
  (before (reset! last-message {:key 0}))
  (with thresholds [[0.2 :first]
                    [0.5 :second]
                    [0.6 nil]
                    [1 :third]])

  (it "sends no message when there's nothing in last-message"
    (reset! last-message {})
    (with-redefs [send-message (stub :send-message)]
      (item-message {:key 0} :key 100 @thresholds)
      (should-not-have-invoked :send-message)))

  (it "sends no message when there has been less than a 10% change"
    (with-redefs [send-message (stub :send-message)]
      (item-message {:key 9} :key 100 @thresholds)
      (should-not-have-invoked :send-message)))

  (it "sends :first message after >10% change"
      (with-redefs [send-message (stub :send-message)]
        (item-message {:key 11} :key 100 @thresholds)
        (should-have-invoked :send-message {:with [:first 11 100]})
        (should= 11 (:key @last-message))))

  (it "sends :second message after >10% change"
        (with-redefs [send-message (stub :send-message)]
          (item-message {:key 49} :key 100 @thresholds)
          (should-have-invoked :send-message {:with [:second 49 100]})))

  (it "sends :third message after >10% change"
        (with-redefs [send-message (stub :send-message)]
          (item-message {:key 99} :key 100 @thresholds)
          (should-have-invoked :send-message {:with [:third 99 100]})))

  (it "sends no message if beyond threshold after >10% change"
        (with-redefs [send-message (stub :send-message)]
          (item-message {:key 101} :key 100 @thresholds)
          (should-not-have-invoked :send-message)
          (should= 101 (:key @last-message))))

  (it "sends no message if message is nil"
         (with-redefs [send-message (stub :send-message)]
           (item-message {:key 59} :key 100 @thresholds)
           (should-not-have-invoked :send-message)
           (should= 59 (:key @last-message))))
  )