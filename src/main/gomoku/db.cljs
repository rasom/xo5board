(ns gomoku.db)

(def default-board-size 15)
(def board-sizes [15 19 25])

(defn make-board [size]
  (vec (repeat (* size size) nil)))

(def default-db
  {:game-state           :setup
   :current-player       :blue
   :first-player         nil
   :board-size           default-board-size
   :board                (make-board default-board-size)
   :history              []
   :redo-stack           []
   :winner               nil
   :winning-cells        #{}
   :last-move-time       nil
   :last-undo-confirm-time nil
   :now                  nil
   :show-diagonals       false
   :last-move-idx        nil})
