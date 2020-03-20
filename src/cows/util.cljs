(ns cows.util)

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
  (zipmap players (shuffle [[17 0] [24 7] [15 24] [10 24] [0 18] [0 5]])))
