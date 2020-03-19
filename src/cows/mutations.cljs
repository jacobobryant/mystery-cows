(ns cows.mutations
  (:require-macros
    [cljs.core.async.macros :refer [go-loop]])
  (:require
    [cljs.core.async :refer [<!]]
    [cows.lib :as lib :refer [capture-env]]
    [trident.firestore :as firestore :refer [write merge-changeset]]))

(defn subscribe [{:keys [db/db misc/fs]} q]
  (let [c (firestore/subscribe fs [q])]
    (go-loop []
      (when-some [changeset (<! c)]
        (swap! db merge-changeset changeset)
        (recur)))
    c))

(defn init-db [{:keys [db/db db/subscriptions misc/auth] :as env}]
  (lib/maintain-subscriptions subscriptions #(subscribe env %))
  (let [user (.-currentUser auth)
        uid (.-uid user)
        email (.-email user)]
    (swap! db assoc :ui {:uid uid :email email})))

(defn create-game
  [{:keys [db/uid misc/fs]}]
  (write fs
    {[:games] {:players [@uid]}}))

(defn leave-game
  [{:db/keys [db game-id uid]
    :misc/keys [fs]}]
  (let [last-player (= 1 (count (get-in @db [:games @game-id :players])))]
    (write fs
      {[:games @game-id] (when-not last-player
                           ^:update {:players (.. js/firebase
                                                -firestore
                                                -FieldValue
                                                (arrayRemove @uid))})})))

(defn join-game
  [{:keys [misc/fs db/uid]} game-id]
  (write fs
    {[:games game-id] ^:update {:players (.. js/firebase
                                           -firestore
                                           -FieldValue
                                           (arrayUnion @uid))}}))

(defn send-message [{:keys [misc/fs db/game-id db/uid]} text]
  (write fs
    {[:messages [:games @game-id]] {:text text
                                    :user @uid
                                    :timestamp (js/Date.)}}))

(def env (capture-env 'cows.mutations))
