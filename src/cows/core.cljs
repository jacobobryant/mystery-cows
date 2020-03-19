(ns cows.core
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [cows.components :as c]
    [cows.db :as db]
    [cows.mutations :as m]
    [cows.lib :as lib]
    [trident.util :as u]
    [rum.core :as rum]))

(when (= "localhost" js/location.hostname)
  (.useFunctionsEmulator (js/firebase.functions) "http://localhost:5001")
  (.settings (js/firebase.firestore) #js {:host "localhost:8080"
                                          :ssl false}))

(def env (merge
           #:misc{:fs (js/firebase.firestore)
                  :auth (js/firebase.auth)}
           (lib/prepend-keys "fn" (lib/firebase-fns [:handle]))
           (lib/prepend-keys "db" db/env)
           (lib/prepend-keys "m" m/env)))

(defn ^:export mount []
  (rum/mount (c/main env) (js/document.querySelector "#app")))

(defn init* [user]
  (m/init-db env)
  (mount))

(defn ^:export init []
  (.. js/firebase auth (onAuthStateChanged init*)))
