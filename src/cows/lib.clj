(ns cows.lib)

(defmacro capture-env [nspace]
  `(capture-env* (ns-publics ~nspace)))
