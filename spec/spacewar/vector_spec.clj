(ns spacewar.vector-spec
  (:require
    [spacewar.spec-utils :as ut]
    [spacewar.vector :as v]
    [speclj.core :refer [describe it should should=]]))


(describe "Vector operations"
  (describe "vector addition"
    (it "adds two vectors correctly"
      (should= [1 1] (v/add [0 0] [1 1]))
      (should= [2 2] (v/add [1 1] [1 1]))))

  (describe "vector subtraction"
    (it "subtracts two vectors correctly"
      (should= [-1 -2] (v/subtract [0 0] [1 2]))))

  (describe "vector multiplication"
    (it "multiplies two vectors correctly"
      (should= [8 15] (v/multiply [2 3] [4 5]))))

  (describe "vector scaling"
    (it "scales a vector by a number"
      (should= [10 10] (v/scale 10 [1 1]))))

  (describe "magnitude"
    (it "computes magnitude correctly"
      (should (ut/roughly= 0 (v/magnitude [0 0])))
      (should (ut/roughly= 1 (v/magnitude [0 1])))
      (should (ut/roughly= 1 (v/magnitude [1 0])))
      (should (ut/roughly= (Math/sqrt 2) (v/magnitude [-1 -1])))))

  (describe "unit vectors"
    (it "computes unit vectors correctly"
      (should= :no-unit-vector (v/unit [0 0]))
      (should (ut/roughly-v [0 1] (v/unit [0 1])))
      (should (ut/roughly-v [1 0] (v/unit [1 0])))
      (should (ut/roughly-v [(/ (Math/sqrt 2) 2) (/ (Math/sqrt 2) 2)] (v/unit [1 1]))))))