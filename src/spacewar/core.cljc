(ns spacewar.core
  (:require [quil.core :as q #?@(:cljs [:include-macros true])]
            [quil.middleware :as m]
            [spacewar.ui.complex :as main-viewer]
            [spacewar.ui.view-frame :as view-frame]
            [spacewar.ui.protocols :as p]
            [spacewar.game-logic.config :as glc]
            [spacewar.game-logic.ship :as ship]
            [spacewar.game-logic.stars :as stars]
            [spacewar.game-logic.klingons :as klingons]
            [spacewar.game-logic.bases :as bases]
            [spacewar.game-logic.shots :as shots]
            [spacewar.game-logic.explosions :as explosions]
            [spacewar.game-logic.clouds :as clouds]
            [spacewar.game-logic.romulans :as romulans]
            [spacewar.util :as util]
            [clojure.spec.alpha :as s]
            #?(:clj [clojure.java.io :as io])
            #?(:cljs [clojure.edn :as edn])))

#?(:cljs (enable-console-print!))

(def version "20250401")

(s/def ::update-time number?)
(s/def ::transport-check-time number?)
(s/def ::shots ::shots/shots)
(s/def ::ms int?)
(s/def ::text string?)
(s/def ::duration int?)
(s/def ::message (s/keys :req-un [::text ::duration]))
(s/def ::game-over-timer int?)
(s/def ::minutes integer?)
(s/def ::version string?)
(s/def ::deaths int?)
(s/def ::klingons-killed int?)
(s/def ::romulans-killed int?)
(s/def ::transport-routes set?)

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
                                ::game-over-timer
                                ::minutes
                                ::version
                                ::deaths
                                ::klingons-killed
                                ::romulans-killed
                                ::transport-routes]))

(defn- add-base [world base]
  (-> world (ship/add-transport-routes-to base)
      (update :bases conj base)))

(defn make-initial-world []
  (let [ship (ship/initialize)
        stars (stars/initialize)
        bases (bases/make-random-bases stars)
        world {:stars stars
               :klingons (klingons/initialize)
               :ship ship
               :bases []
               :transports []
               :clouds []
               :romulans []
               :update-time 0
               :transport-check-time 0
               :explosions []
               :shots []
               :ms 0
               :game-over-timer 0
               :minutes 0
               :version version
               :deaths 0
               :klingons-killed 0
               :romulans-killed 0
               :transport-routes #{}}
        world (reduce #(add-base %1 %2) world bases)]
    (view-frame/clear-messages!)
    (view-frame/add-message! "Welcome to Space War!" 5000)
    (view-frame/add-message! "Save the Federation!" 10000)
    world))


(defn game-saved? []
  #?(:clj  (.exists (io/file "spacewar.world"))
     :cljs (and (exists? js/localStorage)
                (.getItem js/localStorage "spacewar.world"))))

(defn setup []
  (let [vmargin 30
        hmargin 5
        saved? (game-saved?)
        world (if saved?
                #?(:clj  (read-string (slurp "spacewar.world"))
                   :cljs (edn/read-string (.getItem js/localStorage "spacewar.world")))
                (make-initial-world))
        world (if (and saved? (= version (:version world)))
                (do (view-frame/add-message! "Saved game loaded." 10000) world)
                (do (view-frame/add-message! "Saved game ignored, old version." 10000) (make-initial-world)))]
    (q/frame-rate glc/frame-rate)
    (q/color-mode :rgb)
    (q/background 200 200 200)
    (q/ellipse-mode :corner)
    (q/rect-mode :corner)
    {:state (p/setup
              (main-viewer/->complex
                {:x hmargin :y vmargin
                 :w (- (q/width) (* 2 hmargin))
                 :h (- (q/height) (* 2 vmargin))}))
     :world world
     :base-time (:update-time world)
     :fonts #?(:clj  {:lcars (q/create-font "Helvetica-Bold" 24)
                      :lcars-small (q/create-font "Arial" 18)
                      :messages (q/create-font "Bank Gothic" 30)}
               :cljs {:lcars "Helvetica-Bold"               ;; Font names only for JS
                      :lcars-small "Helvetica"
                      :messages "Bank Gothic"})
     :frame-times []})
  )

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
        cloud (clouds/make-cloud x y glc/klingon-debris)
        clouds (conj clouds cloud)]
    (assoc world :clouds clouds)))

