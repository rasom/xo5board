(ns gomoku.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub :board (fn [db] (:board db)))
(rf/reg-sub :current-player (fn [db] (:current-player db)))
(rf/reg-sub :game-state (fn [db] (:game-state db)))
(rf/reg-sub :winner (fn [db] (:winner db)))
(rf/reg-sub :winning-cells (fn [db] (:winning-cells db)))
(rf/reg-sub :history (fn [db] (:history db)))
(rf/reg-sub :redo-stack (fn [db] (:redo-stack db)))
(rf/reg-sub :last-move-time (fn [db] (:last-move-time db)))
(rf/reg-sub :last-undo-confirm-time (fn [db] (:last-undo-confirm-time db)))
(rf/reg-sub :now (fn [db] (:now db)))
(rf/reg-sub :show-diagonals (fn [db] (:show-diagonals db)))
(rf/reg-sub :last-move-idx (fn [db] (:last-move-idx db)))

(rf/reg-sub
 :elapsed-time
 :<- [:last-move-time]
 :<- [:now]
 (fn [[last-move now]]
   (if (and last-move now)
     (quot (- now last-move) 1000)
     nil)))
