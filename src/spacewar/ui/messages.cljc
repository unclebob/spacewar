(ns spacewar.ui.messages
  (:require [spacewar.game-logic.config :as glc]
            [spacewar.ui.view-frame :as view-frame]))

(def last-message (atom nil))

(defn- msg
  ([text]
   (view-frame/add-message! text 4000))
  ([text duration]
   (view-frame/add-message! text duration)))


(defn- damage-message [ship]
  (let [life-support-damage (:life-support-damage ship)
        hull-damage (:hull-damage ship)
        sensor-damage (:sensor-damage ship)
        impulse-damage (:impulse-damage ship)
        warp-damage (:warp-damage ship)
        weapons-damage (:weapons-damage ship)
        old-life-support-damage (get @last-message :life-support-damage 0)
        old-hull-damage (get @last-message :hull-damage 0)
        old-sensor-damage (get @last-message :sensor-damage 0)
        old-impulse-damage (get @last-message :impulse-damage 0)
        old-warp-damage (get @last-message :warp-damage 0)
        old-weapons-damage (get @last-message :weapons-damage 0)
        life-diff (abs (- life-support-damage old-life-support-damage))
        hull-diff (abs (- hull-damage old-hull-damage))
        sensor-diff (abs (- sensor-damage old-sensor-damage))
        impulse-diff (abs (- impulse-damage old-impulse-damage))
        warp-diff (abs (- warp-damage old-warp-damage))
        weapons-diff (abs (- weapons-damage old-weapons-damage))]

    (when (> life-diff 0.1)
      (swap! last-message assoc :life-support-damage life-support-damage)
      (cond (> life-support-damage 0.8)
            (msg "Life Support Critical!")

            (> life-support-damage 0.5)
            (msg "Life Support Seriously Damaged!")

            (> life-support-damage 0.2)
            (msg "Life Support Damaged!")))

    (when (> hull-diff 0.1)
      (swap! last-message assoc :hull-damage hull-damage)
      (cond (> hull-damage 0.8)
            (msg "Hull Critical!")

            (> hull-damage 0.5)
            (msg "Hull Seriously Damaged!")

            (> hull-damage 0.2)
            (msg "Hull Damaged!")))

    (when (> sensor-diff 0.1)
      (swap! last-message assoc :sensor-damage sensor-damage)
      (cond (> sensor-damage 0.8)
            (msg "Sensors Critical!")

            (> sensor-damage 0.5)
            (msg "Sensors Seriously Damaged!")

            (> sensor-damage 0.2)
            (msg "Sensors Damaged!")))

    (when (> impulse-diff 0.1)
      (swap! last-message assoc :impulse-damage impulse-damage)
      (cond (> impulse-damage 0.8)
            (msg "Impulse Drive Critical!")

            (> impulse-damage 0.5)
            (msg "Impulse Drive Seriously Damaged!")

            (> impulse-damage 0.2)
            (msg "Impulse Drive Damaged!")))

    (when (> warp-diff 0.1)
      (swap! last-message assoc :warp-damage warp-damage)
      (cond (> warp-damage 0.8)
            (msg "Warp Drive Critical!")

            (> warp-damage 0.5)
            (msg "Warp Drive Seriously Damaged!")

            (> warp-damage 0.2)
            (msg "Warp Drive Damaged!")))

    (when (> weapons-diff 0.1)
      (swap! last-message assoc :weapons-damage weapons-damage)
      (cond (> weapons-damage 0.8)
            (msg "Weapons Critical!")

            (> weapons-damage 0.5)
            (msg "Weapons Seriously Damaged!")

            (> weapons-damage 0.2)
            (msg "Weapons Damaged!")))
      ))


(defn- shield-message [ship]
  (let [shields (:shields ship)
        old-shields (get @last-message :shields glc/ship-shields)
        pct-old-shields (/ old-shields glc/ship-shields)
        pct-shields (/ shields glc/ship-shields)
        pct-diff (abs (- pct-old-shields pct-shields))]
    (when (> pct-diff 0.1)
      (swap! last-message assoc :shields shields)
      (cond
        (< pct-shields 0.2) (msg "Shields Critical!")
        (< pct-shields 0.5) (msg (str "Shields at " (int (* 100 pct-shields)) "%!"))
        (< pct-shields 0.8) (msg "Shields Holding!")
        (< pct-shields 0.95) (msg "Shields battle ready.")
        :else (msg "Sheilds fully charged.")))))

