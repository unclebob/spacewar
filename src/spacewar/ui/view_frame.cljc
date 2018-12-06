(ns spacewar.ui.view-frame
  (:require [quil.core :as q #?@(:cljs [:include-macros true])]
            [spacewar.ui.config :as uic]
            [spacewar.ui.strategic-scan :refer [->strategic-scan]]
            [spacewar.ui.tactical-scan :refer [->tactical-scan]]
            [spacewar.ui.front-view :refer [->front-view]]
            [spacewar.ui.protocols :as p]))

(defn update-messages [ms world]
  (let [messages (:messages world)
        updates (map #(update % :duration - ms) messages)
        updates (filter #(pos? (:duration %)) updates)]
    (assoc world :messages updates)))

(defn- draw-messages [state]
  (let [{:keys [x y w h messages]} state
        message-x (+ x (* 2 (/ w 3)))
        lines (map :text messages)
        text (clojure.string/join "\n" lines)]
    (when (not (empty? messages))
      (apply q/fill (conj uic/green 180))
      (q/text-font (:messages (q/state :fonts)) 24)
      (q/text-align :left :bottom)
      (q/with-translation
        [message-x (+ y h)]
        (q/text text 0 0)))))

(defn fill-outside-rect [x y w h screen-w screen-h rgb]
  (apply q/fill rgb)
  (q/no-stroke)
  (q/rect 0 0 x screen-h)
  (q/rect x 0 w y)
  (q/rect (+ x w) 0 (- screen-w w x) screen-h)
  (q/rect x (+ y h) w (- screen-h h y)))

(deftype view-frame [state]
  p/Drawable
  (draw [_]
    (let [{:keys [x y w h contents]} state]
      (q/no-stroke)
      (q/fill 0 0 255)
      (q/rect-mode :corner)
      (q/rect (- x 5) (- y 5) (+ w 10) (+ h 10))
      (apply q/fill uic/black)
      (q/rect x y w h 5)
      #?(:clj (q/clip x y w h))
      (when (not (:sensor-loss state))
        (p/draw contents))
      #?(:cljs (fill-outside-rect (- x 5) (- y 5) (+ w 10) (+ h 10)
                 (.-width (q/current-graphics)) (.-height (q/current-graphics))
                 uic/light-grey))
      (draw-messages state)
      #?(:clj (q/no-clip))))

  (setup [_] (let [{:keys [x y w h]} state
                   bounds {:x x :y y :h h :w w}
                   front-view (p/setup (->front-view bounds))
                   strat-view (p/setup (->strategic-scan bounds))
                   tact-view (p/setup (->tactical-scan bounds))]
               (view-frame.
                 (assoc state
                   :front-view front-view
                   :strat-view strat-view
                   :tact-view tact-view
                   :last-view :none
                   :contents front-view
                   :elements [:contents]))))

  (update-state [_ world]
    (let [{:keys [last-view]} state
          ship (:ship world)
          selected-view (:selected-view ship)

          state (if (not= selected-view last-view)
                  (assoc state :contents (selected-view state)
                               :last-view selected-view)
                  state)
          sensor-damage (:sensor-damage ship)
          state (assoc state :sensor-loss (and
                                            (> sensor-damage (rand 100))
                                            (not (:destroyed ship)))
                             :messages (:messages world))
          [state events] (p/update-elements state world)]
      (p/pack-update
        (view-frame. state) events))))