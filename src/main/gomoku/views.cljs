(ns gomoku.views
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [gomoku.db :as db]))

(def cell-size 24)
(def border-width 2)

(defn board-px [n] (* n cell-size))
(defn board-total-px [n] (+ (board-px n) (* 2 border-width)))

(def colors
  {:board-bg   "#FBF0D9"
   :grid-line  "#c0b89e"
   :cell-border "#c8b898"
   :blue       "#5a9de0"
   :pink       "#e8728a"
   :highlight  "rgba(255, 223, 130, 0.5)"
   :btn        "#8a8a8a"
   :btn-hover  "#6a6a6a"
   :btn-disabled "#bbb"})

;; --- Confirmation Modal ---

(defn confirmation-modal [message on-confirm on-cancel]
  [:div {:style {:position "fixed" :top 0 :left 0 :right 0 :bottom 0
                 :background "rgba(0,0,0,0.4)" :z-index 1000
                 :display "flex" :align-items "center" :justify-content "center"}
         :on-click (fn [e] (.stopPropagation e) (on-cancel))}
   [:div {:style {:background "white" :border-radius "12px" :padding "24px 32px"
                  :box-shadow "0 4px 20px rgba(0,0,0,0.3)" :text-align "center"
                  :min-width "280px"}
          :on-click (fn [e] (.stopPropagation e))}
    [:p {:style {:font-size "18px" :margin-bottom "20px" :color "#333"}} message]
    [:div {:style {:display "flex" :gap "12px" :justify-content "center"}}
     [:button {:style {:padding "8px 24px" :border "none" :border-radius "6px"
                       :background "#e74c3c" :color "white" :cursor "pointer"
                       :font-size "14px"}
               :on-click (fn [e] (.stopPropagation e) (on-confirm))}
      "Yes"]
     [:button {:style {:padding "8px 24px" :border "1px solid #ccc" :border-radius "6px"
                       :background "white" :color "#333" :cursor "pointer"
                       :font-size "14px"}
               :on-click (fn [e] (.stopPropagation e) (on-cancel))}
      "No"]]]])

;; --- First Player Modal ---