(defn- resource-message [ship]
  (let [antimatter (:antimatter ship)
        dilithium (:dilithium ship)
        core-temp (:core-temp ship)
        old-antimatter (get @last-message :antimatter glc/ship-antimatter)
        old-dilithium (get @last-message :dilithium glc/ship-dilithium)
        old-core-temp (get @last-message :core-temp 0)
        antimatter-pct (/ antimatter glc/ship-antimatter)
        dilithium-pct (/ dilithium glc/ship-dilithium)
        antimatter-pct-diff (abs (- (/ old-antimatter glc/ship-antimatter) antimatter-pct))
        dilithium-pct-diff (abs (- (/ old-dilithium glc/ship-dilithium) dilithium-pct))
        core-temp-diff (abs (- core-temp old-core-temp))]
    (prn 'antimatter-pct-diff antimatter-pct-diff)
    (when (> antimatter-pct-diff 0.1)
      (swap! last-message  assoc :antimatter antimatter)
      (cond
        (< antimatter-pct 0.2) (msg "Antimatter Critical!")
        (< antimatter-pct 0.5) (msg (str "Antimatter at " (int (* 100 antimatter-pct)) "%!"))
        (< antimatter-pct 0.8) (msg "Antimatter OK.")
        (< antimatter-pct 0.95) (msg "Antimatter nearly full.")
        :else (msg "Antimatter full.")))

    (when (> dilithium-pct-diff 0.1)
      (swap! last-message assoc :dilithium dilithium)
      (cond
        (< dilithium-pct 0.2) (msg "Dilithium Critical!")
        (< dilithium-pct 0.5) (msg (str "Dilithium at " (int (* 100 dilithium-pct)) "%!"))
        (< dilithium-pct 0.8) (msg "Dilithium OK.")
        (< dilithium-pct 0.95) (msg "Dilithium nearly full.")
        :else (msg "Dilithium full.")))

    (when (> core-temp-diff 10)
      (swap! last-message assoc :core-temp core-temp)
      (cond
        (> core-temp 80) (msg "Core Temperature Critical!")
        (> core-temp 50) (msg "Core Temperature High!")
        (> core-temp 20) (msg "Core Temperature Normal!")
        :else (msg "Core Temperature Cool!")))))

(defn- weapons-message [ship]
  (let [torpedos (:torpedos ship)
        kinetics (:kinetics ship)
        old-torpedos (get @last-message :torpedos glc/ship-torpedos)
        old-kinetics (get @last-message :kinetics glc/ship-kinetics)
        torpedos-pct (/ torpedos glc/ship-torpedos)
        kinetics-pct (/ kinetics glc/ship-kinetics)
        torpedos-pct-diff (abs (- (/ old-torpedos glc/ship-torpedos) torpedos-pct))
        kinetics-pct-diff (abs (- (/ old-kinetics glc/ship-kinetics) kinetics-pct))]

    (when (> torpedos-pct-diff 0.1)
      (swap! last-message assoc :torpedos torpedos)
      (cond
        (< torpedos-pct 0.1) (msg "Torpedos Low!")
        (< torpedos-pct 0.3) (msg (str torpedos " Torpedos left!"))
        (< torpedos-pct 0.8) (msg "Torpedos OK.")
        (< torpedos-pct 0.95) (msg (str "Torpedos full " (int (* 100 torpedos-pct)) "%!"))
        :else (msg "Torpedos full.")))

    (when (> kinetics-pct-diff 0.1)
      (swap! last-message assoc :kinetics kinetics)
      (cond
        (< kinetics-pct 0.1) (msg "Kinetics Low!")
        (< kinetics-pct 0.3) (msg (str kinetics " Kinetics left!"))
        (< kinetics-pct 0.8) (msg "Kinetics OK.")
        (< kinetics-pct 0.95) (msg (str "Kinetics full " (int (* 100 kinetics-pct)) "%!"))
        :else (msg "Kinetics full.")))))

(defn add-messages! [world]
  (damage-message (:ship world))
  (shield-message (:ship world))
  (resource-message (:ship world))
  (weapons-message (:ship world)))
