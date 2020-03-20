(ns cows.lib
  (:require-macros
    [cows.lib]
    [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [cljs.core.async :as async :refer [take! chan put!]]
    [clojure.edn :as edn]
    [rum.core]
    [clojure.set :as set]
    [cljs.core.async :refer [close!]]
    [trident.util :as u]))

(defn maintain-subscriptions
  "Watch for changes in a set of subscriptions (stored in sub-atom), subscribing
  and unsubscribing accordingly. sub-fn should take an element of @sub-atom and
  return a channel that delivers the subscription channel after the first subscription result
  has been received. This is necessary because otherwise, old subscriptions would
  be closed too early, causing problems for the calculation of sub-atom."
  [sub-atom sub-fn]
  (let [sub->chan (atom {})
        c (chan)
        watch (fn [_ _ old-subs new-subs]
                (put! c [old-subs new-subs]))]
    (go-loop []
      (let [[old-subs new-subs] (<! c)
            tmp old-subs
            old-subs (set/difference old-subs new-subs)
            new-subs (vec (set/difference new-subs tmp))
            new-channels (<! (async/map vector (map sub-fn new-subs)))]
        (swap! sub->chan merge (zipmap new-subs new-channels))
        (doseq [channel (map @sub->chan old-subs)]
          (close! channel))
        (swap! sub->chan #(apply dissoc % old-subs)))
      (recur))
    (add-watch sub-atom ::maintain-subscriptions watch)
    (watch nil nil #{} @sub-atom)))

(defn merge-subscription-results!
  "Continually merge results from subscription into sub-data-atom. Returns a channel
  that delivers sub-channel after the first result has been merged."
  [{:keys [sub-data-atom merge-result sub-key sub-channel]}]
  (go
    (let [merge! #(swap! sub-data-atom update sub-key merge-result %)]
      (merge! (<! sub-channel))
      (go-loop []
        (if-some [result (<! sub-channel)]
          (do
            (merge! result)
            (recur))
          (swap! sub-data-atom dissoc sub-key)))
      sub-channel)))

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

(defn prepend-keys [ns-segment m]
  (u/map-keys #(prepend-ns ns-segment %) m))

(defn firebase-fns [ks]
  (u/map-to (fn [k]
              (let [f (.. js/firebase
                        functions
                        (httpsCallable (name k)))]
                (fn [data]
                  (-> data
                    pr-str
                    f
                    u/js<!
                    .-data
                    edn/read-string
                    go))))
    ks))

(defn chan? [x]
  (satisfies? cljs.core.async.impl.protocols/ReadPort x))

(defn wrap-fn [handler]
  (fn [data context]
    (let [[event data] (edn/read-string data)
          env (-> context
                (js->clj :keywordize-keys true)
                (assoc :event event))
          env (-> env
                (merge (prepend-keys "auth" (:auth env)))
                (dissoc :auth))
          result (handler env data)]
      (if (chan? result)
        (js/Promise.
          (fn [success]
            (take! result (comp success pr-str))))
        (pr-str result)))))
