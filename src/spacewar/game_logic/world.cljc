(ns spacewar.game-logic.world)

(defn add-message [world message duration]
  (let [messages (:messages world)
        message {:text message :duration duration}
        messages (conj messages message)]
    (assoc world :messages messages)))