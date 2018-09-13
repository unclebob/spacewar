(ns spacewar.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [spacewar.ui.main-viewscreen :as main-viewer]
            [spacewar.ui.protocols :as p]))

(defn setup []
  (let [vmargin 50 hmargin 5]
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
     :commands []
     :fonts {:lcars (q/create-font "Helvetica-Bold" 24)}}))

(defn make-commands [events]
  (filter some? (for [e events]
    (condp = (:event e)
              :strategic-scan {:command :strategic-scan}
              :tactical-scan {:command :tactical-scan}
           nil))))

(defn update-state [context]
  (let [state (:state context)
        commands (:commands context)
        [new-drawable events] (p/update-state state commands)]
    (assoc context :state new-drawable :commands (make-commands (flatten events)))))

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
