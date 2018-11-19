(ns spacewar.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [spacewar.ui.complex :as main-viewer]
            [spacewar.ui.view-frame :as view-frame]
            [spacewar.ui.protocols :as p]
            [spacewar.game-logic.config :as gc]
            [spacewar.game-logic.world :as world]
            [spacewar.game-logic.ship :as ship]
            [spacewar.game-logic.stars :as stars]
            [spacewar.game-logic.klingons :as klingons]
            [spacewar.game-logic.bases :as bases]
            [spacewar.game-logic.shots :as shots]
            [spacewar.game-logic.explosions :as explosions]
            [spacewar.game-logic.clouds :as clouds]
            [spacewar.game-logic.romulans :as romulans]
            [spacewar.util :as util]
            [clojure.spec.alpha :as s]))

(s/def ::update-time number?)
(s/def ::transport-check-time number?)
(s/def ::shots ::shots/shots)
(s/def ::ms int?)
(s/def ::text string?)
(s/def ::duration int?)
(s/def ::message (s/keys :req-un [::text ::duration]))
(s/def ::messages (s/coll-of ::message))
(s/def ::game-over boolean?)

(s/def ::world (s/keys :req-un [::explosions/explosions
                                ::klingons/klingons
                                ::ship/ship
                                ::stars/stars
                                ::bases/bases
                                ::bases/transports
                                ::clouds/clouds
                                ::romulans/romulans
                                ::shots
                                ::update-time
                                ::transport-check-time
                                ::ms
                                ::messages
                                ::game-over]))

(defn make-initial-world []
  (let [ship (ship/initialize)]
    {:stars (stars/initialize)
     :klingons (klingons/initialize)
     :ship ship
     :bases []
     :transports []
     :clouds []
     :romulans []
     :update-time (q/millis)
     :transport-check-time (q/millis)
     :explosions []
     :shots []
     :ms 0
     :messages [{:text "Welcome to Space War!"
                 :duration 5000}
                {:text "Save the Federation!"
                 :duration 10000}]
     :game-over false}))

(defn setup []
  (let [vmargin 30 hmargin 5]
    (q/frame-rate gc/frame-rate)
    (q/color-mode :rgb)
    (q/background 200 200 200)
    (q/ellipse-mode :corner)
    (q/rect-mode :corner)
    {:state (p/setup
              (main-viewer/->complex
                {:x hmargin :y vmargin
                 :w (- (q/width) (* 2 hmargin))
                 :h (- (q/height) (* 2 vmargin))}))
     :world (make-initial-world)
     :fonts {:lcars (q/create-font "Helvetica-Bold" 24)
             :lcars-small (q/create-font "Arial" 18)
             :messages (q/create-font "Bank Gothic" 24)}
     :frame-times []}))

(defn- debug-position-ship-handler [event world]
  (println "debug-position-ship-handler" (:pos event))
  (let [ship (:ship world)
        [x y] (:pos event)
        ship (assoc ship :x x :y y)]
    (assoc world :ship ship)))

(defn- debug-dilithium-cloud-handler [event world]
  (println event)
  (let [[x y] (:pos event)
        clouds (:clouds world)
        cloud (clouds/make-cloud x y gc/klingon-debris)
        clouds (conj clouds cloud)]
    (assoc world :clouds clouds)))

(defn- debug-resupply-ship [_ world]
  (let [ship (:ship world)
        ship (assoc ship :antimatter gc/ship-antimatter
                         :dilithium gc/ship-dilithium
                         :torpedos gc/ship-torpedos
                         :kinetics gc/ship-kinetics
                         :shields gc/ship-shields
                         :core-temp 0)]
    (assoc world :ship ship)))

(defn- debug-add-klingon [event world]
  (println event)
  (let [[x y] (:pos event)
        klingons (:klingons world)
        klingon (klingons/make-klingon x y)
        klingon (update klingon :antimatter / 2)
        klingons (conj klingons klingon)]
    (assoc world :klingons klingons)))

(defn- debug-add-romulan [event world]
  (println event)
  (let [[x y] (:pos event)
        romulans (:romulans world)
        romulan (romulans/make-romulan x y)
        romulans (conj romulans romulan)]
    (assoc world :romulans romulans)))

