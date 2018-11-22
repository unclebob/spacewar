(ns spacewar.ui.config)

(def white [255 255 255])
(def black [0 0 0])
(def dark-grey [50 50 50])
(def grey [128 128 128])
(def light-grey [200 200 200])
(def yellow [255 255 0])
(def dark-yellow [100 100 0])
(def orange [255 150 50])
(def red [255 0 0])
(def dark-red [100 0 0])
(def dark-red [100 0 0])
(def green [0 255 0])
(def blue [0 0 255])
(def klingon-color [200 0 50])
(def enterprise-color [0 255 50])
(def enterprise-vector-color [0 100 20])
(def velocity-vector-scale 20)
(def weapon-factory-color [0 50 255])
(def antimatter-factory-color [255 100 0])
(def dilithium-factory-color [250 200 0])

(def scan-panel-color [150 200 255])
(def scan-panel-button-color [100 150 255])
(def scan-panel-selection-color [70 100 200])
(def engine-panel-color [150 255 150])
(def engine-panel-button-color [80 255 80])
(def engine-panel-selection-color [30 200 30])
(def weapons-panel-color [255 200 50])
(def weapons-panel-button-color [255 150 50])
(def weapons-panel-selection-color [200 100 30])
(def damage-panel-color [255 100 100])
(def deploy-panel-button-color [80 80 255])
(def deploy-panel-color [120 120 255])
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
(def klingon-kinetic-color [255 200 0])

(def phaser-target 500)
(def torpedo-target 700)
(def kinetic-target 900)

(def transport-route-color [0 255 100 64])

(def star-colors {:o [200 200 255]
                  :b [220 220 255]
                  :a [240 240 240]
                  :f [250 250 200]
                  :g [250 250 150]
                  :k [255 200 150]
                  :m [255 150 150]})

(def star-sizes {:o 6
                 :b 5
                 :a 5
                 :f 4
                 :g 4
                 :k 3
                 :m 3})

