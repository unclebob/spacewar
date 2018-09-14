(ns spacewar.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [spacewar.ui.main-viewscreen :as main-viewer]
            [spacewar.ui.protocols :as p]
            [spacewar.game-logic.ship :as ship]))

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
      :global-state {:ship (ship/initialize)}}
     :fonts {:lcars (q/create-font "Helvetica-Bold" 24)}}))

(defn make-commands [events]
  (filter some? (for [e events]
                  (condp = (:event e)
                    :strategic-scan {:command :strategic-scan}
                    :tactical-scan {:command :tactical-scan}
                    nil))))

(defn make-global-state [events global-state]
  {})

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
