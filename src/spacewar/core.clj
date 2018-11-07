(ns spacewar.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [spacewar.game-logic.config :refer :all]
            [spacewar.ui.complex :as main-viewer]
            [spacewar.ui.view-frame :as view-frame]
            [spacewar.ui.protocols :as p]
            [spacewar.game-logic.world :refer :all]
            [spacewar.game-logic.ship :as ship]
            [spacewar.game-logic.stars :as stars]
            [spacewar.game-logic.klingons :as klingons]
            [spacewar.game-logic.bases :as bases]
            [spacewar.game-logic.shots :as shots]
            [spacewar.game-logic.explosions :as explosions]
            [spacewar.game-logic.clouds :as clouds]
            [spacewar.util :refer :all]
            [clojure.spec.alpha :as s]))

(s/def ::update-time number?)
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
                                ::shots
                                ::update-time
                                ::ms
                                ::messages
                                ::game-over]))

(defn make-initial-world []
  (let [ship (ship/initialize)]
    {:stars (stars/initialize)
     :klingons (klingons/initialize)
     :ship ship
     :bases [(bases/make-base
               [(+ (:x ship) ship-docking-distance -1) (:y ship)]
               :antimatter-factory)]
     :transports []
     :clouds []
     :update-time (q/millis)
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
    (q/frame-rate frame-rate)
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
        cloud (clouds/make-cloud x y 300)
        clouds (conj clouds cloud)]
    (assoc world :clouds clouds)))

(defn- debug-resupply-ship [_ world]
  (let [ship (:ship world)
        ship (assoc ship :antimatter ship-antimatter
                         :dilithium ship-dilithium
                         :torpedos ship-torpedos
                         :kinetics ship-kinetics
                         :shields ship-shields
                         :core-temp 0)]
    (assoc world :ship ship)))

(defn- process-debug-events [events world]
  (let [[_ world] (->> [events world]
                       (handle-event :debug-position-ship debug-position-ship-handler)
                       (handle-event :debug-dilithium-cloud debug-dilithium-cloud-handler)
                       (handle-event :debug-resupply-ship debug-resupply-ship)
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
  (add-message world text 5000))

(defn- shield-message [world]
  (let [ship (:ship world)
        shields (:shields ship)]
    (cond
      (< shields (/ ship-shields 5)) (msg world "Captain! Shields are buckling!")
      (< shields (/ ship-shields 2)) (msg world "Taking Damage sir!")
      (< shields ship-shields) (msg world "Shields Holding sir!")
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
       (view-frame/update-messages ms)
       (add-messages)
       ))

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
        ms (- time (:update-time world))
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
        world (update-world ms world)]
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
               ;:features [:keep-on-top]
               :middleware [m/fun-mode])
  args)
