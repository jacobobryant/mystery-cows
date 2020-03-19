(ns cows.lib)

(defmacro capture-env [nspace]
  `(capture-env* (ns-publics ~nspace)))

(defmacro defcursors [db & forms]
  `(do
     ~@(for [[sym path] (partition 2 forms)]
         `(def ~sym (rum.core/cursor-in ~db ~path)))))

(defn flatten-form [form]
  (if (some #(% form)
        [list?
         #(instance? clojure.lang.IMapEntry %)
         seq?
         #(instance? clojure.lang.IRecord %)
         coll?])
    (mapcat flatten-form form)
    (list form)))

(defn derivations [sources nspace & forms]
  (->> (partition 2 forms)
    (reduce
      (fn [[defs sources] [sym form]]
        (let [deps (->> form
                     flatten-form
                     (map sources)
                     (filter some?)
                     distinct
                     vec)
              k (keyword (name nspace) (name sym))]
          [(conj defs `(def ~sym (rum.core/derived-atom ~deps ~k
                                   (fn ~deps ~form))))
           (conj sources sym)]))
      [[] (set sources)])
    first))

(defmacro defderivations [& args]
  `(do ~@(apply derivations args)))
