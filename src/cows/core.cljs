(ns cows.core
  (:require
    [cows.components :as c]
    [cows.db :as db]
    [cows.mutations :as m]
    [cows.lib :as lib]
    [trident.util :as u]
    [rum.core :as rum]))

(def env (merge
           #:misc{:fs (js/firebase.firestore)
                  :auth (js/firebase.auth)}
           (u/map-keys #(lib/prepend-ns "db" %) db/env)
           (u/map-keys #(lib/prepend-ns "m" %) m/env)))

(defn ^:export mount []
  (rum/mount (c/main env) (js/document.querySelector "#app")))

(defn init* [user]
  (m/init-db env)
  (mount))

(defn ^:export init []
  (.. js/firebase auth (onAuthStateChanged init*)))
