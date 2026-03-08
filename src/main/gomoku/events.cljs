(ns gomoku.events
  (:require [re-frame.core :as rf]
            [gomoku.db :as db]))

(defn idx->row-col [board-size idx]
  [(quot idx board-size) (rem idx board-size)])

(defn row-col->idx [board-size row col]
  (when (and (<= 0 row (dec board-size))
             (<= 0 col (dec board-size)))
    (+ (* row board-size) col)))

(defn find-line
  [board board-size row col color dr dc]
  (let [extend (fn [r c step-r step-c]
                 (loop [r r c c cells []]
                   (let [idx (row-col->idx board-size r c)]
                     (if (and idx (= (get board idx) color))
                       (recur (+ r step-r) (+ c step-c) (conj cells idx))
                       cells))))
        forward  (extend (+ row dr) (+ col dc) dr dc)
        backward (extend (- row dr) (- col dc) (- dr) (- dc))
        center   (row-col->idx board-size row col)]
    (into #{center} (concat forward backward))))

(defn check-win
  [board board-size idx]
  (let [[row col] (idx->row-col board-size idx)
        color     (get board idx)
        directions [[0 1] [1 0] [1 1] [1 -1]]]
    (some (fn [[dr dc]]
            (let [line (find-line board board-size row col color dr dc)]
              (when (>= (count line) 5)
                line)))
          directions)))

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   db/default-db))

(rf/reg-event-db
 :tick
 (fn [db _]
   (assoc db :now (.now js/Date))))

(rf/reg-event-db
 :toggle-diagonals
 (fn [db _]
   (update db :show-diagonals not)))

(rf/reg-event-db
 :set-board-size
 (fn [db [_ size]]
   (assoc db
          :board-size size
          :board (db/make-board size)
          :history []
          :redo-stack []
          :winner nil
          :winning-cells #{}
          :last-move-idx nil
          :last-move-time nil)))

(rf/reg-event-db
 :select-first-player
 (fn [db [_ player]]
   (let [size (:board-size db)]
     (assoc db
            :game-state :playing
            :first-player player
            :current-player player
            :board (db/make-board size)
            :history []
            :redo-stack []
            :winner nil
            :winning-cells #{}
            :last-move-time nil
            :last-undo-confirm-time nil))))

(rf/reg-event-db
 :reset-game
 (fn [db _]
   (let [size (:board-size db)]
     (assoc db
            :game-state :setup
            :current-player :blue
            :first-player nil
            :board (db/make-board size)
            :history []
            :redo-stack []
            :winner nil
            :winning-cells #{}
            :last-move-idx nil
            :last-move-time nil
            :last-undo-confirm-time nil))))

(defn other-player [p]
  (if (= p :blue) :pink :blue))

(rf/reg-event-db
 :place-chip
 (fn [db [_ idx]]
   (let [size (:board-size db)]
     (if (and (= (:game-state db) :playing)
              (nil? (get-in db [:board idx])))
       (let [new-board   (assoc (:board db) idx (:current-player db))
             history-entry {:board (:board db) :player (:current-player db) :move-idx (:last-move-idx db)}
             winning-cells (check-win new-board size idx)]
         (cond-> (assoc db
                        :board new-board
                        :history (conj (:history db) history-entry)
                        :redo-stack []
                        :last-move-idx idx
                        :last-move-time (.now js/Date)
                        :now (.now js/Date))
           winning-cells
           (assoc :game-state :game-over
                  :winner (:current-player db)
                  :winning-cells winning-cells)

           (not winning-cells)
           (assoc :current-player (other-player (:current-player db)))))
       db))))

(defn do-undo [db]
  (if (seq (:history db))
    (let [prev       (peek (:history db))
          redo-entry {:board (:board db) :player (:current-player db) :move-idx (:last-move-idx db)}]
      (assoc db
             :board (:board prev)
             :current-player (:player prev)
             :history (pop (:history db))
             :redo-stack (conj (:redo-stack db) redo-entry)
             :game-state :playing
             :winner nil
             :winning-cells #{}
             :last-move-idx (:move-idx prev)
             :last-move-time (.now js/Date)
             :now (.now js/Date)))
    db))

(rf/reg-event-db
 :undo
 (fn [db _]
   (do-undo db)))

(rf/reg-event-db
 :confirm-undo
 (fn [db _]
   (-> (do-undo db)
       (assoc :last-undo-confirm-time (.now js/Date)))))

(rf/reg-event-db
 :redo
 (fn [db _]
   (let [size (:board-size db)]
     (if (seq (:redo-stack db))
       (let [redo-entry    (peek (:redo-stack db))
             history-entry {:board (:board db) :player (:current-player db) :move-idx (:last-move-idx db)}
             new-board     (:board redo-entry)
             changed-idx   (first (keep-indexed
                                   (fn [i [old new]]
                                     (when (and (nil? old) (some? new)) i))
                                   (map vector (:board db) new-board)))
             winning-cells (when changed-idx (check-win new-board size changed-idx))]
         (cond-> (assoc db
                        :board new-board
                        :current-player (:player redo-entry)
                        :redo-stack (pop (:redo-stack db))
                        :history (conj (:history db) history-entry)
                        :last-move-idx (:move-idx redo-entry)
                        :last-move-time (.now js/Date)
                        :now (.now js/Date))
           winning-cells
           (assoc :game-state :game-over
                  :winner (other-player (:player redo-entry))
                  :winning-cells winning-cells)))
       db))))
