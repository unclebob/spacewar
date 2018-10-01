(ns spacewar.ui.config)

(def white [255 255 255])
(def black [0 0 0])
(def dark-grey [50 50 50])
(def grey [128 128 128])
(def light-grey [200 200 200])
(def yellow [255 255 0])
(def red [255 0 0])
(def klingon-color [200 0 50])
(def enterprise-color [0 255 50])
(def enterprise-vector-color [0 100 20])
(def velocity-vector-scale 1)
(def base-color [0 50 255])

(def scan-panel-color [150 150 255])
(def scan-panel-button-color [100 100 255])
(def scan-panel-selection-color [70 70 200])
(def engine-panel-color [150 255 150])
(def engine-panel-button-color [80 255 80])
(def engine-panel-selection-color [30 200 30])
(def weapons-panel-color [255 200 50])
(def weapons-panel-button-color [255 150 50])
(def weapons-panel-selection-color [200 100 30])
(def damage-panel-color [255 100 100])
(def status-panel-color [255 255 200])
(def status-panel-mercury-color [240 240 0])

(def banner-width 40)
(def stringer-width 15)
(def button-gap 10)
(def button-h 40)
(def slider-width 50)
(def engage-width 100)

(def phaser-length 30)
(def kinetic-color [255 0 0])

(def explosion-duration 1000)

(def phaser-fragment-color-profile
  [{:until 100 :colors [white white]}
   {:until 300 :colors [white yellow]}
   {:until 500 :colors [yellow red]}
   {:until 1000 :colors [red black]}])

(def explosion-profiles {:phaser {:explosion-profile [{:velocity 0.5 :until 100}
                                                      {:velocity -0.05 :until 1000}]
                                  :explosion-color-profile [{:until 100 :colors [white white]}
                                                            {:until 1000 :colors [white black]}]
                                  :fragments 20
                                  :fragment-velocity 0.5
                                  :fragment-color-profile [{:until 100 :colors [white white]}
                                                           {:until 300 :colors [white yellow]}
                                                           {:until 500 :colors [yellow red]}
                                                           {:until 1000 :colors [red black]}]}
                         :torpedo {:explosion-profile [{:velocity 0.5 :until 100}
                                                       {:velocity -0.05 :until 1000}]
                                   :explosion-color-profile [{:until 100 :colors [white white]}
                                                             {:until 1000 :colors [white black]}]
                                   :fragments 20
                                   :fragment-velocity 0.5
                                   :fragment-color-profile [{:until 100 :colors [white white]}
                                                            {:until 300 :colors [white yellow]}
                                                            {:until 500 :colors [yellow red]}
                                                            {:until 1000 :colors [red black]}]}
                         :kinetic {:explosion-profile [{:velocity 0.5 :until 100}
                                                       {:velocity -0.05 :until 1000}]
                                   :explosion-color-profile [{:until 100 :colors [white white]}
                                                             {:until 1000 :colors [white black]}]
                                   :fragments 20
                                   :fragment-velocity 0.5
                                   :fragment-color-profile [{:until 100 :colors [white white]}
                                                            {:until 300 :colors [white yellow]}
                                                            {:until 500 :colors [yellow red]}
                                                            {:until 1000 :colors [red black]}]}})

