(ns cows.mutations
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [cows.lib :as lib :refer [capture-env]]
    [trident.util :as u]
    [trident.firestore :as firestore :refer [write query merge-changeset]]))

(defn subscribe [{:db/keys [sub-data]
                  :misc/keys [fs]} q]
  (lib/merge-subscription-results!
    {:sub-data-atom sub-data
     :merge-result merge-changeset
     :sub-key q
     :sub-channel (firestore/subscribe fs [q])}))

(defn init-db [{:keys [db/db db/subscriptions misc/auth] :as env}]
  (let [user (.-currentUser auth)
        uid (.-uid user)
        email (.-email user)]
    (swap! db assoc :ui {:uid uid :email email}))
  (lib/maintain-subscriptions subscriptions #(subscribe env %)))

(defn create-game
  [{:keys [db/uid misc/fs]}]
  (write fs
    {[:games] {:players [@uid]
               :state "lobby"}}))

(defn leave-game
  [{:keys [db/game-id fn/handle] :as env}]
  (handle [:leave-game @game-id]))

(defn join-game
  [{:keys [fn/handle] :as env} game-id]
  (handle [:join-game game-id]))

(defn send-message [{:keys [misc/fs db/game-id db/uid]} text]
  (write fs
    {[:messages [:games @game-id]] {:text text
                                    :user @uid
                                    :timestamp (js/Date.)}}))

(defn start-game
  [{:keys [db/game-id fn/handle] :as env}]
  (handle [:start-game @game-id]))

(defn roll
  [{:keys [db/game-id fn/handle] :as env}]
  (handle [:roll @game-id]))

(defn move
  [{:keys [db/game-id fn/handle] :as env} position]
  (handle [:move [@game-id position]]))

(defn suggest
  [{:keys [db/game-id fn/handle] :as env} cards]
  (handle [:suggest [@game-id cards]]))

(defn respond
  [{:keys [db/game-id fn/handle] :as env} card]
  (handle [:respond [@game-id card]]))

(defn end-turn
  [{:keys [db/game-id fn/handle] :as env}]
  (handle [:end-turn @game-id]))

(defn accuse
  [{:keys [db/game-id fn/handle] :as env} cards]
  (handle [:accuse [@game-id cards]]))

(defn quit
  [{:keys [db/game-id fn/handle] :as env}]
  (when (js/confirm "Are you sure? Quitting will end the game for everyone.")
    (handle [:quit @game-id])))

(def env (capture-env 'cows.mutations))
