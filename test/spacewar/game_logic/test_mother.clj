(ns spacewar.game-logic.test-mother)

(defn make-world []
  {:explosions []
   :ship (make-ship)
   :klingons []
   :stars []
   :bases []
   :update-time 0
   })

(defn make-ship []
  {
   :x 0
   :y 0
   :warp 0
   :warp-charge 0
   :impulse 0
   :heading 0
   :velocity 0
   :selected-view :front-view
   :selected-weapon :none
   :selected-engine :none
   :target-bearing 0
   :engine-power-setting 0
   :weapon-number-setting 1
   :weapon-spread-setting 0
   :heading-setting 0
   :antimatter 0
   :core-temp 0
   :dilithium 0
   :strat-scale 1
   })

(defn make-klingon []
  {
   :x 0
   :y 0
   :shields 0
   :anti-matter 0
   })

(defn set-pos [obj [x y]]
  )

(defn set-ship [ship world]
  )

(defn set-klingons [klingons world])
