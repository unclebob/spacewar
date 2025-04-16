(ns spacewar.ui.messages
  (:require [spacewar.game-logic.config :as glc]
            [spacewar.ui.view-frame :as view-frame]))

(def last-message (atom nil))

(defn- msg
  ([text]
   (view-frame/add-message! text 4000))
  ([text duration]
   (view-frame/add-message! text duration)))

(defn send-message [message-key value max]
  (condp = message-key
    :life-damage (msg "Life Support Damage.")
    :life-severe (msg "Life Support Damage Severe!")
    :life-critical (msg "Life Support Critical!")
    :hull-damage (msg "Hull Damage.")
    :hull-severe (msg "Hull Damage Severe!")
    :hull-critical (msg "Hull Critical!")
    :sensor-damage (msg "Sensor Damage.")
    :sensor-severe (msg "Sensor Damage Severe!")
    :sensor-critical (msg "Sensors Critical!")
    :impulse-damage (msg "Impulse Damage.")
    :impulse-severe (msg "Impulse Damage Severe!")
    :impulse-critical (msg "Impulse Critical!")
    :warp-damage (msg "Warp Damage.")
    :warp-severe (msg "Warp Damage Severe!")
    :warp-critical (msg "Warp Critical!")
    :weapons-damage (msg "Weapons Damage.")
    :weapons-severe (msg "Weapons Damage Severe!")
    :weapons-critical (msg "Weapons Critical!")
    :shields-charged (msg "Shields Fully Charged.")
    :shields-ready (msg "Shields Battle Ready.")
    :shields-damaged (msg (str "Shields Holding. " (int (* 100 (/ value max))) "%."))
    :shields-severe (msg (str "Shields Weak." (int (* 100 (/ value max))) "%!"))
    :shields-critical (msg "Shields Critical!")
    :antimatter-full (msg "Antimatter Full.")
    :antimatter-topped-off (msg "Antimatter Topped Off.")
    :antimatter-high (msg (str "Antimatter High. " (int (* 100 (/ value max))) "%."))
    :antimatter-low (msg (str "Antimatter Low. " (int (* 100 (/ value max))) "%!"))
    :antimatter-critical (msg "Antimatter Critical!")
    :dilithium-full (msg "Dilithium Full.")
    :dilithium-topped-off (msg "Dilithium Topped Off.")
    :dilithium-high (msg (str "Dilithium High. " (int (* 100 (/ value max))) "%."))
    :dilithium-low (msg (str "Dilithium Low. " (int (* 100 (/ value max))) "%!"))
    :dilithium-critical (msg "Dilithium Critical!")
    :temp-normal (msg "Temperature Normal.")
    :temp-high (msg (str "Temperature High. " (int (* 100 (/ value max))) "%."))
    :temp-critical (msg "Temperature Critical!")
    :temp-severe (msg (str "Temperature Severe. " (int (* 100 (/ value max))) "%!"))
    :torpedos-critical (msg "Torpedos Critical!")
    :torpedos-low (msg (str "Torpedos Low. " value "!"))
    :torpedos-full (msg "Torpedos Full.")
    :torpedos-normal (msg (str "Torpedos Nominal. " value "."))
    :torpedos-high (msg (str "Torpedos High."))
    :kinetics-critical (msg "Kinetics Critical!")
    :kinetics-low (msg (str "Kinetics Low. " value "!"))
    :kinetics-full (msg "Kinetics Full.")
    :kinetics-normal (msg (str "Kinetics Nominal. " value "."))
    :kinetics-high (msg (str "Kinetics High. " value "!"))
    )
  )

(defn item-message [item key max thresholds]
  (if (nil? (key @last-message))
    (swap! last-message assoc key (key item))
    (let [value (key item)
          old-value (key @last-message)
          pct-value (/ value max)
          pct-old-value (/ old-value max)
          pct-diff (abs (- pct-value pct-old-value))]
      (when (> pct-diff 0.1)
        (swap! last-message assoc key value)
        (loop [previous 0
               thresholds thresholds]
          (if (empty? thresholds)
            nil
            (let [[threshold message] (first thresholds)]
              (if (and (<= previous pct-value)
                       (< pct-value threshold)
                       (some? message))
                (send-message message value max)
                (recur threshold (rest thresholds))))))))))

(defn- ship-messages [ship]
  (item-message ship :life-support-damage
                100
                [[0.1 nil]
                 [0.3 :life-damage]
                 [0.8 :life-severe]
                 [1 :life-critical]])
  (item-message ship :hull-damage
                100
                [[0.1 nil]
                 [0.3 :hull-damage]
                 [0.8 :hull-severe]
                 [1 :hull-critical]])
  (item-message ship :sensor-damage
                100
                [[0.1 nil]
                 [0.3 :sensor-damage]
                 [0.8 :sensor-severe]
                 [1 :sensor-critical]])
  (item-message ship :impulse-damage
                100
                [[0.1 nil]
                 [0.3 :impulse-damage]
                 [0.8 :impulse-severe]
                 [1 :impulse-critical]])
  (item-message ship :warp-damage
                100
                [[0.1 nil]
                 [0.3 :warp-damage]
                 [0.8 :warp-severe]
                 [1 :warp-critical]])
  (item-message ship :weapons-damage
                100
                [[0.1 nil]
                 [0.3 :weapons-damage]
                 [0.8 :weapons-severe]
                 [1 :weapons-critical]])
  (item-message ship :shields glc/ship-shields
                [[0.2 :shields-critical]
                 [0.5 :shields-severe]
                 [0.8 :shields-damaged]
                 [0.95 :shields-ready]
                 [1 :shields-charged]])
  (item-message ship :antimatter glc/ship-antimatter
                [[0.2 :antimatter-critical]
                 [0.5 :antimatter-low]
                 [0.8 :antimatter-high]
                 [0.95 :antimatter-full]
                 [1 :antimatter-topped-off]])
  (item-message ship :dilithium glc/ship-dilithium
                [[0.2 :dilithium-critical]
                 [0.5 :dilithium-low]
                 [0.8 :dilithium-high]
                 [0.95 :dilithium-full]
                 [1 :dilithium-topped-off]])
  (item-message ship :core-temp 100
                [[0.1 nil]
                 [0.3 :temp-normal]
                 [0.5 :temp-high]
                 [0.8 :temp-severe]
                 [1 :temp-critical]])
  (item-message ship :torpedos glc/ship-torpedos
                [[0.1 :torpedos-critical]
                 [0.3 :torpedos-low]
                 [0.5 :torpedos-normal]
                 [0.8 :torpedos-high]
                 [1 :torpedos-full]])
  (item-message ship :kinetics glc/ship-kinetics
                [[0.1 :kinetics-critical]
                 [0.3 :kinetics-low]
                 [0.5 :kinetics-normal]
                 [0.8 :kinetics-high]
                 [1 :kinetics-full]])
)

(defn add-messages! [world]
  (ship-messages (:ship world))
  )
