(ns cows.macros)

(defmacro defcursors [db & forms]
  `(do
     ~@(for [[sym path] (partition 2 forms)]
         `(def ~sym (rum.core/cursor-in ~db ~path)))))
