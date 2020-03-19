(ns cows.macros)

(defmacro defcursors [db & forms]
  `(do
     ~@(for [[sym path] (partition 2 forms)]
         `(def ~sym (rum.core/cursor-in ~db ~path)))))

(defmacro defderivations [& forms]
  `(do
     ~@(for [[[k & deps] expr] (partition 2 forms)
             :let [deps (vec deps)]]
         `(def ~(symbol (name k))
            (rum.core/derived-atom ~deps ~k
              (fn ~deps ~expr))))))

