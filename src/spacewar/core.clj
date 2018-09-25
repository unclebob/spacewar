(ns spacewar.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [spacewar.game-logic.config :refer :all]
            [spacewar.ui.complex :as main-viewer]
            [spacewar.ui.protocols :as p]
            [spacewar.game-logic.ship :as ship]
            [spacewar.game-logic.stars :as stars]
            [spacewar.game-logic.klingons :as klingons]
            [spacewar.game-logic.bases :as bases]))

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
     :commands-and-state
     {:commands []
      :global-state {:stars (stars/initialize)
                     :klingons (klingons/initialize)
                     :ship (ship/initialize)
                     :bases (bases/initialize)
                     :update-time (q/millis)
                     :since-last-update 0}}
     :fonts {:lcars (q/create-font "Helvetica-Bold" 24)
             :lcars-small (q/create-font "Arial" 18)}}))

(defn process-events [events global-state]
  (let [ship-state (ship/process-events events global-state)]
    (assoc global-state :ship ship-state)))

(defn update-state [context]
  (let [time (q/millis)
        complex (:state context)
        commands-and-state (:commands-and-state context)
        global-state (:global-state commands-and-state)
        global-state (assoc global-state
                       :update-time time
                       :since-last-update (- time (:update-time global-state)))
        [complex events] (p/update-state complex commands-and-state)
        flat-events (flatten events)
        global-state (process-events flat-events global-state)]
    (assoc context
      :state complex
      :commands-and-state {:global-state global-state})))

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