(defn- debug-resupply-ship [_ world]
  (let [ship (:ship world)
        ship (assoc ship :antimatter glc/ship-antimatter
                         :dilithium glc/ship-dilithium
                         :torpedos glc/ship-torpedos
                         :kinetics glc/ship-kinetics
                         :shields glc/ship-shields
                         :core-temp 0
                         :hull-damage 0
                         :weapons-damage 0
                         :life-support-damage 0
                         :warp-damage 0
                         :impulse-damage 0
                         :sensor-damage 0)]
    (assoc world :ship ship)))

(defn- debug-add-klingon [event world]
  (println event)
  (let [[x y] (:pos event)
        klingons (:klingons world)
        klingon (klingons/make-klingon x y)
        klingon (update klingon :antimatter / 2)
        klingons (conj klingons klingon)]
    (assoc world :klingons klingons)))

(defn- debug-add-kamikazee-klingon [event world]
  (println event)
  (let [[x y] (:pos event)
        klingons (:klingons world)
        klingon (klingons/make-klingon x y)
        klingon (update klingon :antimatter / 2)
        klingon (assoc klingon :battle-state :kamikazee)
        klingons (conj klingons klingon)]
    (assoc world :klingons klingons)))

(defn- debug-new-klingon-from-praxis [event world]
  (println event)
  (klingons/new-klingon-from-praxis world)
  )

(defn- debug-add-romulan [event world]
  (println event)
  (let [[x y] (:pos event)
        romulans (:romulans world)
        romulan (romulans/make-romulan x y)
        romulans (conj romulans romulan)]
    (assoc world :romulans romulans)))

(defn- new-game [event world]
  (make-initial-world))

(defn- debug-corbomite-device-installed [event world]
  (let [ship (:ship world)
        ship (assoc ship :corbomite-device-installed true)]
    (assoc world :ship ship)))

(defn- debug-explosion [event world]
  (let [[x y] (:pos event)
        explosion (explosions/->explosion :corbomite-device {:x x :y y})
        explosions (:explosions world)
        explosions (conj explosions explosion)]
    (assoc world :explosions explosions)))

(defn- debug-add-pulsar [event world]
  (let [[x y] (:pos event)
        pulsar (stars/make-pulsar)
        pulsar (assoc pulsar :x x :y y)
        stars (:stars world)
        stars (conj stars pulsar)]
    (assoc world :stars stars))
  )

(defn- debug-klingon-stats [_event world]
  (swap! glc/klingon-stats not)
  world)

(defn- process-game-events [events world]
  (let [[_ world] (->> [events world]
                       (util/handle-event :debug-position-ship debug-position-ship-handler)
                       (util/handle-event :debug-dilithium-cloud debug-dilithium-cloud-handler)
                       (util/handle-event :debug-resupply-ship debug-resupply-ship)
                       (util/handle-event :debug-add-klingon debug-add-klingon)
                       (util/handle-event :debug-add-kamikazee-klingon debug-add-kamikazee-klingon)
                       (util/handle-event :debug-add-romulan debug-add-romulan)
                       (util/handle-event :debug-new-klingon-from-praxis debug-new-klingon-from-praxis)
                       (util/handle-event :debug-corbomite-device-installed debug-corbomite-device-installed)
                       (util/handle-event :debug-explosion debug-explosion)
                       (util/handle-event :debug-add-pulsar debug-add-pulsar)
                       (util/handle-event :debug-klingon-stats debug-klingon-stats)
                       (util/handle-event :new-game new-game)
                       )]
    world))

(defn process-events [events world]
  (let [world (ship/process-events events world)
        world (process-game-events events world)
        world (shots/process-events events world)]
    world))

(defn- ship-explosion [ship]
  (explosions/->explosion :ship ship))

