(ns cows.mutations
  (:require
    [cows.lib :as lib :refer [capture-env]]
    [trident.firestore :as firestore :refer [write merge-changeset]]))

(defn subscribe [{:db/keys [db sub-data]
                  :misc/keys [fs]} q]
  (lib/merge-subscription!
    {:state-atom db
     :sub-data-atom sub-data
     :merge-result merge-changeset
     :sub-key q
     :sub-channel (firestore/subscribe fs [q])}))

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
  [{:db/keys [current-game game-id uid]
    :misc/keys [fs]}]
  (let [last-player (= 1 (count (:players @current-game)))]
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
