(ns cows.util
  (:require
    [clojure.spec.alpha :as s]
    [clojure.set :as set]
    [clojure.string :as str]))

(def adjectives ["miserable" "vomitous" "artless" "bawdy" "beslubbering" "bootless" "churlish" "clouted" "craven" "currish" "dankish" "dissembling" "droning" "errant" "fawning" "fobbing" "froward" "frothy" "gleeking" "goatish" "gorbellied" "impertinent" "infectious" "jarring" "loggerheaded" "lumpish" "mammering" "mangled" "mewling" "paunchy" "pribbling" "puking" "puny" "qualling" "rank" "reeky" "roguish" "ruttish" "saucy" "spleeny" "spongy" "surly" "tottering" "unmuzzled" "vain" "venomed" "villainous" "warped" "wayward" "weedy" "yeasty"])

(def nouns ["mass" "pig" "apple-john" "baggage" "barnacle" "bladder" "boar-pig" "bugbear" "bum-bailey" "canker-blossom" "clack-dish" "clotpole" "coxcomb" "codpiece" "death-token" "dewberry" "flap-dragon" "flirt-gill" "foot-licker" "fustilarian" "giglet" "gudgeon" "haggard" "harpy" "hedge-pig" "horn-beast" "hugger-mugger" "joithead" "lout" "maggot-pie" "malt-worm" "mammet" "measle" "minnow" "miscreant" "moldwarp" "mumble-news" "nut-hook" "pigeon-egg" "pignut" "puttock" "pumpion" "ratsbane" "scut" "skainsmate" "strumpet" "varlot" "vassal" "whey-face" "wagtail"])

(defn rand-word [v seed]
  (v (mod (hash seed) (count v))))

(defn username [uid]
  (str (rand-word adjectives uid) "-" (rand-word nouns uid)))

(def names ["Miss Scarlet"
            "Colonel Mustard"
            "Mrs. Peacock"
            "Mr. Green"
            "Professor Plum"
            "Mrs. White"])

(def colors ["red"
             "#ffdb58"
             "blue"
             "darkgreen"
             "purple"
             "white"])

(def weapons ["Knife"
              "Lead pipe"
              "Candlestick"
              "Rope"
              "Revolver"
              "Wrench"])

(def rooms ["Study"
            "Hall"
            "Lounge"
            "Dining Room"
            "Kitchen"
            "Ballroom"
            "Conservatory"
            "Billiard Room"
            "Library"])

(defn starting-cards [players]
  (let [cards (->> [names weapons rooms]
                (mapcat (comp rest shuffle))
                shuffle)
        hand-size (quot (count cards) (count players))
        hands (partition-all hand-size cards)
        face-up-cards (nth hands (count players) nil)]
    [(zipmap players hands) face-up-cards]))

(defn starting-positions [players]
  (zipmap players (shuffle [[0 16] [7 23] [24 14] [24 9] [18 0] [5 0]])))

(def raw-board
  ["              -                 r"
   "    A A A     - -             - -"
   "    A A A     - -             - -     C C C"
   "              - -     B B B   - -     C C C"
   "  - - - - - 0 - 1     B B B   - -"
   "p - - - - - - - -             - -"
   "            - - -             - - 2 - - - - -"
   "    I I I     - - - - 1 1 - - - - - - - - - - y"
   "    I I I     8 -           - - - 3 - - - - -"
   "              - -           - -"
   "            - - -           - -"
   "  7 - 8 - - - - -           - -       D D D"
   "            - - -           - 3       D D D"
   "  H H H     - - -           - -"
   "  H H H     - - -           - -"
   "            7 - - - - - - - - - - - -"
   "            - - - 5 - - - - 5 - - - - - - - -"
   "  - - - - - - -                 - - - 4 - - - -"
   "b - - - - - - -                 - -"
   "          6 - 5     F F F       5 -"
   "            - -     F F F       - -     E E E"
   "    G G G   - -                 - -     E E E"
   "    G G G   - -                 - -"
   "              - - -         - - -"
   "                  g         w"])

