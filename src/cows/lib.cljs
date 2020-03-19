(ns cows.lib
  (:require-macros [cows.lib])
  (:require
    [clojure.set :as set]
    [cljs.core.async :refer [close!]]
    [trident.util :as u]))

(defn maintain-subscriptions [sub-atom sub-fn]
  (let [sub->chan (atom {})
        watch (fn [_ _ _ new-subs]
                (let [old-subs (set (keys @sub->chan))
                      old-subs (set/difference old-subs new-subs)
                      new-subs (set/difference new-subs old-subs)]
                  (doseq [channel (map @sub->chan old-subs)]
                    (close! channel))
                  (swap! sub->chan #(apply dissoc % old-subs))
                  (swap! sub->chan merge (u/map-to sub-fn new-subs))))]
    (add-watch sub-atom ::maintain-subscriptions watch)
    (watch nil nil nil @sub-atom)))

(defn respectively [& fs]
  (fn [& xs]
    (mapv #(%1 %2) fs xs)))

(defn capture-env* [nspace]
  (trident.util/map-kv (respectively keyword deref) nspace))

(defn prepend-ns [ns-segment k]
  (keyword
    (cond-> ns-segment
      (not-empty (namespace k)) (str "." (namespace k)))
    (name k)))
