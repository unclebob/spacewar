(ns spacewar.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [spacewar.game-logic.config :refer :all]
            [spacewar.ui.complex :as main-viewer]
            [spacewar.ui.protocols :as p]
            [spacewar.game-logic.ship :as ship]
            [spacewar.game-logic.stars :as stars]
            [spacewar.game-logic.klingons :as klingons]
            [spacewar.game-logic.bases :as bases]
            [spacewar.game-logic.shots :as shots]
            [spacewar.game-logic.explosions :as explosions]
            [spacewar.util :refer :all]))

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
     :world {:stars (stars/initialize)
             :klingons (klingons/initialize)
             :ship (ship/initialize)
             :bases (bases/initialize)
             :update-time (q/millis)}
     :fonts {:lcars (q/create-font "Helvetica-Bold" 24)
             :lcars-small (q/create-font "Arial" 18)}}))

(defn- add-explosion-debug [event world]
  (let [[x y] (:position event)
        explosions (:explosions world)
        weapon (->> world :ship :selected-weapon)
        ]
    (assoc world :explosions (conj explosions (explosions/->explosion weapon {:x x :y y})))))

(defn- process-debug-events [events world]
  (let [[_ world] (->> [events world]
                       (handle-event :explosion-debug add-explosion-debug)
                       )]
    world)
  )

(defn process-events [events world]
  (let [{:keys [ship]} world
        world (process-debug-events events world)
        world (assoc world :ship (ship/process-events events ship))
        world (shots/process-events events world)]
    world))

(defn update-world [ms world]
  (let [{:keys [ship]} world
        world (assoc world :ship (ship/update-ship ms ship))
        world (shots/update-shots ms world)
        world (explosions/update-explosions ms world)
        world (klingons/update-klingons ms world)]
    world))

(defn update-state [context]
  (let [world (:world context)
        time (q/millis)
        ms (- time (:update-time world))
        complex (:state context)
        world (assoc world :update-time time)
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
               :features [:keep-on-top]
               :middleware [m/fun-mode])
  args)
