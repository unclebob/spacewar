(ns spacewar.ui.complex
  (:require [quil.core :as q #?@(:cljs [:include-macros true])]
            [spacewar.ui.protocols :as p]
            [spacewar.ui.view-frame :as f]
            [spacewar.ui.config :as uic]
            [spacewar.ui.control-panels.scan-panel :refer [->scan-panel]]
            [spacewar.ui.control-panels.engine-panel :refer [->engine-panel]]
            [spacewar.ui.control-panels.weapons-panel :refer [->weapons-panel]]
            [spacewar.ui.control-panels.damage-panel :refer [->damage-panel]]
            [spacewar.ui.control-panels.status-panel :refer [->status-panel]]
            [spacewar.ui.control-panels.deploy-panel :refer [->deploy-panel]]
            [spacewar.ui.widgets.lights :refer [->indicator-light
                                                rectangle-light
                                                round-light]]))

(defn draw-light-panel [state]
  (let [{:keys [x y w h indicators background]} state]
    (apply q/fill background)
    (q/no-stroke)
    (q/rect-mode :corner)
    (q/rect x y w h)
    (doseq [indicator indicators] (p/draw indicator))))

(defn update-light-panel [state]
  (let [{:keys [indicators level-func]} state
        indicator-states (map p/get-state indicators)
        new-indicators (map-indexed #(->indicator-light (assoc %2 :level (level-func %1))) indicator-states)
        new-state (assoc state :indicators new-indicators)]
    new-state))

(defn shift-pattern [n i]
  (if (= i (rem (quot (q/frame-count) 3) n)) 1 0))

(defn random-pattern [_ _]
  (rand-int 2))

(defn build-indicators [state]
  (let [{:keys [x y w h rows columns gap indicator-height indicator-width draw-func colors]} state
        cell-width (/ (- w gap gap) columns)
        cell-height (/ (- h gap gap) rows)
        cell-x-offset (- (/ cell-width 2) (/ indicator-width 2))
        cell-y-offset (- (/ cell-height 2) (/ indicator-height 2))
        indicators (for [row (range rows) column (range columns)]
                     (p/setup
                       (->indicator-light
                         {:x (+ x gap cell-x-offset (* cell-width column))
                          :y (+ y gap cell-y-offset (* cell-height row))
                          :w indicator-width
                          :h indicator-height
                          :level 0
                          :draw-func draw-func
                          :colors colors})))]
    indicators))

(deftype bottom-lights [state]
  p/Drawable
  (draw [_] (draw-light-panel state))

  (setup [_]
    (let [rows 1
          columns 14
          indicators (build-indicators
                       (assoc state :rows rows
                                    :columns columns
                                    :gap 20
                                    :indicator-height 10
                                    :indicator-width 20
                                    :draw-func rectangle-light
                                    :colors [[50 50 50] [255 255 255]]))
          new-state (assoc state :indicators indicators
                                 :level-func (partial shift-pattern (* rows columns))
                                 :background [150 150 150])]
      (bottom-lights. new-state)))

  (update-state [_ _] (p/pack-update (bottom-lights. (update-light-panel state)))))

(deftype side-lights [state]
  p/Drawable
  (draw [_] (draw-light-panel state))

  (setup [_]
    (let [rows 10
          columns 2
          indicators (build-indicators
                       (assoc state :rows rows
                                    :columns columns
                                    :gap 20
                                    :indicator-height 15
                                    :indicator-width 15
                                    :draw-func round-light
                                    :colors [[50 50 50] [255 255 0]]))
          new-state (assoc state :indicators indicators
                                 :level-func (partial random-pattern (* rows columns))
                                 :background [150 50 50])]
      (side-lights. new-state)))

  (update-state [this _]
    (p/pack-update
      (if (zero? (rand-int 15))
        (side-lights. (update-light-panel state))
        this))))

(deftype complex [state]
  p/Drawable
  (draw [_]
    (p/draw-elements state)
    (apply q/fill uic/white)
    (q/text-align :left :top)
    (q/text-font (:lcars (q/state :fonts)) 18)
    (let [{:keys [x y fps]} state
          fps (or fps 0)
          fps #?(:clj (format "FPS: %6.2f" (float fps))
                 :cljs (str "FPS: " (.toFixed fps 2)))]
      (q/text fps x y))
    )

  (setup [_]
    (let [{:keys [x y h w]} state
          left-margin 200
          right-margin 200
          bottom-margin 200
          panel-gap 20
          small-panel-gap 10

          frame-width (- w left-margin right-margin)
          frame-height (- h bottom-margin)
          frame-bottom (+ y frame-height)
          frame (p/setup
                  (f/->view-frame {:x (+ x left-margin)
                                   :y y
                                   :h frame-height
                                   :w frame-width}))

          bottom-lights-width (/ frame-width 2)
          bottom-lights-left-offset (/ (- frame-width bottom-lights-width) 2)
          bottom-lights-x (+ x left-margin bottom-lights-left-offset)
          bottom-lights-y (+ y (- h bottom-margin) panel-gap)
          bottom-lights-h 40
          bottom-lights (p/setup
                          (->bottom-lights {:x bottom-lights-x
                                            :y bottom-lights-y
                                            :h bottom-lights-h
                                            :w bottom-lights-width}))

          side-panel-height (/ frame-height 2.5)
          side-panel-width 120
          side-panel-y (+ y (/ frame-height 5))
          left-lights (p/setup
                        (->side-lights {:x (- (+ x left-margin) panel-gap side-panel-width)
                                        :y side-panel-y
                                        :h side-panel-height
                                        :w side-panel-width}))

          right-lights (p/setup
                         (->side-lights {:x (+ x left-margin frame-width panel-gap)
                                         :y side-panel-y
                                         :w side-panel-width
                                         :h side-panel-height}))

          scan-panel-x (+ small-panel-gap x)
          scan-panel-y (+ side-panel-y side-panel-height panel-gap)
          scan-panel-w (- left-margin (* 2 small-panel-gap))
          scan-panel-h (- frame-bottom side-panel-height side-panel-y panel-gap)
          scan-panel (p/setup
                       (->scan-panel {:x scan-panel-x
                                      :y scan-panel-y
                                      :w scan-panel-w
                                      :h scan-panel-h
                                      :name "SCAN"
                                      :color uic/scan-panel-color
                                      :button-color uic/scan-panel-button-color}))

          engine-panel-x scan-panel-x
          engine-panel-y (+ y frame-height small-panel-gap)
          engine-panel-w (+ left-margin bottom-lights-left-offset (- (* 2 small-panel-gap)))
          engine-panel-h (- bottom-margin small-panel-gap)
          engine-panel (p/setup
                         (->engine-panel {:x engine-panel-x
                                          :y engine-panel-y
                                          :w engine-panel-w
                                          :h engine-panel-h
                                          :name "ENGINES"
                                          :color uic/engine-panel-color
                                          :button-color uic/engine-panel-button-color}))

          weapons-panel-x (+ bottom-lights-x bottom-lights-width small-panel-gap)
          weapons-panel-y (+ y frame-height small-panel-gap)
          weapons-panel-w (- w left-margin bottom-lights-left-offset bottom-lights-width panel-gap)
          weapons-panel-h (- bottom-margin small-panel-gap)
          weapons-panel (p/setup
                          (->weapons-panel {:x weapons-panel-x
                                            :y weapons-panel-y
                                            :w weapons-panel-w
                                            :h weapons-panel-h
                                            :name "WEAPONS"
                                            :color uic/weapons-panel-color
                                            :button-color uic/weapons-panel-button-color
                                            :inverted true}))

          damage-panel-x (+ x left-margin frame-width small-panel-gap)
          damage-panel-y scan-panel-y
          damage-panel-w (- right-margin (* 2 small-panel-gap))
          damage-panel (p/setup
                         (->damage-panel {:x damage-panel-x
                                          :y damage-panel-y
                                          :w damage-panel-w
                                          :name "DAMAGE"
                                          :color uic/damage-panel-color
                                          :inverted true}))
          damage-panel-h (:h (p/get-state damage-panel))

          deploy-panel-x damage-panel-x
          deploy-panel-y (+ damage-panel-y damage-panel-h panel-gap)
          deploy-panel-w damage-panel-w
          deploy-panel (p/setup
                         (->deploy-panel {:x deploy-panel-x
                                          :y deploy-panel-y
                                          :w deploy-panel-w
                                          :name "DEPLOY"
                                          :color uic/deploy-panel-color
                                          :button-color uic/deploy-panel-button-color
                                          :inverted true}))
          deploy-panel-h (:h (p/get-state deploy-panel))

          status-panel-x (+ engine-panel-x engine-panel-w small-panel-gap)
          status-panel-y (+ bottom-lights-y bottom-lights-h panel-gap)
          status-panel-w bottom-lights-width
          status-panel-h (- h frame-height panel-gap bottom-lights-h)
          status-panel (p/setup
                         (->status-panel {:x status-panel-x
                                          :y status-panel-y
                                          :w status-panel-w
                                          :h status-panel-h
                                          :name "STATUS"
                                          :color uic/status-panel-color
                                          :mercury-color uic/status-panel-mercury-color}))

          new-state (assoc state :frame frame
                                 :bottom-lights bottom-lights
                                 :left-lights left-lights
                                 :right-lights right-lights
                                 :scan-panel scan-panel
                                 :engine-panel engine-panel
                                 :weapons-panel weapons-panel
                                 :damage-panel damage-panel
                                 :status-panel status-panel
                                 :deploy-panel deploy-panel
                                 :elements [:frame :bottom-lights :left-lights
                                            :right-lights :scan-panel
                                            :engine-panel :weapons-panel
                                            :damage-panel :status-panel
                                            :deploy-panel])]
      (complex. new-state)))

  (update-state [_ world]
    (let [state (assoc state :fps (:fps world))
          [state events] (p/update-elements state world)]
      (p/pack-update
        (complex. state)
        events))))




