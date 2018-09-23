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
    (q/frame-rate 30)
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
                     :bases (bases/initialize)}}
     :fonts {:lcars (q/create-font "Helvetica-Bold" 24)
             :lcars-small (q/create-font "Arial" 18)}}))

(defn process-events [events global-state]
  (let [[ship-commands ship-state] (ship/process-events events (:ship global-state))]
    [(concat ship-commands) (assoc global-state :ship ship-state)]))

(defn update-state [context]
  (let [complex (:state context)
        commands-and-state (:commands-and-state context)
        global-state (:global-state commands-and-state)
        [new-complex events] (p/update-state complex commands-and-state)
        flat-events (flatten events)
        [commands new-global-state] (process-events flat-events global-state)]
    (assoc context
      :state new-complex
      :commands-and-state {:commands commands
                           :global-state new-global-state})))

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