(defn- game-won [_ms world]
  (when (zero? (count (:klingons world)))
    (view-frame/add-message! "The Federation is safe!  You win!" 1000000))
  world)

(defn- game-over [_ms {:keys [ship game-over-timer explosions deaths] :as world}]
  (if (:destroyed ship)
    (let [explosions (if (zero? game-over-timer)
                       (conj explosions (ship-explosion ship))
                       explosions)
          _ (when (zero? game-over-timer)
              (view-frame/add-message! "You Died!" 10000))
          done? (and
                  (pos? game-over-timer)
                  (empty? explosions))
          game-over-timer (if done? 0 1)
          ship (if done? (ship/reincarnate) ship)
          deaths (if done? (inc deaths) deaths)
          ]
      (assoc world :game-over-timer game-over-timer
                   :explosions explosions
                   :ship ship
                   :deaths deaths))
    world))

(defn- valid-world? [world]
  (let [valid (s/valid? ::world world)]
    (when (not valid)
      (println (s/explain-str ::world world)))
    valid))

(defn- msg [text]
  (view-frame/add-message! text 5000))

(defn- shield-message [world]
  (let [ship (:ship world)
        shields (:shields ship)]
    (cond
      (< shields (/ glc/ship-shields 5)) (msg "Captain! Shields are buckling!")
      (< shields (/ glc/ship-shields 2)) (msg "Taking Damage sir!")
      (< shields glc/ship-shields) (msg "Shields Holding sir!"))
    world))

(defn- add-messages [world]
  (let [message-time (and (not (->> world :ship :destroyed))
                          (> 1 (rand 200)))]
    (when message-time
      (->> world (shield-message)))
    world))

(defn update-world [ms world]
  ;{:pre [(valid-world? world)]
  ; :post [(valid-world? %)]}
  (->> world
       (game-won ms)
       (game-over ms)
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

(defn- increment-minutes [world]
  (let [minutes (get world :minutes 0)]
    (assoc world :minutes (inc minutes))))

(defn update-world-per-minute [world]
  (->> world
       (increment-minutes)
       (klingons/update-klingons-per-minute)))

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
  (let [{:keys [world base-time]} context
        time (+ base-time (q/millis))
        last-update-time (:update-time world)
        ms (- time last-update-time)
        new-game? (> ms 500)
        last-update-time (if new-game?
                           time
                           last-update-time)
        ms (- time last-update-time)
        ms (max 1 ms)                                       ;zero or negative values imply a game restart or new game.
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
        new-minute? (not= (int (/ time 60000)) (int (/ last-update-time 60000)))
        new-save? (not= (int (/ time 5000)) (int (/ last-update-time 5000)))

        world (if new-second?
                (update-world-per-second world)
                world)
        world (if new-minute?
                (update-world-per-minute world)
                world)]
    (when new-save?
      #?(:clj  (spit "spacewar.world" world)
         :cljs (when (exists? js/localStorage)
                 (.setItem js/localStorage "spacewar.world" world))))
    (assoc context
      :state complex
      :world world)))

(defn draw-state [{:keys [state]}]
  (q/fill 200 200 200)
  (q/rect-mode :corner)
  (q/no-stroke)
  (q/rect 0 0 (q/width) (q/height))
  (p/draw state)
  )

#?(:clj
   (defn ^:export -main [& args]
     (q/defsketch space-war
                  :title "Space War"
                  :size [(- (q/screen-width) 10) (- (q/screen-height) 40)]
                  :setup setup
                  :update update-state
                  :draw draw-state
                  :middleware [m/fun-mode]
                  )
     args)

   :cljs
   (defn ^:export -main []
     (let [size [(max (- (.-scrollWidth (.-body js/document)) 20) 900)
                 (max (- (.-innerHeight js/window) 25) 700)]]
       (q/defsketch space-war
                    :title "Space War"
                    :size size
                    :setup setup
                    :update update-state
                    :draw draw-state
                    :middleware [m/fun-mode]
                    :host "space-war"
                    )

       )
     )
   )