(ns spacewar.ui.protocols)

(defprotocol Drawable
  (draw [this])
  (setup [this])
  (update-state [this])
  (get-state [this]))
