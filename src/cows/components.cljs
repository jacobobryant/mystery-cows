(ns cows.components
  (:require
    [clojure.string :as str]
    [clojure.spec.alpha :as s]
    [cows.util :as util]
    [trident.util :as u]
    [rum.core :as rum :refer [defc defcs static reactive react local]]))

(defc text-button < static
  [props & contents]
  (into
    [:span.text-primary (assoc-in props [:style :cursor] "pointer")]
    contents))

(defc user-info < reactive
  [{:db/keys [uid email]
    :misc/keys [auth]}]
  [:div
   [:.d-flex.flex-md-row.flex-column
    [:div "Username: " (util/username (react uid))]
    [:.flex-grow-1]
    [:div
     (react email) " ("
     (text-button {:on-click #(.signOut ^js auth)} "sign out") ")"]]
   [:hr.mt-1]])

(defc game-card < static {:key-fn (comp second :ident)}
  [{:m/keys [join-game] :as env} {:keys [players] [_ game-id] :ident}]
  [:.p-3.border.border-dark.rounded.mr-3.mb-3
   [:div "Game ID: " (subs game-id 0 4)]
   [:ul.pl-4
    (for [p players]
      [:li {:key p} (util/username p)])]
   [:button.btn.btn-primary.btn-sm.btn-block
    {:disabled (>= (count players) 6)
     :on-click #(join-game env game-id)}
    "Join"]])

(defc game-list < reactive
  [{:keys [m/create-game db/games] :as env}]
  [:div
   [:button.btn.btn-primary {:on-click #(create-game env)}
    "Create game"]
   [:.mt-4]
   [:.d-flex.flex-wrap
    (->> (react games)
      vals
      (map (partial game-card env)))]])

(defc message < static {:key-fn #(.getTime (:timestamp %))}
  [{:keys [user timestamp text]}]
  [:div.mb-2
   [:strong (util/username user)]
   [:.text-muted.small (.toLocaleString timestamp)]
   (map #(if (= "\n" %) [:br] %) text)])

(def square-size 22)

; Of course, the hardest part about making a chat component is rendering it. >:-(
(defcs chat < reactive
  {:did-update (fn [{component :rum/react-component :as s}]
                 (let [node (js/ReactDOM.findDOMNode component)
                       box (.querySelector node ".scroll-box")]
                   (set! (.-scrollTop box) (.-scrollHeight box)))
                 s)}
  (local "" ::text)

  [{::keys [text]} {:keys [db/messages m/send-message] :as env}]
  [:.border.rounded.border-primary.d-flex.flex-column.flex-grow-1
   {:style {:min-width "200px"
            :height (* 25 square-size)
            :overflow "hidden"}}
   [:.flex-grow-1]
   [:.pl-3.scroll-box.border-bottom {:style {:overflow-y "auto"}}
    (map message (take 2 (react messages)))]
   [:textarea.form-control.border-0.flex-shrink-0
    {:placeholder "Enter message"
     :on-key-down #(when (and (= 13 (.-keyCode %))
                           (not (.-shiftKey %)))
                     (send-message env @text)
                     (reset! text "")
                     (.preventDefault %))
     :value @text
     :on-change #(reset! text (.. % -target -value))}]])

(defc event < reactive {:key-fn (fn [_ {[_ [_ _ event-id]] :ident}]
                                  event-id)}
  [{:db/keys [uid players]}
   {:keys [event timestamp player roll destination cards
           correct responder suggester card]}]
  (let [pronoun #(if (= % (react uid))
                   "You"
                   (util/username %))
        event-description
        (case event
          "roll" (str (pronoun player) " rolled a " roll)
          "move" (when-not (s/valid? :cows.util/coordinate destination)
                   (str (pronoun player) " moved to the "
                     (util/room-char->name destination)))
          "suggest" [:div
                     (str (pronoun player) " made a suggestion: " (str/join ", " cards)) [:br]
                     (for [p (util/in-between (react players) player responder)]
                       [:span {:key p}
                        (pronoun p) " couldn't respond." [:br]])
                     (when responder
                       (str "Waiting for " (str/lower-case (pronoun responder)) " to respond."))]
          "respond" (str (pronoun responder) " showed " (str/lower-case (pronoun suggester))
                      " a card"
                      (if card
                        (str ": " card)
                        "."))
          "accuse" [:div (pronoun player) " made an accusation: " (str/join ", " cards) [:br]
                    (if correct "Correct!" "Wrong!")])]
    (when event-description
      [:.mb-2.border-top.pt-2
       event-description])))

(defc events < reactive
  {:did-update (fn [{component :rum/react-component :as s}]
                 (let [node (js/ReactDOM.findDOMNode component)
                       box (.querySelector node ".scroll-box")]
                   (set! (.-scrollTop box) (.-scrollHeight box)))
                 s)}

  [{:keys [db/events] :as env}]
  [:.border.rounded.border-primary.d-flex.flex-column.mb-3
   {:style {:height "300px"
            :overflow "hidden"}}
   [:.flex-grow-1]
   [:.p-2.scroll-box.border-bottom {:style {:overflow-y "auto"}}
    (map #(event env %) (react events))]])

(defc game-lobby < reactive
  [{:keys [db/current-game m/leave-game m/start-game] :as env}]
  (let [{:keys [players state]
         [_ game-id] :ident} (react current-game)]
    [:div
     [:div "Game ID: " (subs game-id 0 4)]
     [:ul.pl-4
      (for [p players]
        [:li {:key p} (util/username p)])]
     [:div
      [:button.btn.btn-primary.mr-2
       {:disabled (< (count players) 3)
        :on-click #(start-game env)}
       "Start game"]
      [:button.btn.btn-secondary {:on-click #(leave-game env)} "Leave game"]]]))

(defc select-card < reactive
  [model cards]
  [:select.form-control.mr-2
   {:value (react model)
    :on-change #(reset! model (.. % -target -value))}
   (for [x cards]
     [:option {:key x :value x} x])])

(defcs choose-cards < reactive (local nil ::person) (local nil ::weapon) (local nil ::room)
  [{::keys [person weapon room]} {:keys [include-room on-choose text on-cancel]}]
  (let [btn [:button.btn.btn-primary.flex-shrink-0
             {:on-click #(->> [(or @person (first util/names))
                               (or @weapon (first util/weapons))
                               (or @room (first util/rooms))]
                           (remove nil?)
                           on-choose)}
             text]
        selects [:.d-flex
                 (select-card person util/names)
                 (select-card weapon util/weapons)
                 (when include-room
                   (select-card room util/rooms))
                 (when-not on-cancel
                   btn)]]
    (if on-cancel
      [:div
       selects
       [:.d-flex.mt-2
        btn
        [:button.btn.btn-secondary.ml-2 {:on-click on-cancel} "Cancel"]]]
      selects)))

(defcs show-card < reactive (local nil ::choice)
  [{::keys [choice]} {:keys [db/suggestion db/cards m/respond] :as env}]
  (let [suggestion (filter (set (react cards)) (react suggestion))
        choice-value (or @choice (first suggestion))]
    [:.d-flex
     (for [card suggestion
           :let [selected (= choice-value card)]]
       [:.form-check.form-check-inline.mr-3
        {:key card
         :style {:cursor "pointer"}
         :on-click #(reset! choice card)}
        [:input.form-check-input
         {:checked selected
          :style {:cursor "pointer"}
          :type "radio"}]
        [:label.form-check-label
         {:style {:cursor "pointer"}}
         card]])
     [:button.btn.btn-primary.ml-2
      {:on-click #(respond env choice-value)}
      "Show card"]]))

(defcs accuse < reactive (local false ::accusing)
  [{::keys [accusing]} {:keys [m/accuse m/end-turn] :as env}]
  (if @accusing
    (choose-cards {:text "Make accusation"
                   :include-room true
                   :on-cancel #(reset! accusing false)
                   :on-choose #(accuse env %)})
    [:.d-flex
     [:button.btn.btn-primary.mr-2 {:on-click #(end-turn env)} "End turn"]
     [:button.btn.btn-secondary {:on-click #(reset! accusing true)} "Make accusation"]]))

(defc turn-controls < reactive
  [{:db/keys [your-turn state uid winner responder
              responding current-player roll-result]
    :m/keys [roll suggest] :as env}]
  (cond
    (= :game-over (react state))
    [:p "Game over. "
     (if (= (react uid) (react winner))
       "You won!"
       (str (util/username (react winner)) " won."))]

    (react your-turn)
    (case (react state)
      :start-turn [:button.btn.btn-primary {:on-click #(roll env)} "Roll dice"]
      :after-roll [:p "You rolled " (react roll-result) ". Choose a destination."]
      :suggest (choose-cards {:text "Make suggestion"
                              :on-choose #(suggest env %)})
      :respond [:p "Waiting for " (util/username (react responder)) " to show you a card."]
      :accuse (accuse env))

    :default
    (if (and (= :respond (react state)) (react responding))
      (show-card env)
      [:p "It's " (util/username (react current-player)) "'s turn."])))

(defn board-element [row col width height z]
  {:position "absolute"
   :top (int (* square-size row))
   :left (int (* square-size col))
   :width (int (* square-size width))
   :height (int (* square-size height))
   :z-index z})

(defc board < reactive
  [{:db/keys [available-locations positions colors your-turn state]
    :m/keys [move] :as env}]
  (let [available-locations (react available-locations)
        moving (and (react your-turn) (= (react state) :after-roll))]
    [:.flex-shrink-0 {:style {:position "relative"
                              :width (* square-size util/board-width)
                              :height (* square-size 25)}}

     ; Rooms
     (for [[room [row col width height]] util/room-tiles
           :let [available (contains? available-locations room)
                 can-move (and available moving)]]
       [:div {:key (name room)
              :class (when can-move "available-room")
              :style (merge (board-element row col width height 1)
                       {:text-align "center"})
              :on-click (when can-move
                          #(move env (util/rooms-map-invert room)))}
        [:div {:style (cond-> {}
                        (= :conservatory room) (assoc :margin-left "-20px")
                        available (assoc :font-weight "bold"))}
         (util/card-names room)]])

     (for [[row col :as loc] (keys util/empty-board)
           :let [available (contains? available-locations loc)
                 can-move (and available moving)
                 [bottom-border right-border]
                 (for [i [0 1]
                       :let [next-loc (update loc i inc)
                             next-available (contains? available-locations next-loc)]]
                   (str "1px solid " (if (or available next-available) "black" "#bc9971")))
                 [top-border left-border]
                 (for [i [0 1]
                       :let [prev-loc (update loc i dec)]]
                   (when (and available (not (contains? util/empty-board prev-loc)))
                     "1px solid black"))]]
       [:div {:key (pr-str loc)
              :class (when can-move "available-square")
              :style (merge (board-element row col 1 1 2)
                       {:border-top top-border
                        :border-left left-border
                        :border-right right-border
                        :border-bottom bottom-border
                        :background-color "#eac8a3"})
              :on-click (when can-move #(move env loc))}])

     ; Doors
     (for [[[row col] orientation] util/door-directions]
       (let [horizontal (= orientation :horizontal)
             [width height] (cond-> [0.1 1] horizontal reverse)
             style (merge (board-element row col width height 3)
                     {:background-color "black"})
             style (update style (if horizontal :top :left) dec)]
         [:div {:key (pr-str [row col])
                :style style}]))

     ; Players
     (for [[player [row col]] (util/positions->coordinates (react positions))]
       (let [style (merge (board-element row col 0.7 0.7 4)
                     {:margin (int (* square-size 0.15))
                      :background-color ((react colors) player)})]
         [:div {:key player
                :style style}]))]))

(defc static-info < reactive
  [{:db/keys [events uid losers players names current-player cards]}]
  [:div
   [:div.mb-2 "Cards: " (str/join ", " (react cards))]
   [:div "Players: "]
   [:ul
    (for [p (react players)]
      [:li {:key p
            :class [(when (= (react current-player) p) "font-weight-bold")
                    (when (= (react uid) p) "font-italic")]}
       (cond->>
         (str (util/username p) " (" ((react names) p)  ")")
         ((react losers) p) (vector :del))])]])

(defc game < reactive
  [{:keys [db/state m/quit] :as env}]
  (if (= :lobby (react state))
    [:div
     (game-lobby env)
     [:.mt-3]
     (chat env)]
    [:div
     (events env)
     [:.d-flex
      (board env)
      [:.mr-4]
      (chat env)]
     [:.mt-3]
     (turn-controls env)
     [:hr]
     (static-info env)
     [:button.btn.btn-secondary {:on-click #(quit env)} "Quit"]]))

(defc main < reactive
  [{:keys [db/game-id] :as env}]
  [:div
   [:.container-fluid.mt-2
    (user-info env)
    [:.mt-2]
    (if (react game-id)
      (game env)
      (game-list env))
    [:.mt-3]]])