(defn first-player-modal []
  [:div {:style {:position "fixed" :top 0 :left 0 :right 0 :bottom 0
                 :background "rgba(0,0,0,0.4)" :z-index 1000
                 :display "flex" :align-items "center" :justify-content "center"}}
   [:div {:style {:background "white" :border-radius "12px" :padding "32px"
                  :box-shadow "0 4px 20px rgba(0,0,0,0.3)" :text-align "center"
                  :min-width "300px"}}
    [:p {:style {:font-size "20px" :margin-bottom "24px" :color "#333"
                 :font-weight "bold"}}
     "Who goes first?"]
    [:div {:style {:display "flex" :gap "16px" :justify-content "center"}}
     [:button {:style {:padding "12px 32px" :border "none" :border-radius "8px"
                       :background (:blue colors) :color "white" :cursor "pointer"
                       :font-size "16px" :font-weight "bold"}
               :on-click #(rf/dispatch [:select-first-player :blue])}
      "Blue"]
     [:button {:style {:padding "12px 32px" :border "none" :border-radius "8px"
                       :background (:pink colors) :color "white" :cursor "pointer"
                       :font-size "16px" :font-weight "bold"}
               :on-click #(rf/dispatch [:select-first-player :pink])}
      "Pink"]]]])

;; --- Board ---

(defn center-region? [n idx]
  (let [mid (quot n 2)
        row (quot idx n)
        col (rem idx n)]
    (and (<= (- mid 2) row (+ mid 2))
         (<= (- mid 2) col (+ mid 2)))))

(defn center-idx [n]
  (let [mid (quot n 2)]
    (+ (* mid n) mid)))

(defn cell [n idx chip winning? last-move? game-state]
  (let [clickable? (and (= game-state :playing) (nil? chip))
        center?    (= idx (center-idx n))
        bg         (cond
                     winning?                    (:highlight colors)
                     center?                     "rgba(120, 160, 210, 0.45)"
                     (center-region? n idx)      "rgba(120, 160, 210, 0.25)"
                     :else                       "transparent")]
    [:div {:class (when (and last-move? (not winning?)) "last-move")
           :style {:width (str cell-size "px") :height (str cell-size "px")
                   :border (str "1px solid " (:cell-border colors))
                   :display "flex" :align-items "center" :justify-content "center"
                   :cursor (if clickable? "pointer" "default")
                   :background bg}
           :on-click (when clickable? #(rf/dispatch [:place-chip idx]))}
     (when chip
       [:div {:style {:width (str (- cell-size 6) "px")
                      :height (str (- cell-size 6) "px")
                      :border-radius "50%"
                      :background (get colors chip)}}])]))

(defn diagonal-lines-svg [n]
  (let [btp    (board-total-px n)
        center (fn [i] (+ border-width (/ cell-size 2) (* i cell-size)))]
    [:svg {:style {:position "absolute" :top 0 :left 0
                   :width (str btp "px") :height (str btp "px")
                   :pointer-events "none"}
           :view-box (str "0 0 " btp " " btp)}
     (doall
      (for [d (range (- 1 n) n)
            :let [cells (for [r (range n)
                              :let [c (+ r d)]
                              :when (and (>= c 0) (< c n))]
                          [(center c) (center r)])
                  start (first cells)
                  end   (last cells)]
            :when (> (count cells) 1)]
        ^{:key (str "d1-" d)}
        [:line {:x1 (first start) :y1 (second start)
                :x2 (first end) :y2 (second end)
                :stroke "rgba(0,0,0,0.3)" :stroke-width "0.5"}]))
     (doall
      (for [s (range 0 (* 2 (dec n)))
            :let [cells (for [r (range n)
                              :let [c (- s r)]
                              :when (and (>= c 0) (< c n))]
                          [(center c) (center r)])
                  start (first cells)
                  end   (last cells)]
            :when (> (count cells) 1)]
        ^{:key (str "d2-" s)}
        [:line {:x1 (first start) :y1 (second start)
                :x2 (first end) :y2 (second end)
                :stroke "rgba(0,0,0,0.3)" :stroke-width "0.5"}]))]))

(defn board-component []
  (let [scale-ref (r/atom 1)
        prev-size (r/atom nil)]
    (letfn [(calc-scale [n]
              (let [btp (board-total-px n)
                    available-w (- (.-innerWidth js/window) 8)
                    available-h (- (.-innerHeight js/window) 64)
                    sx (/ available-w btp)
                    sy (/ available-h btp)]
                (reset! scale-ref (min sx sy))))]
      (r/create-class
       {:component-did-mount
        (fn [_]
          (let [n @(rf/subscribe [:board-size])]
            (reset! prev-size n)
            (calc-scale n)
            (.addEventListener js/window "resize" #(calc-scale (or @prev-size db/default-board-size)))))
        :component-will-unmount
        (fn [_])
        :reagent-render
        (fn []
          (let [n              @(rf/subscribe [:board-size])
                board          @(rf/subscribe [:board])
                winning-cells  @(rf/subscribe [:winning-cells])
                game-state     @(rf/subscribe [:game-state])
                show-diags?    @(rf/subscribe [:show-diagonals])
                last-move-idx  @(rf/subscribe [:last-move-idx])
                _              (when (not= n @prev-size)
                                 (reset! prev-size n)
                                 (calc-scale n))
                scale          @scale-ref
                btp            (board-total-px n)
                total          (* n n)
                scaled-w       (* btp scale)
                scaled-h       (* btp scale)
                vw             (.-innerWidth js/window)
                left-margin    (max 0 (/ (- vw scaled-w) 2))]
            [:div {:style {:width (str scaled-w "px")
                           :height (str scaled-h "px")
                           :margin-left (str left-margin "px")
                           :margin-bottom "20px"
                           :overflow "hidden"}}
             [:div {:style {:transform (str "scale(" scale ")")
                            :transform-origin "top left"
                            :width (str btp "px")
                            :height (str btp "px")}}
              [:div {:style {:position "relative"}}
               [:div {:style {:display "grid"
                              :grid-template-columns (str "repeat(" n ", " cell-size "px)")
                              :background (:board-bg colors)
                              :border (str "2px solid " (:grid-line colors))}}
                (doall
                 (for [i (range total)]
                   ^{:key i}
                   [cell n i (get board i) (contains? winning-cells i) (= i last-move-idx) game-state]))]
               (when show-diags?
                 [diagonal-lines-svg n])]]]))}))))


;; --- Control Panel ---

(defn format-time [seconds]
  (if (nil? seconds)
    "--"
    (let [m (quot seconds 60)
          s (rem seconds 60)]
      (str m ":" (when (< s 10) "0") s))))

(defn control-panel []
  (let [show-reset-confirm (r/atom false)
        show-undo-confirm  (r/atom false)]
    (fn []
      (let [history      @(rf/subscribe [:history])
            redo-stack   @(rf/subscribe [:redo-stack])
            game-state   @(rf/subscribe [:game-state])
            winner       @(rf/subscribe [:winner])
            elapsed      @(rf/subscribe [:elapsed-time])
            last-undo-t  @(rf/subscribe [:last-undo-confirm-time])
            now          @(rf/subscribe [:now])
            current-player @(rf/subscribe [:current-player])
            board-size   @(rf/subscribe [:board-size])
            has-history? (seq history)
            has-redo?    (seq redo-stack)
            can-resize?  (and (#{:setup :playing} game-state)
                              (empty? history))
            btn-style    (fn [disabled?]
                           {:padding "10px 20px" :border "none" :border-radius "6px"
                            :color "white" :cursor (if disabled? "default" "pointer")
                            :font-size "14px" :font-weight "bold"
                            :min-height "44px"
                            :background (if disabled? (:btn-disabled colors) (:btn colors))
                            :opacity (if disabled? 0.6 1)
                            :touch-action "manipulation"})]
        [:div {:style {:display "flex" :align-items "center" :justify-content "center"
                       :gap "8px" :padding "6px 4px" :flex-wrap "wrap"}}

         ;; Board size selector
         [:select {:style {:padding "6px 8px" :border-radius "6px" :font-size "13px"
                           :border "1px solid #666" :background (if can-resize? "#444" "#333")
                           :color (if can-resize? "#eee" "#777")
                           :cursor (if can-resize? "pointer" "default")
                           :min-height "36px"}
                   :disabled (not can-resize?)
                   :value (str board-size)
                   :on-change (fn [e]
                                (let [size (js/parseInt (.. e -target -value))]
                                  (rf/dispatch [:set-board-size size])))}
          (for [s db/board-sizes]
            ^{:key s}
            [:option {:value (str s)} (str s "x" s)])]

         ;; Reset
         [:button {:style (btn-style false)
                   :on-click #(reset! show-reset-confirm true)}
          "Reset"]

         ;; Undo
         [:button {:style (btn-style (not has-history?))
                   :disabled (not has-history?)
                   :on-click (fn []
                               (if (and last-undo-t now (< (- now last-undo-t) 60000))
                                 (rf/dispatch [:undo])
                                 (reset! show-undo-confirm true)))}
          "Undo"]

         ;; Redo
         (when has-redo?
           [:button {:style (btn-style false)
                     :on-click #(rf/dispatch [:redo])}
            "Redo"])

         ;; Current player indicator
         (when (= game-state :playing)
           [:div {:style {:display "flex" :align-items "center" :gap "4px"
                          :margin-left "6px"}}
            [:div {:style {:width "12px" :height "12px" :border-radius "50%"
                           :background (get colors current-player)}}]
            [:span {:style {:font-size "12px" :color "#999"}} "to move"]])

         ;; Winner message
         (when winner
           [:span {:style {:font-size "14px" :font-weight "bold" :margin-left "6px"
                           :color (get colors winner)}}
            (str (name winner) " wins!")])

         ;; Timer
         [:span {:style {:font-size "12px" :color "#999" :margin-left "6px"}}
          (str "Time: " (format-time elapsed))]

         ;; Diagonals toggle
         (let [show-diags? @(rf/subscribe [:show-diagonals])]
           [:label {:style {:display "flex" :align-items "center" :gap "3px"
                            :margin-left "6px" :font-size "12px" :color "#999"
                            :cursor "pointer" :user-select "none"}}
            [:input {:type "checkbox" :checked show-diags?
                     :on-change #(rf/dispatch [:toggle-diagonals])}]
            "Diag"])

         ;; Reset confirmation modal
         (when @show-reset-confirm
           [confirmation-modal "Are you sure you want to reset?"
            (fn []
              (reset! show-reset-confirm false)
              (rf/dispatch [:reset-game]))
            #(reset! show-reset-confirm false)])

         ;; Undo confirmation modal
         (when @show-undo-confirm
           [confirmation-modal "Are you sure you want to undo?"
            (fn []
              (reset! show-undo-confirm false)
              (rf/dispatch [:confirm-undo]))
            #(reset! show-undo-confirm false)])]))))

;; --- App ---

(defn app []
  (let [game-state @(rf/subscribe [:game-state])]
    [:div {:style {:text-align "center" :padding "4px"
                   :height "100vh" :overflow "hidden" :background "#222222"}}
     [control-panel]
     [board-component]
     (when (= game-state :setup)
       [first-player-modal])]))
