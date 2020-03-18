(ns cows.mutations
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [cljs.core.async :refer [<!]]
    [cows.db :as db :refer [db]]
    [cows.util :as util :refer [fs]]
    [trident.firestore :as tfire :refer [write merge-changeset]]))

(defn subscribe [queries]
  (let [c (tfire/subscribe (fs) queries)]
    (go-loop []
      (swap! db merge-changeset (<! c))
      (recur))))

(defn init-db []
  (let [user (.. js/firebase auth -currentUser)
        uid (.-uid user)
        email (.-email user)]
    (swap! db assoc :ui {:uid uid :email email}))
  (subscribe [[:games]]))

(defn create-game []
  (write (fs)
    {[:games] {:players [@db/uid]}}))

(defn leave-game [game-id]
  (let [last-player (= 1 (count (get-in @db [:games game-id :players])))]
    (write (fs)
      {[:games game-id] (when-not last-player
                          ^:update {:players (.. js/firebase
                                               -firestore
                                               -FieldValue
                                               (arrayRemove @db/uid))})})))

(defn join-game [game-id]
  (write (fs)
    {[:games game-id] ^:update {:players (.. js/firebase
                                           -firestore
                                           -FieldValue
                                           (arrayUnion @db/uid))}}))

(defn send-message [game-id text]
  (write (fs)
    {[:messages [:games game-id]] {:text text
                                   :user @db/uid
                                   :timestamp (js/Date.)}}))
