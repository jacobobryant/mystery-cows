(ns cows.components
  (:require
    [cows.util :as util]
    [rum.core :as rum :refer [defc defcs static reactive react local]]))

(defc text-button < static
  [props & contents]
  (into
    [:span.text-primary (assoc-in props [:style :cursor] "pointer")]
    contents))

(defc user-info < reactive
  [{:db/keys [uid email auth]}]
  [:div
   [:.d-flex.flex-md-row.flex-column
    [:div "Username: " (util/username (react uid))]
    [:.flex-grow-1]
    [:div
     (react email) " ("
     (text-button {:on-click #(.signOut auth)} "sign out") ")"]]
   [:hr.mt-1]])

(defc game-card < static {:key-fn (comp second :ident)}
  [{:m/keys [join-game] :as env} {:keys [players] [_ game-id] :ident}]
  [:.p-3.border.border-dark.rounded.mr-3.mb-3
   [:div "Game ID: " (subs game-id 0 4)]
   [:ul.pl-4
    (for [p players]
      [:li {:key p} (util/username p)])]
   [:button.btn.btn-primary.btn-sm.btn-block
    {:on-click #(join-game env game-id)}
    "Join"]])

(defc game-list < reactive
  [{:keys [m/create-game db/games] :as env}]
  [:div
   [:button.btn.btn-primary {:on-click #(create-game env)}
    "Create game"]
   [:.mt-4]
   [:.d-flex.flex-wrap
    (->> (react games)
      vals
      (map (partial game-card env)))]])

(defc message < static {:key-fn :timestamp}
  [{:keys [user timestamp text]}]
  [:div.mb-2
   [:strong (util/username user)]
   [:.text-muted.small (.toLocaleString timestamp)]
   (map #(if (= "\n" %) [:br] %) text)])

; Of course, the hardest part about making a chat component is rendering it. >:-(
(defcs chat < reactive
  {:did-update (fn [{component :rum/react-component :as s}]
                 (let [node (js/ReactDOM.findDOMNode component)
                       box (.querySelector node ".scroll-box")]
                   (set! (.-scrollTop box) (.-scrollHeight box)))
                 s)}
  (local "" ::text)

  [{::keys [text]} {:keys [db/messages m/send-message db/game-id] :as env}]
  [:.border.rounded.border-primary.d-flex.flex-column
   {:style {:height "600px"
            :overflow "hidden"}}
   [:.flex-grow-1]
   [:.pl-3.scroll-box.border-bottom {:style {:overflow-y "auto"}}
    (map message (react messages))]
   [:textarea.form-control.border-0.flex-shrink-0
    {:placeholder "Enter message"
     :on-key-down #(when (and (= 13 (.-keyCode %))
                           (not (.-shiftKey %)))
                     (send-message env @text)
                     (reset! text "")
                     (.preventDefault %))
     :value @text
     :on-change #(reset! text (.. % -target -value))}]])

(defc game < reactive
  [{:keys [db/current-game m/leave-game] :as env}]
  (let [{:keys [players]
         [_ game-id] :ident} (react current-game)]
    [:.row
     [:.col-md-8
      [:div "Game ID: " (subs game-id 0 4) " ("
       (text-button {:on-click #(leave-game env)} "leave") ")"]
      [:ul.pl-4
       (for [p players]
         [:li {:key p} (util/username p)])]]
     [:.col-md-4
      (chat env)]]))

(defc main < reactive
  [{:keys [db/game-id] :as env}]
  [:div
   [:.container-fluid.mt-2
    (user-info env)
    [:.mt-2]
    (if (react game-id)
      (game env)
      (game-list env))
    [:.mt-3]]])
