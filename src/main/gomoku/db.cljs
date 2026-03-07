(ns gomoku.db)

(def board-size 25)
(def total-cells (* board-size board-size))

(def default-db
  {:game-state           :setup
   :current-player       :blue
   :first-player         nil
   :board                (vec (repeat total-cells nil))
   :history              []
   :redo-stack           []
   :winner               nil
   :winning-cells        #{}
   :last-move-time       nil
   :last-undo-confirm-time nil
   :now                  nil})
