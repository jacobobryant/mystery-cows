(ns cows.core
  (:require
    [rum.core :as rum :refer [defc]]))

(defc main []
  [:.d-flex.flex-column.align-items-center.mt-4
   [:p "Welcome to Mystery Cows, "
    (.. js/firebase auth -currentUser -email) "."]
   [:button.btn.btn-primary {:on-click #(.. js/firebase auth signOut)}
    "Sign Out"]])

(defn ^:export mount []
  (rum/mount (main) (js/document.querySelector "#app")))

(defn init* [user]
  (mount))

(defn ^:export init []
  (.. js/firebase auth (onAuthStateChanged init*)))
