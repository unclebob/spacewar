(ns spacewar.ui.protocol-spec
  (:require [spacewar.ui.protocols :refer [change-elements
                                           get-state
                                           update-elements
                                           ]]
            [speclj.core :refer [context describe it should=]])
  (:import (spacewar.ui.protocols Drawable)))

(deftype MockDrawable [state]
  Drawable
  (draw [_])
  (setup [this] this)
  (update-state [_ _] [(MockDrawable. (assoc state :updated true))
                       [{:event :e :state state}]])
  (get-state [_] state))

(deftype MockDrawableNoEvents [state]
  Drawable
  (draw [_])
  (setup [this] this)
  (update-state [_ _] [(MockDrawableNoEvents. (assoc state :updated true)) []])
  (get-state [_] state))

(describe "update-elements"
  (it "handles degenerate case"
    (should= [{} []] (update-elements {} {})))

  (it "handles no elements"
    (should= [{:x 1} []] (update-elements {:x 1} {})))

  (context "with elements"
    (it "updates one element"
      (let [mock (->MockDrawable {:y 1})
            state {:x 1 :element mock :elements [:element]}
            [new-state events] (update-elements state {})
            element (:element new-state)]
        (should= {:y 1 :updated true} (get-state element))
        (should= [{:event :e :state {:y 1}}] events)))

    (it "updates two elements"
      (let [mock1 (->MockDrawable {:y 1})
            mock2 (->MockDrawable {:y 2})
            state {:x 1
                   :element1 mock1
                   :element2 mock2
                   :elements [:element1 :element2]}
            [new-state events] (update-elements state {})
            element1 (:element1 new-state)
            element2 (:element2 new-state)]
        (should= {:y 1 :updated true} (get-state element1))
        (should= {:y 2 :updated true} (get-state element2))
        (should= [{:event :e :state {:y 1}}
                  {:event :e :state {:y 2}}] events)))

    (it "updates two elements with no events"
      (let [mock1 (->MockDrawableNoEvents {:y 1})
            mock2 (->MockDrawableNoEvents {:y 2})
            state {:x 1
                   :element1 mock1
                   :element2 mock2
                   :elements [:element1 :element2]}
            [new-state events] (update-elements state {})
            element1 (:element1 new-state)
            element2 (:element2 new-state)]
        (should= {:y 1 :updated true} (get-state element1))
        (should= {:y 2 :updated true} (get-state element2))
        (should= [] events)))))

(deftype TestDrawable [state]
  Drawable
  (draw [_])
  (setup [this] this)
  (update-state [this _] this)
  (get-state [_] state)
  (clone [_ clone-state] (TestDrawable. clone-state)))

(describe "change-elements"
  (it "changes multiple elements"
    (let [element1 (TestDrawable. {:attr1 0})
          element2 (TestDrawable. {:attr2 0})
          container {:element1 element1 :element2 element2}
          changed (change-elements container [[:element1 :attr1 1]
                                              [:element2 :attr2 2]])]
      (should= 1 (-> changed :element1 get-state :attr1))
      (should= 2 (-> changed :element2 get-state :attr2)))))