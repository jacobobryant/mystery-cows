(ns cows.core
  (:require
    [cows.components :as c]
    [cows.db :as db :refer [db]]
    [cows.mutations :as m]
    [cows.util :as util]
    [rum.core :as rum :refer [defc defcs reactive static react local]]
    [trident.firestore :refer [subscribe]]))

(defc user-info < reactive
  []
  [:div
   [:.d-flex.flex-md-row.flex-column
    [:div "Username: " (util/username (react db/uid))]
    [:.flex-grow-1]
    [:div
     (react db/email) " ("
     (c/text-button {:on-click #(.. js/firebase auth signOut)} "sign out") ")"]]
   [:hr.mt-1]])

(defc game-list < reactive
  []
  [:div
   [:button.btn.btn-primary {:on-click m/create-game}
    "Create game"]
   [:.mt-4]
   [:.d-flex.flex-wrap
    (for [{[_ game-id] :ident :as game} (vals (react db/games))]
      (c/game-card (assoc game :join #(m/join-game game-id))))]])

(defc message < static
  [{:keys [user timestamp text]}]
  [:div.mb-2
   [:strong (util/username user)]
   [:.text-muted.small (.toLocaleString timestamp)]
   (map #(if (= "\n" %) [:br] %) text)])

; Of course, the hardest part about making a chat component is rendering it. >:-(
(defcs chat < reactive
  {:init (fn [{[game-id] :rum/args :as s} _]
           (m/subscribe [[:messages [:games game-id]]])
           s)}
  {:did-update (fn [{component :rum/react-component :as s}]
                 (let [node (js/ReactDOM.findDOMNode component)
                       box (.querySelector node ".scroll-box")]
                   (set! (.-scrollTop box) (.-scrollHeight box)))
                 s)}
  (local "" ::text)

  [{::keys [text]} game-id]
  [:.border.rounded.border-primary.d-flex.flex-column
   {:style {:height "600px"
            :overflow "hidden"}}
   [:.flex-grow-1]
   [:.pl-3.scroll-box.border-bottom {:style {:overflow-y "auto"}}
    (->> (react db/messages)
      vals
      (filter (fn [{[_ [_ message-game-id]] :ident}]
                (= game-id message-game-id)))
      (sort-by :timestamp)
      (map message))]
   [:textarea.form-control.border-0.flex-shrink-0
    {:placeholder "Enter message"
     :on-key-down #(when (and (= 13 (.-keyCode %))
                           (not (.-shiftKey %)))
                     (m/send-message game-id @text)
                     (reset! text "")
                     (.preventDefault %))
     :value @text
     :on-change #(reset! text (.. % -target -value))}]])

(defc game < reactive
  [{:keys [players]
    [_ game-id] :ident}]
  [:.row
   [:.col-md-8
    [:div "Game ID: " (subs game-id 0 4) " ("
     (c/text-button {:on-click #(m/leave-game game-id)} "leave") ")"]
    [:ul.pl-4
     (for [p players]
       [:li {:key p} (util/username p)])]]
   [:.col-md-4
    (chat game-id)]])

(defc main < reactive
  []
  [:div
   [:.container-fluid.mt-2
    (user-info)
    [:.mt-2]
    (let [uid (react db/uid)
          current-game (->> (react db/games)
                         vals
                         (filter (fn [{:keys [players]}]
                                   (some #{uid} players)))
                         first)]
      (if current-game
        (game current-game)
        (game-list)))
    [:.mt-3]]])

(defn ^:export mount []
  (rum/mount (main) (js/document.querySelector "#app")))

(defn init* [user]
  (m/init-db)
  (mount))

(defn ^:export init []
  (.. js/firebase auth (onAuthStateChanged init*)))
