(ns cows.fn
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    ["firebase-functions" :as functions]
    [cljs.core.async :refer [<!]]
    [clojure.edn :as edn]
    [cows.lib :as lib]
    [trident.util :as u]))

(defmulti handle (fn [env data] (:event env)))
(defmethod handle :default [_ _] nil)

(defmethod handle :start-game
  [{:keys [auth/uid]}]
  (str "starting a game for " uid))

(def exports
  #js {:handle (->> handle
                 lib/wrap-fn
                 (.onCall functions/https))})