(def explosion-profiles
  {:phaser {:duration 1000
            :explosion-profile [{:velocity 0.5 :until 100}
                                {:velocity -0.05 :until 1000}]
            :explosion-color-profile [{:until 100 :colors [white white]}
                                      {:until 1000 :colors [white black]}]
            :fragments 20
            :fragment-velocity 0.5
            :fragment-color-profile [{:until 100 :colors [white white]}
                                     {:until 300 :colors [white yellow]}
                                     {:until 500 :colors [yellow red]}
                                     {:until 1000 :colors [red black]}]}
   :torpedo {:duration 2000
             :explosion-profile [{:velocity 0.8 :until 100}
                                 {:velocity 0.6 :until 200}
                                 {:velocity 0.4 :until 300}
                                 {:velocity 0.2 :until 400}
                                 {:velocity 0.1 :until 500}
                                 {:velocity 0.0 :until 700}
                                 {:velocity -0.05 :until 2000}]
             :explosion-color-profile [{:until 100 :colors [white white]}
                                       {:until 400 :colors [white yellow]}
                                       {:until 1500 :colors [yellow dark-red]}
                                       {:until 2000 :colors [dark-red black]}]
             :fragments 50
             :fragment-velocity 0.2
             :fragment-color-profile [{:until 500 :colors [white white]}
                                      {:until 800 :colors [white yellow]}
                                      {:until 1200 :colors [yellow red]}
                                      {:until 2000 :colors [red black]}]}
   :kinetic {:duration 800
             :explosion-profile [{:velocity 0.5 :until 50}
                                 {:velocity -0.05 :until 800}]
             :explosion-color-profile [{:until 100 :colors [white white]}
                                       {:until 300 :colors [white yellow]}
                                       {:until 600 :colors [yellow red]}
                                       {:until 800 :colors [red black]}]
             :fragments 10
             :fragment-velocity 0.1
             :fragment-color-profile [{:until 100 :colors [white white]}
                                      {:until 300 :colors [white yellow]}
                                      {:until 500 :colors [yellow red]}
                                      {:until 800 :colors [red black]}]}

   :klingon {:duration 4000
             :explosion-profile [{:velocity 0.8 :until 100}
                                 {:velocity 0.9 :until 200}
                                 {:velocity 1 :until 400}
                                 {:velocity 0.2 :until 600}
                                 {:velocity 0.1 :until 600}
                                 {:velocity -0.3 :until 800}
                                 {:velocity 1 :until 1000}
                                 {:velocity -0.05 :until 4000}]
             :explosion-color-profile [{:until 100 :colors [white white]}
                                       {:until 700 :colors [white yellow]}
                                       {:until 2500 :colors [yellow dark-red]}
                                       {:until 4000 :colors [dark-red black]}]
             :fragments 100
             :fragment-velocity 0.2
             :fragment-color-profile [{:until 500 :colors [white white]}
                                      {:until 800 :colors [white yellow]}
                                      {:until 2000 :colors [yellow red]}
                                      {:until 4000 :colors [red black]}]}
   :romulan {:duration 4000
             :explosion-profile [{:velocity 0.8 :until 100}
                                 {:velocity 0.9 :until 200}
                                 {:velocity 1 :until 400}
                                 {:velocity 0.2 :until 600}
                                 {:velocity 0.1 :until 600}
                                 {:velocity -0.3 :until 800}
                                 {:velocity 1 :until 1000}
                                 {:velocity -0.05 :until 4000}]
             :explosion-color-profile [{:until 100 :colors [white white]}
                                       {:until 700 :colors [white orange]}
                                       {:until 2500 :colors [orange dark-red]}
                                       {:until 4000 :colors [dark-red black]}]
             :fragments 100
             :fragment-velocity 0.2
             :fragment-color-profile [{:until 500 :colors [white white]}
                                      {:until 800 :colors [white orange]}
                                      {:until 2000 :colors [yellow red]}
                                      {:until 4000 :colors [red black]}]}

   :klingon-kinetic {:duration 800
                     :explosion-profile [{:velocity 0.5 :until 50}
                                         {:velocity -0.03 :until 800}]
                     :explosion-color-profile [{:until 100 :colors [white green]}
                                               {:until 300 :colors [green yellow]}
                                               {:until 600 :colors [yellow red]}
                                               {:until 800 :colors [red black]}]
                     :fragments 20
                     :fragment-velocity 0.2
                     :fragment-color-profile [{:until 100 :colors [grey white]}
                                              {:until 300 :colors [white yellow]}
                                              {:until 500 :colors [yellow red]}
                                              {:until 800 :colors [red black]}]}
   :klingon-phaser {:duration 1000
                    :explosion-profile [{:velocity 0.5 :until 100}
                                        {:velocity -0.05 :until 1000}]
                    :explosion-color-profile [{:until 100 :colors [white white]}
                                              {:until 500 :colors [white green]}
                                              {:until 1000 :colors [green black]}]
                    :fragments 20
                    :fragment-velocity 0.5
                    :fragment-color-profile [{:until 100 :colors [white white]}
                                             {:until 300 :colors [white green]}
                                             {:until 500 :colors [green yellow]}
                                             {:until 1000 :colors [yellow black]}]}
   :klingon-torpedo {:duration 2000
                     :explosion-profile [{:velocity 0.8 :until 100}
                                         {:velocity 0.6 :until 200}
                                         {:velocity 0.4 :until 300}
                                         {:velocity 0.2 :until 400}
                                         {:velocity 0.1 :until 500}
                                         {:velocity 0.0 :until 700}
                                         {:velocity -0.05 :until 2000}]
                     :explosion-color-profile [{:until 100 :colors [white white]}
                                               {:until 400 :colors [white green]}
                                               {:until 1500 :colors [green dark-red]}
                                               {:until 2000 :colors [dark-red black]}]
                     :fragments 50
                     :fragment-velocity 0.2
                     :fragment-color-profile [{:until 500 :colors [white white]}
                                              {:until 800 :colors [white yellow]}
                                              {:until 1200 :colors [yellow red]}
                                              {:until 2000 :colors [red black]}]}
   :romulan-blast {:duration 4000
                   :explosion-profile [{:velocity 0.8 :until 100}
                                       {:velocity 0.9 :until 200}
                                       {:velocity 1 :until 400}
                                       {:velocity 0.2 :until 600}
                                       {:velocity 0.1 :until 600}
                                       {:velocity -0.3 :until 800}
                                       {:velocity 1 :until 1000}
                                       {:velocity -0.05 :until 4000}]
                   :explosion-color-profile [{:until 100 :colors [white white]}
                                             {:until 700 :colors [white blue]}
                                             {:until 2500 :colors [blue orange]}
                                             {:until 2500 :colors [orange dark-red]}
                                             {:until 4000 :colors [dark-red black]}]
                   :fragments 100
                   :fragment-velocity 0.2
                   :fragment-color-profile [{:until 500 :colors [white white]}
                                            {:until 800 :colors [white orange]}
                                            {:until 2000 :colors [yellow red]}
                                            {:until 4000 :colors [red black]}]}

   :ship {:duration 8000
          :explosion-profile [{:velocity 0.8 :until 100}
                              {:velocity 0.9 :until 200}
                              {:velocity 1 :until 400}
                              {:velocity 0.2 :until 600}
                              {:velocity 0.1 :until 600}
                              {:velocity -0.3 :until 800}
                              {:velocity 1 :until 1000}
                              {:velocity -0.01 :until 8000}]
          :explosion-color-profile [{:until 100 :colors [white white]}
                                    {:until 2000 :colors [white yellow]}
                                    {:until 7000 :colors [yellow dark-red]}
                                    {:until 8000 :colors [dark-red black]}]
          :fragments 300
          :fragment-velocity 0.2
          :fragment-color-profile [{:until 500 :colors [white white]}
                                   {:until 2000 :colors [white yellow]}
                                   {:until 4000 :colors [yellow red]}
                                   {:until 8000 :colors [red black]}]}})