(defn- process-debug-events [events world]
  (let [[_ world] (->> [events world]
                       (util/handle-event :debug-position-ship debug-position-ship-handler)
                       (util/handle-event :debug-dilithium-cloud debug-dilithium-cloud-handler)
                       (util/handle-event :debug-resupply-ship debug-resupply-ship)
                       (util/handle-event :debug-add-klingon debug-add-klingon)
                       (util/handle-event :debug-add-romulan debug-add-romulan)
                       )]
    world))

(defn process-events [events world]
  (let [world (ship/process-events events world)
        world (process-debug-events events world)
        world (shots/process-events events world)]
    world))

(defn- ship-explosion [ship]
  (explosions/->explosion :ship ship))

(defn- game-over [world]
  (let [ship (:ship world)
        destroyed (:destroyed ship)
        game-over (:game-over world)
        explosions (:explosions world)
        messages (:messages world)
        game-ending (and destroyed (not game-over))
        game-over destroyed
        explosions (if game-ending
                     (conj explosions (ship-explosion ship))
                     explosions)
        messages (if game-ending
                   (conj messages {:text "Game Over!" :duration 10000000})
                   messages)]
    (assoc world :game-over game-over
                 :explosions explosions
                 :messages messages)
    ))

(defn- valid-world? [world]
  (let [valid (s/valid? ::world world)]
    (when (not valid)
      (println (s/explain-str ::world world)))
    valid))

(defn- msg [world text]
  (world/add-message world text 5000))

(defn- shield-message [world]
  (let [ship (:ship world)
        shields (:shields ship)]
    (cond
      (< shields (/ gc/ship-shields 5)) (msg world "Captain! Shields are buckling!")
      (< shields (/ gc/ship-shields 2)) (msg world "Taking Damage sir!")
      (< shields gc/ship-shields) (msg world "Shields Holding sir!")
      :else world)))

(defn- add-messages [world]
  (let [message-time (and (not (->> world :ship :destroyed))
                          (> 1 (rand 200)))]
    (if message-time
      (->> world (shield-message))
      world)))

(defn update-world [ms world]
  ;{:pre [(valid-world? world)]
  ; :post [(valid-world? %)]}
  (->> world
       (game-over)
       (ship/update-ship ms)
       (shots/update-shots ms)
       (explosions/update-explosions ms)
       (clouds/update-clouds ms)
       (klingons/update-klingons ms)
       (bases/update-bases ms)
       (romulans/update-romulans ms)
       (view-frame/update-messages ms)
       (add-messages)
       ))

(defn update-world-per-second [world]
  (->> world
       (klingons/update-klingons-per-second)
       (romulans/update-romulans-per-second)))

(defn add-frame-time [frame-time context]
  (let [frame-times (->
                      context
                      :frame-times
                      (conj frame-time))
        frame-times (if (> (count frame-times) 10)
                      (vec (rest frame-times))
                      frame-times)]
    (assoc context :frame-times frame-times))
  )

(defn frames-per-second [frame-times]
  (if (empty? frame-times)
    0
    (let [sum (reduce + frame-times)
          mean (/ sum (count frame-times))
          fps (/ 1000 mean)]
      fps)))


(defn update-state [context]
  (let [world (:world context)
        time (q/millis)
        last-update-time (:update-time world)
        ms (- time last-update-time)
        context (add-frame-time ms context)
        frame-times (:frame-times context)
        fps (frames-per-second frame-times)
        complex (:state context)
        world (assoc world :update-time time
                           :ms ms
                           :fps fps)
        [complex events] (p/update-state complex world)
        events (flatten events)
        world (process-events events world)
        world (update-world ms world)
        new-second? (not= (int (/ time 1000)) (int (/ last-update-time 1000)))
        world (if new-second?
                (update-world-per-second world)
                world)]
    (assoc context
      :state complex
      :world world)))

(defn draw-state [{:keys [state]}]
  (q/fill 200 200 200)
  (q/rect-mode :corner)
  (q/no-stroke)
  (q/rect 0 0 (q/width) (q/height))
  (p/draw state))

(declare space-war)
(defn -main [& args]

  (q/defsketch space-war
               :title "Space War"
               :size [(- (q/screen-width) 10) (- (q/screen-height) 40)]
               :setup setup
               :update update-state
               :draw draw-state
               :middleware [m/fun-mode])
  args)
