(ns gomoku.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [gomoku.events]
            [gomoku.subs]
            [gomoku.views :as views]))

(defonce timer-interval (atom nil))

(defn start-timer! []
  (when-let [old @timer-interval]
    (js/clearInterval old))
  (reset! timer-interval (js/setInterval #(rf/dispatch [:tick]) 1000)))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (rdom/render [views/app] (.getElementById js/document "app")))

(defn init []
  (rf/dispatch-sync [:initialize-db])
  (start-timer!)
  (mount-root))
