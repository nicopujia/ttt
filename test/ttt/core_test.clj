(ns ttt.core-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ttt.core :as core]))

(defn run-game-output [input]
  (let [reader (java.io.PushbackReader. (java.io.StringReader. input) 1)
        writer (java.io.StringWriter.)]
    (core/run-game! reader writer)
    (str writer)))

(deftest move-validation-is-strict
  (testing "only exact 1 through 9 are syntactically valid"
    (is (true? (core/valid-move-text? "1")))
    (is (true? (core/valid-move-text? "9")))
    (is (false? (core/valid-move-text? "01")))
    (is (false? (core/valid-move-text? "+1")))
    (is (false? (core/valid-move-text? "1.0")))))

(deftest winner-detection-covers-lines-and-draw
  (is (= :x (core/winner [:x :x :x nil nil nil nil nil nil])))
  (is (= :o (core/winner [:o nil nil :o nil nil :o nil nil])))
  (is (= :x (core/winner [:x nil nil nil :x nil nil nil :x])))
  (is (true? (core/draw? [:x :o :x :x :o :o :o :x :x]))))

(deftest full-game-renders-board-scoreboard-and-goodbye
  (let [output (run-game-output "1\n4\n2\n5\n3\nn\n")]
    (is (str/includes? output "Welcome to terminal tic-tac-toe!"))
    (is (str/includes? output core/clear-screen))
    (is (re-find #"1 \| 2 \| 3" output))
    (is (str/includes? output (str core/x-color "X" core/color-reset)))
    (is (str/includes? output (str core/o-color "O" core/color-reset)))
    (is (str/includes? output "Round result: X wins."))
    (is (re-find #"X wins: 1\s+O wins: 0\s+Draws: 0" output))
    (is (str/includes? output "Thanks for playing."))))

(deftest occupied-cell-retry-keeps-same-player-and-board
  (let [output (run-game-output "1\n1\n2")]
    (is (str/includes? output "That cell is already occupied."))
    (is (re-find #"\x1b\[2J\x1b\[H .*X.*\n---\+---\+---\n 4 \| 5 \| 6" output))
    (is (>= (count (re-seq #"Player O, choose a cell \(1-9\):" output)) 2))
    (is (str/includes? output "EOF received. Exiting."))))

(deftest partial-eof-move-exits-gracefully-after-refresh
  (let [output (run-game-output "1")]
    (is (str/includes? output "Player O, choose a cell (1-9): "))
    (is (str/includes? output "EOF received. Exiting."))))