(def board (remove #(empty? (str/trim %)) raw-board))
(def raw-board-width (count (reduce #(max-key count %1 %2) board)))
(def board-width (quot (inc raw-board-width) 2))

(defn map-inverse [m]
  (reduce
    (fn [inverse [k v]]
      (update inverse v
              #(if (nil? %)
                 #{k}
                 (conj % k))))
    {}
    m))

(defn dissoc-by [m f]
  (into {} (remove (comp f second) m)))

(defn lookup [i j]
  (get (nth board i) j))

(def starting-board
  (let [board-map (into {} (for [i (range (count board))
                                 j (range 0 raw-board-width 2)]
                             [[i (quot j 2)] (lookup i j)]))]
    (dissoc-by board-map #(contains? #{\space nil} %))))

(def board-inverse (dissoc (map-inverse starting-board) \-))

(def room-coordinates
  (-> board-inverse
    (select-keys [\A \B \C \D \E \F \G \H \I])
    (set/rename-keys {\A \0
                      \B \1
                      \C \2
                      \D \3
                      \E \4
                      \F \5
                      \G \6
                      \H \7
                      \I \8})))

(def room-char->name (zipmap (sort (keys room-coordinates)) rooms))

(defn coordinates-with [values]
  (mapcat (comp vec board-inverse) values))

(defn replace-values [board values replacement]
  (reduce
    #(assoc %1 %2 replacement)
    board
    (coordinates-with values)))

(def player-chars #{\r \y \b \g \p \w})
(def room-print-chars #{\A \B \C \D \E \F \G \H \I})

(def empty-board
  (as-> starting-board x
    (replace-values x player-chars \-)
    (reduce dissoc x (coordinates-with room-print-chars))))

(def room-tiles
  {:study [0 0 7 4]
   :hall [0 9 6 7]
   :lounge [0 17 7 6]
   :dining-room [9 16 8 7]
   :kitchen [18 18 6 7]
   :ballroom [17 8 8 6]
   :conservatory [19 0 6 6]
   :billiard-room [12 0 6 5]
   :library [6 0 7 5]})

(def card-names
  {:scarlet "Miss Scarlet"
   :mustard "Colonel Mustard"
   :peacock "Mrs. Peacock"
   :green "Mr. Green"
   :plum "Mr. Plum"
   :white "Mrs. White"
   :knife "Knife"
   :lead-pipe "Lead pipe"
   :candlestick "Candlestick"
   :rope "Rope"
   :revolver "Revolver"
   :wrench "Wrench"
   :study "Study"
   :hall "Hall"
   :lounge "Lounge"
   :dining-room "Dining room"
   :kitchen "Kitchen"
   :ballroom "Ballroom"
   :conservatory "Conservatory"
   :billiard-room "Billiard room"
   :library "Library"})

(def door-directions
  {[4 6] :horizontal
   [4 9] :vertical
   [7 11] :horizontal
   [7 12] :horizontal
   [6 17] :horizontal
   [9 17] :horizontal
   [12 16] :vertical
   [18 19] :horizontal
   [19 16] :vertical
   [17 14] :horizontal
   [17 9] :horizontal
   [19 8] :vertical
   [19 5] :vertical
   [15 6] :vertical
   [12 1] :horizontal
   [11 3] :horizontal
   [8 7] :vertical})

(s/def ::coordinate (s/tuple int? int?))

(defn conj-some [coll x]
  (cond-> coll
    x (conj x)))

; Like in Nacho Libre
(def secret-tunnels {\0 \4, \4 \0, \2 \6, \6 \2})

(def all-coordinates (set (keys starting-board)))

(def room-chars #{\0 \1 \2 \3 \4 \5 \6 \7 \8})

(defn adjacent-locations
  [source]
  (let [[x y] source,
        coordinates
        (set/intersection all-coordinates
          #{[(inc x) y] [(dec x) y] [x (inc y)] [x (dec y)]}),
        room (room-chars (starting-board source))]
    (conj-some coordinates room)))

; BFS
(defn available-locations
  [source roll]
  (let [[roll-0 visited-0]
        (if (s/valid? ::coordinate source)
          [roll #{source}]
          [(dec roll) (conj-some (board-inverse source)
                        (secret-tunnels source))])]
    (loop [roll roll-0
           visited visited-0
           current-batch visited-0]
      (if (= roll 0)
        visited
        (let [next-batch (as-> current-batch x
                           (filter #(s/valid? ::coordinate %) x)
                           (map adjacent-locations x)
                           (apply set/union x)
                           (set/difference x visited))]
          (recur (dec roll)
                 (set/union visited next-batch)
                 next-batch))))))

(def rooms-vec [:study :hall :lounge :dining-room :kitchen
                :ballroom :conservatory :billiard-room :library])

(def rooms-map
  (into {} (map vector (map #(char (+ 48 %)) (range 10)) rooms-vec)))

(def rooms-map-invert (set/map-invert rooms-map))

(defn valid-move? [{:keys [positions roll dest player]}]
  (let [taken (filter #(s/valid? ::coordinate %) (vals positions))
        available (available-locations (positions player) roll)
        available (apply disj available taken)]
    (contains? available dest)))

(defn next-player [players losers current-player]
  (let [players (vec (remove (set losers) players))]
    (-> players
      (.indexOf current-player)
      inc
      (mod (count players))
      players)))

(defn next-players [players current-player]
  (concat (rest (drop-while (complement #{current-player}) players))
    (take-while (complement #{current-player}) players)))

(defn in-between [players p1 p2]
  (take-while #(not= % p2) (next-players players p1)))

(defn split-by [f coll]
  (reduce
    #(update %1 (if (f %2) 0 1) conj %2)
    [nil nil]
    coll))

(defn positions->coordinates [positions]
  (reduce (fn [player->coordinates [player position]]
            (assoc player->coordinates
              player (cond-> position
                       (not (s/valid? ::coordinate position))
                       (->>
                         room-coordinates
                         (remove (set (vals player->coordinates)))
                         first))))
    {}
    positions))
