(ns cows.components
  (:require
    [cows.util :as util]
    [rum.core :as rum :refer [defc static]]))

(defc text-button < static
  [props & contents]
  (into
    [:span.text-primary (assoc-in props [:style :cursor] "pointer")]
    contents))

(defc game-card < static {:key-fn (comp second :ident)}
  [{:keys [players join]
    [_ game-id] :ident}]
  [:.p-3.border.border-dark.rounded.mr-3.mb-3 {:key game-id}
   [:div "Game ID: " (subs game-id 0 4)]
   [:ul.pl-4
    (for [p players]
      [:li {:key p} (util/username p)])]
   [:button.btn.btn-primary.btn-sm.btn-block {:on-click join}
    "Join"]])
