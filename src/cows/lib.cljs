(ns cows.lib
  (:require-macros
    [cows.lib]
    [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [cljs.core.async :refer [take!]]
    [clojure.edn :as edn]
    [rum.core]
    [clojure.set :as set]
    [cljs.core.async :refer [close!]]
    [trident.util :as u]))

(defn maintain-subscriptions
  "Watch for changes in a set of subscriptions (stored in sub-atom), subscribing
  and unsubscribing accordingly."
  [sub-atom sub-fn]
  (let [sub->chan (atom {})
        watch (fn [_ _ _ new-subs]
                (let [old-subs (set (keys @sub->chan))
                      old-subs (set/difference old-subs new-subs)
                      new-subs (set/difference new-subs old-subs)]
                  (swap! sub->chan merge (u/map-to sub-fn new-subs))
                  (doseq [channel (map @sub->chan old-subs)]
                    (close! channel))
                  (swap! sub->chan #(apply dissoc % old-subs))))]
    (add-watch sub-atom ::maintain-subscriptions watch)
    (watch nil nil nil @sub-atom)))

(defn merge-subscription!
  "Continually merge results from subscription into sub-data-atom. Data from closed
  subscriptions is removed, but only after any new subscriptions have received their
  initial results."
  [{:keys [state-atom sub-data-atom merge-result sub-key sub-channel]}]
  (swap! state-atom update ::pending-subs (fnil conj #{}) sub-key)
  (go-loop []
    (when (and (not-empty (::gc-subs @state-atom)) (empty? (::pending-subs @state-atom)))
      (apply swap! sub-data-atom dissoc (::gc-subs @state-atom))
      (swap! state-atom dissoc ::gc-subs))
    (if-some [result (<! sub-channel)]
      (do
        (swap! state-atom update ::pending-subs disj sub-key)
        (swap! state-atom update ::gc-subs disj sub-key)
        (swap! sub-data-atom update sub-key merge-result result)
        (recur))
      (swap! state-atom update ::gc-subs (fnil conj #{}) sub-key))))

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
      (u/pprint env)
      (if (chan? result)
        (js/Promise.
          (fn [success]
            (take! result (comp success pr-str))))
        (pr-str result)))))
