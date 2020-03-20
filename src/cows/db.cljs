(ns cows.db
  (:require
    [cows.lib :as lib :refer [capture-env defcursors defderivations]]
    [trident.util :as u]))

(defonce db (atom {}))

(defcursors db
  uid [:ui :uid]
  email [:ui :email]
  sub-data [:sub-data])

(defderivations [uid sub-data] cows.db
  data (apply merge-with merge (vals sub-data))
  games (:games data)
  all-messages (:messages data)
  current-game (->> games
                  vals
                  (filter (fn [{:keys [players]}]
                            (some #{uid} players)))
                  first)
  game-id (-> current-game :ident second)
  messages (when game-id
             (->> all-messages
               vals
               (filter (fn [{[_ [_ message-game-id]] :ident}]
                         (= game-id message-game-id)))
               (sort-by :timestamp)))
  subscriptions (if game-id
                  #{[:games game-id]
                    [:messages [:games game-id]]}
                  #{{:ident [:games]
                     :where [[:state '== "lobby"]]}
                    {:ident [:games]
                     :where [[:players 'array-contains uid]]}}))

(def env (capture-env 'cows.db))
