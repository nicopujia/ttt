(ns ttt.game)

(defn empty-board
  []
  (vec (repeat 9 nil)))

(defn board?
  [board]
  (and (vector? board)
       (= 9 (count board))
       (every? #(contains? #{nil :x :o} %) board)))
