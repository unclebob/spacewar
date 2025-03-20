(ns spacewar.ui.widgets-spec
  (:require [speclj.core :refer [describe context it should=]]
            [spacewar.ui.widgets.direction-selector :refer [degree-tick]]
            [spacewar.ui.widgets.horizontal-scale :as scale]
            [spacewar.ui.config :refer [white yellow red]]))

(describe "direction-selector"
  (context "cardinal ticks length 10"
    (it "returns correct coordinates for 0 degrees"
      (should= [0 100 0 90] (degree-tick 100 0)))
    (it "returns correct coordinates for 90 degrees"
      (should= [100 0 90 0] (degree-tick 100 90)))
    (it "returns correct coordinates for 180 degrees"
      (should= [0 -100 0 -90] (degree-tick 100 180)))
    (it "returns correct coordinates for 270 degrees"
      (should= [-100 0 -90 0] (degree-tick 100 270))))

  (context "non-cardinal ticks length 5"
    (it "returns correct coordinates for 45 degrees"
      (should= [71 71 67 67] (degree-tick 100 45)))))

(describe "scale"
  (context "scale colors"
    (it "returns white for value 0"
      (should= white (scale/mercury-color 0 [[10 white] [20 yellow] [30 red]])))
    (it "returns white for value 10"
      (should= white (scale/mercury-color 10 [[10 white] [20 yellow] [30 red]])))
    (it "returns yellow for value 11"
      (should= yellow (scale/mercury-color 11 [[10 white] [20 yellow] [30 red]])))
    (it "returns yellow for value 20"
      (should= yellow (scale/mercury-color 20 [[10 white] [20 yellow] [30 red]])))
    (it "returns red for value 30"
      (should= red (scale/mercury-color 30 [[10 white] [20 yellow] [30 red]])))
    (it "returns red for value 100"
      (should= red (scale/mercury-color 100 [[10 white] [20 yellow] [30 red]])))))