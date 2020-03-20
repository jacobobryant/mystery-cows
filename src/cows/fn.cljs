(ns cows.fn
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    ["firebase-functions" :as functions]
    ["firebase-admin" :as admin :refer [firestore]]
    [cljs.core.async :refer [<! take!]]
    [clojure.edn :as edn]
    [cows.lib :as lib]
    [cows.util :as util]
    [trident.firestore :as tfire :refer [query pull write]]
    [trident.util :as u]))

; use transactions

(defonce init (.initializeApp admin))

(defmulti handle (fn [env data] (:event env)))
(defmethod handle :default [_ _] nil)

(defn game
  ([game-id uid]
   (go
     (let [{:keys [players] :as game} (<! (game game-id))]
       (when (some #{uid} players)
         game))))
  ([game-id]
   (pull (firestore) [:games game-id])))

(defmethod handle :leave-game
  [{:keys [auth/uid]} game-id]
  (go
    (when-some [{:keys [players state]} (<! (game game-id uid))]
      (when (nil? state)
        (<! (write (firestore)
              {[:games game-id] (when-not (= 1 (count players))
                                  ^:update {:players (.. firestore
                                                       -FieldValue
                                                       (arrayRemove uid))})}))))
    nil))

(defmethod handle :join-game
  [{:keys [auth/uid]} game-id]
  (go
    (when-some [{:keys [players state]} (<! (game game-id))]
      (when (and (nil? state) (< (count players) 6))
        (<! (write (firestore)
              {[:games game-id] ^:update {:players (.. firestore
                                                     -FieldValue
                                                     (arrayUnion uid))}}))))
    nil))

(defmethod handle :start-game
  [{:keys [auth/uid]} game-id]
  (go
    (when-some [{:keys [players state] :as game} (<! (game game-id uid))]
      (when (and (nil? state) (<= 3 (count players) 6))
        (let [players (shuffle players)
              [cards face-up-cards] (util/starting-cards players)]
          (<! (write (firestore)
                (merge
                  {[:games game-id] {:players players
                                     :state "start-turn"
                                     :current-player (first players)
                                     :face-up-cards face-up-cards
                                     :positions (util/starting-positions players)}}
                  (u/map-kv (fn [player cards]
                              [[:cards [:games game-id player]] {:cards cards}])
                    cards))))
          nil)))))

(def exports
  #js {:handle (->> handle
                 lib/wrap-fn
                 (.onCall functions/https))})
