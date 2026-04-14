(ns ttt.core
  (:require [ttt.game :as game]))

(defn startup-message
  []
  (str "ttt scaffold ready with " (count (game/empty-board)) " cells."))

(defn -main
  [& _args]
  (println (startup-message)))
