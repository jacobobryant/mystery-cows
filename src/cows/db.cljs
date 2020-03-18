(ns cows.db
  (:require
    [cows.macros :refer [defcursors]]))

(defonce db (atom {}))

(defcursors db
  uid [:ui :uid]
  email [:ui :email]
  games [:games]
  messages [:messages])
