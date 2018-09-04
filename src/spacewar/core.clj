(ns spacewar.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [spacewar.ui.main-viewscreen :as mv]
            [spacewar.ui.protocols :as p]))

(defn setup []
  (let [vmargin 50 hmargin 20]
    (q/frame-rate 30)
    (q/color-mode :rgb)
    {:state (mv/->frame {:x hmargin :y vmargin
                         :w (- (q/width) (* 2 hmargin))
                         :h (- (q/height) (* 2 vmargin))})}))

(defn update-state [{:keys [state]}]
  {:state (p/update-state state)})

(defn draw-state [{:keys [state]}]
  (p/draw state))

(defn -main [& args]
  (q/defsketch space-war
               :title "Space War"
               :size [(- (q/screen-width) 10) (- (q/screen-height) 40)]
               :setup setup
               :update update-state
               :draw draw-state
               :features [:keep-on-top]
               :middleware [m/fun-mode]))
