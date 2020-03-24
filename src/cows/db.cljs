(ns cows.db
  (:require
    [cows.lib :as lib :refer [capture-env defcursors defderivations]]
    [cows.util :as util]
    [trident.util :as u]))

(defonce db (atom {}))

(defcursors db
  uid [:ui :uid]
  email [:ui :email]
  sub-data [:sub-data])

(defderivations [uid sub-data] cows.db
  data                (apply merge-with merge (vals sub-data))

  games               (:games data)
  all-messages        (:messages data)
  events              (->> [:events :responses :accusations]
                        ; For some reason, (map data) makes this fail. Bug in defderivations?
                        (map #(% data))
                        (apply merge)
                        vals
                        (sort-by :timestamp))

  current-game        (->> games
                         vals
                         (filter (fn [{:keys [players]}]
                                   (some #{uid} players)))
                         first)

  players             (:players current-game)
  current-player      (:current-player current-game)
  responder           (:responder current-game)
  suggestion          (:suggestion current-game)
  roll-result         (:roll-result current-game)
  face-up-cards       (:face-up-cards current-game)
  winner              (:winner current-game)
  state               (keyword (:state current-game))
  losers              (set (:losers current-game))
  game-id             (-> current-game :ident second)

  cards               (get-in data [:cards [:games game-id uid] :cards])
  messages            (when game-id
                        (->> all-messages
                          vals
                          (filter (fn [{[_ [_ message-game-id]] :ident}]
                                    (= game-id message-game-id)))
                          (sort-by :timestamp)))
  names               (zipmap players util/names)
  colors              (zipmap players util/colors)
  your-turn           (= uid current-player)
  responding          (and (= :respond state) (= responder uid))
  positions           (u/map-keys name (:positions current-game))
  available-locations (when (and (= state :after-roll)
                              (every? some? [(positions current-player) roll-result]))
                        (->> (util/available-locations (positions current-player) roll-result)
                          (map #(cond-> % (string? %) util/rooms-map))
                          (into #{})))

  subscriptions       (if game-id
                        #{[:games game-id]
                          [:cards    [:games game-id uid]]
                          [:messages [:games game-id]]
                          [:events   [:games game-id]]
                          {:ident [:responses   [:games game-id]] :where [[:responder '== uid]]}
                          {:ident [:responses   [:games game-id]] :where [[:suggester '== uid]]}
                          {:ident [:accusations [:games game-id]] :where [[:player '== uid]]}}
                        #{{:ident [:games] :where [[:state '== "lobby"]]}
                          {:ident [:games] :where [[:players 'array-contains uid]]}}))

(def env (capture-env 'cows.db))
