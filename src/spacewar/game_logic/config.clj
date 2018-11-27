(ns spacewar.game-logic.config)

(def frame-rate 30)

(def spectral-classes [:o :b :a :f :g :k :m])
(def drag-factor 0.005)
(def rotation-rate 0.01)                                    ; degrees per millisecond.
(def impulse-thrust 0.0001)                                 ; per millisecond per power.
(def impulse-power 0.01)
(def warp-power 0.01)
(def warp-leap 10000)                                       ;spacial coordinates.
(def warp-charge-rate 1)
(def warp-threshold 2000)
(def ship-shields 1000)
(def ship-antimatter 100000)
(def ship-dilithium 1000)
(def ship-dilithium-consumption 0.0005)
(def ship-shield-recharge-rate 0.01)
(def ship-shield-recharge-cost 5)
(def ship-docking-distance 2000)
(def ship-deploy-distance 5000)
(def ship-kinetics 500)
(def ship-torpedos 100)
(def ship-repair-capacity 0.005)
(def antimatter-to-heat 0.01)
(def dilithium-heat-dissipation 0.00005)

(def max-shots-by-type {:none 0 :phaser 10 :torpedo 5 :kinetic 20})

(def phaser-range 50000)
(def phaser-velocity 40)                                    ;per ms
(def phaser-proximity 1000)
(def phaser-damage 300)
(def phaser-power 500)

(def torpedo-range 100000)
(def torpedo-velocity 5)
(def torpedo-proximity 2000)
(def torpedo-damage 100)
(def torpedo-power 200)

(def kinetic-range 1000000)
(def kinetic-velocity 6)
(def kinetic-proximity 500)
(def kinetic-damage 50)
(def kinetic-power 20)

(def number-of-stars 1000)
(def number-of-klingons 20)
(def tactical-range 200000)
(def strategic-range 1000000)

(def known-space-x 18000000)
(def known-space-y 10000000)

(def klingon-shields 200)
(def klingon-antimatter 50000)
(def klingon-shield-recharge-rate 0.01)
(def klingon-shield-recharge-cost 10)
(def klingon-tactical-range 150000)
(def klingon-evasion-limit 50000)
(def klingon-battle-state-transition-age 20000)
(def klingon-battle-states [:advancing
                            :retreating
                            :flank-left
                            :flank-right])

(def klingon-evasion-trajectories {:flank-left 90
                                   :flank-right 270
                                   :advancing 30
                                   :retreating 210
                                   :no-battle 0})

(def klingon-antimatter-runaway-threshold (* 0.1 klingon-antimatter))
(def klingon-thrust 0.002)
(def klingon-drag 0.999)
(def klingon-debris 1000)

(def klingon-kinetic-range 1000000)
(def klingon-kinetic-firing-distance 150000)
(def klingon-kinetics 500)
(def klingon-torpedos 20)
(def klingon-kinetic-threshold 2000)
(def klingon-kinetic-velocity 6.0)
(def klingon-kinetic-proximity 1000)
(def klingon-kinetic-damage 50)
(def klingon-kinetic-power 100)

(def klingon-phaser-power 200)
(def klingon-phaser-firing-distance 30000)
(def klingon-phaser-threshold 1000)
(def klingon-phaser-velocity 10)
(def klingon-phaser-damage 100)
(def klingon-phaser-proximity 1000)
(def klingon-phaser-range 30000)

(def klingon-torpedo-power 200)
(def klingon-torpedo-firing-distance 60000)
(def klingon-torpedo-threshold 1000)
(def klingon-torpedo-velocity 5)
(def klingon-torpedo-damage 150)
(def klingon-torpedo-proximity 2000)
(def klingon-torpedo-range 100000)

(def romulan-invisible-time 1000)
(def romulan-appearing-time 2000)
(def romulan-visible-time 1000)
(def romulan-firing-time 2000)
(def romulan-fading-time 2000)
(def romulan-appear-odds-per-second 0.001)
(def romulan-appear-distance 40000)
(def romulan-blast-velocity 60)
(def romulan-blast-range 1000000)
(def romulan-blast-damage (* 1.5 ship-shields))

(def transport-range strategic-range)
(def transport-ready 10000)
(def antimatter-cargo-size 10000)
(def dilithium-cargo-size 100)
(def transport-check-period 1000)
(def transport-velocity 10)
(def transport-delivery-range 2000)

(def base-maturity-age 60000)                               ;60000
(def base-deployment-antimatter 30000)
(def base-deployment-dilithium 300)
(def base-antimatter-maximum (* 1.5 ship-antimatter))
(def base-dilithium-maximum (* 1.5 ship-dilithium))
(def base-kinetics-maximum (* 1.5 ship-kinetics))
(def base-torpedos-maximum (* 1.5 ship-torpedos))

(def antimatter-factory-production-rate 0.1)                ;0.1
(def antimatter-factory-sufficient-antimatter antimatter-cargo-size)
(def antimatter-factory-antimatter-reserve 0)
(def antimatter-factory-sufficient-dilithium dilithium-cargo-size)
(def antimatter-factory-dilithium-reserve 0)

(def dilithium-factory-production-rate 0.001)               ;0.001
(def dilithium-factory-dilithium-antimatter-cost 100)
(def dilithium-factory-sufficient-antimatter antimatter-cargo-size)
(def dilithium-factory-antimatter-reserve (* dilithium-factory-dilithium-antimatter-cost
                                             dilithium-cargo-size))
(def dilithium-factory-sufficient-dilithium (/ dilithium-cargo-size 10))
(def dilithium-factory-dilithium-reserve 0)

(def weapon-factory-torpedo-production-rate 0.0001)
(def weapon-factory-kinetic-production-rate 0.001)
(def weapon-factory-torpedo-antimatter-cost 1000)
(def weapon-factory-kinetic-antimatter-cost 100)
(def weapon-factory-torpedo-dilithium-cost 100)

(def weapon-factory-sufficient-antimatter base-antimatter-maximum)
(def weapon-factory-antimatter-reserve ship-antimatter)
(def weapon-factory-sufficient-dilithium base-dilithium-maximum)
(def weapon-factory-dilithium-reserve ship-dilithium)

(def cloud-decay-rate 0.999988)
(def dilithium-harvest-range 3000)
(def dilithium-harvest-rate 0.05)
