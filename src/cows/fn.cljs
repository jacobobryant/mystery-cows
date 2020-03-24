(ns cows.fn
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    ["firebase-functions" :as functions]
    ["firebase-admin" :as admin :refer [firestore]]
    [cljs.core.async :as async :refer [<! take!]]
    [clojure.edn :as edn]
    [clojure.spec.alpha :as s]
    [clojure.set :as set]
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
      (when (= state "lobby")
        (let [messages (when (= 1 (count players))
                         (u/map-from-to :ident (constantly nil)
                           (<! (query (firestore) [:messages [:games game-id]]))))]
          (<! (write (firestore)
                (merge
                  {[:games game-id] (when-not (= 1 (count players))
                                      ^:update {:players (.. firestore
                                                           -FieldValue
                                                           (arrayRemove uid))})}
                  messages))))))
    nil))

(defmethod handle :join-game
  [{:keys [auth/uid]} game-id]
  (go
    (when-some [{:keys [players state]} (<! (game game-id))]
      (when (and (= state "lobby") (< (count players) 6))
        (<! (write (firestore)
              {[:games game-id] ^:update {:players (.. firestore
                                                     -FieldValue
                                                     (arrayUnion uid))}}))))
    nil))

(defmethod handle :roll
  [{:keys [auth/uid]} game-id]
  (go
    (when-some [{:keys [current-player state]} (<! (game game-id uid))]
      (when (and (= state "start-turn") (= uid current-player))
        (let [roll (apply + (repeatedly 2 #(inc (rand-int 6))))]
          (<! (write (firestore)
                {[:games game-id] ^:update {:state "after-roll"
                                            :roll-result roll}})))))
    nil))

(defmethod handle :move
  [{:keys [auth/uid]} [game-id dest]]
  (go
    (when-some [{:keys [current-player state positions players roll-result]
                 :as game} (<! (game game-id uid))]
      (when (and (= state "after-roll")
              (= uid current-player)
              (util/valid-move?
                {:positions (u/map-keys name positions)
                 :roll roll-result
                 :player uid
                 :dest dest}))
        (<! (write (firestore)
              {[:games game-id]
               ^:update {:state (if (s/valid? :cows.util/coordinate dest)
                                  "accuse"
                                  "suggest")
                         (str "positions." uid) dest}}))))
    nil))

(defmethod handle :end-turn
  [{:keys [auth/uid]} game-id]
  (go
    (when-some [{:keys [current-player players losers state]} (<! (game game-id uid))]
      (when (and (= state "accuse")
              (= uid current-player))
        (<! (write (firestore)
              {[:games game-id]
               ^:update {:state "start-turn"
                         :current-player (util/next-player players losers current-player)}}))))))

(defmethod handle :suggest
  [{:keys [auth/uid]} [game-id [person weapon]]]
  (go
    (when-some [{:keys [current-player state positions players losers]
                 :as game} (<! (game game-id uid))]
      (when (and (= state "suggest")
              (= uid current-player)
              ((set util/names) person)
              ((set util/weapons) weapon))
        (let [room (util/room-char->name (positions (keyword current-player)))
              suggestion [person weapon room]
              responder (->> (util/next-players players current-player)
                          (map #(pull (firestore) [:cards [:games game-id %]]))
                          (async/map vector)
                          <!
                          (filter #(some (set (:cards %)) suggestion))
                          first
                          :ident
                          second
                          last)]
          (<! (write (firestore)
                {[:games game-id]
                 ^:update {:state (if responder "respond" "accuse")
                           :responder responder
                           :suggestion suggestion}
                 [:events [:games game-id]] {:event "suggest"
                                             :timestamp (u/now)
                                             :player uid
                                             :responder responder
                                             :cards suggestion}})))))
    nil))

(defmethod handle :accuse
  [{:keys [auth/uid]} [game-id [person weapon room :as accusation]]]
  (go
    (when-some [{:keys [current-player face-up-cards state positions players losers]
                 :as game} (<! (game game-id uid))]
      (when (and (= state "accuse")
              (= uid current-player)
              ((set util/names) person)
              ((set util/weapons) weapon)
              ((set util/rooms) room))
        (let [player-cards (->> players
                             (map #(pull (firestore) [:cards [:games game-id %]]))
                             (async/map vector)
                             <!
                             (mapcat :cards)
                             set)
              solution (set/difference (set (concat util/names util/weapons util/rooms))
                         player-cards
                         (set face-up-cards))
              correct (= #{person weapon room} solution)
              two-left (and (not correct)
                         (= 2 (- (count players) (count losers))))
              winner (cond
                       correct uid
                       two-left (->> players
                                  (remove (conj (set losers) uid))
                                  first)
                       :default nil)
              game-data (u/assoc-some
                          {:state (if correct
                                    "game-over"
                                    "start-turn")}
                          :current-player (when-not correct
                                            (util/next-player players losers uid))
                          :losers (when-not correct
                                    (.. firestore
                                      -FieldValue
                                      (arrayUnion uid)))
                          :winner winner
                          :solution (when winner solution))]
          (let [event-id (str (random-uuid))
                event {:event "accuse"
                       :timestamp (u/now)
                       :player uid
                       :correct correct
                       :cards [person weapon room]}
                events (cond->
                         {[:events [:games game-id event-id]] event}
                         (not correct) (assoc [:accusations [:games game-id event-id]]
                                         (assoc event :solution solution)))]
            (<! (write (firestore)
                  (merge
                    {[:games game-id] (with-meta game-data {:update true})}
                    events)))))))
    nil))

(defmethod handle :respond
  [{:keys [auth/uid]} [game-id card]]
  (go
    (when-some [{:keys [responder state positions players current-player suggestion]
                 :as game} (<! (game game-id uid))]
      (when (and (= state "respond")
              (= responder uid)
              (some #{card} suggestion)
              ((set (:cards (<! (pull (firestore) [:cards [:games game-id responder]]))))
               card))
        (let [event-id (str (random-uuid))
              event {:event "respond"
                     :timestamp (u/now)
                     :responder uid
                     :suggester current-player}]
          (<! (write (firestore)
                {[:games game-id] ^:update {:state "accuse"}
                 [:events [:games game-id event-id]] event
                 [:responses [:games game-id event-id]] (assoc event :card card)})))))
    nil))

(defmethod handle :quit
  [{:keys [auth/uid]} game-id]
  (go
    (when-some [_ (<! (game game-id uid))]
      (let [subcollections (->> [:messages :cards :events :responses]
                             (map #(query (firestore) [% [:games game-id]]))
                             (async/map vector)
                             <!
                             (map #(u/map-from-to :ident (constantly nil) %)))]
        (<! (write (firestore)
              (apply merge {[:games game-id] nil} subcollections)))))
    nil))

(defmethod handle :start-game
  [{:keys [auth/uid]} game-id]
  (go
    (when-some [{:keys [players state] :as game} (<! (game game-id uid))]
      (when (and (= state "lobby") (<= 3 (count players) 6))
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
