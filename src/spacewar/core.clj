(ns spacewar.core
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(defn setup []
  (q/frame-rate 30)
  (q/color-mode :rgb)
  {})

(defn update-state [state]
  {})

(defn draw-state [state]
  )

(defn -main [& args]
  (q/defsketch space-war
               :title "Space War"
               :size [(q/screen-width) (q/screen-height)]
               :setup setup
               :update update-state
               :draw draw-state
               :features [:keep-on-top]
               :middleware [m/fun-mode]))
