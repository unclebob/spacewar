(ns spacewar.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [spacewar.ui.complex :as main-viewer]
            [spacewar.ui.protocols :as p]
            [spacewar.game-logic.ship :as ship]
            [spacewar.game-logic.stars :as stars]
            [spacewar.game-logic.klingons :as klingons]))

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
                     :ship (ship/initialize)}}
     :fonts {:lcars (q/create-font "Helvetica-Bold" 24)
             :lcars-small (q/create-font "Arial" 18)}}))

(defn make-commands [events]
  (let [commands
        (filter some?
                (for [e events]
                  (condp = (:event e)
                    :strategic-scan {:command :strategic-scan}
                    :tactical-scan {:command :tactical-scan}
                    :engine-direction {:command :set-engine-direction :angle (:angle e)}
                    :weapon-direction {:command :set-weapon-direction :angle (:angle e)}
                    :engine-power {:command :set-engine-power :power (:value e)}
                    :weapon-number {:command :set-weapon-number :number (:value e)}
                    :weapon-spread {:command :set-weapon-spread :spread (:value e)}
                    nil)))]
    commands))

(defn make-global-state [_ global-state]
  global-state)

(defn update-state [context]
  (let [state (:state context)
        commands-and-state (:commands-and-state context)
        global-state (:global-state commands-and-state)
        [new-drawable events] (p/update-state state commands-and-state)
        flat-events (flatten events)]
    (assoc context
      :state new-drawable
      :commands-and-state {:commands (make-commands flat-events)
                           :global-state (make-global-state
                                           flat-events
                                           global-state)})))

(defn draw-state [{:keys [state]}]
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
