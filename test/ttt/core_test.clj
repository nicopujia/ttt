(ns ttt.core-test
  (:require [clojure.string :as str]
            [clojure.test :refer [are deftest is testing]]
            [ttt.core :as core]))

(def ansi-mark-pattern #"\u001b\[[0-9;]*m([XO])\u001b\[[0-9;]*m")

(defn run-game-output [input]
  (let [reader (java.io.PushbackReader. (java.io.StringReader. input) 1)
        writer (java.io.StringWriter.)]
    (core/run-game! reader writer)
    (str writer)))

(defn screen-chunks [output]
  (str/split output (re-pattern (java.util.regex.Pattern/quote core/clear-screen))))

(defn rendered-screens [output]
  (vec (rest (screen-chunks output))))

(defn clear-screen-count [output]
  (count (rendered-screens output)))

(defn scoreboard-pattern [x-wins o-wins draws]
  (re-pattern (str "X wins: " x-wins "\\s+"
                   "O wins: " o-wins "\\s+"
                   "Draws: " draws)))

(defn ansi-mark-count [screen]
  (count (re-seq ansi-mark-pattern screen)))

(defn has-board? [screen]
  (str/includes? screen "---+---+---"))

(deftest pure-game-rules-are-covered
  (testing "only exact 1 through 9 are syntactically valid after trimming elsewhere"
    (are [text expected] (= expected (core/valid-move-text? text))
      "1" true
      "9" true
      "01" false
      "+1" false
      "1.0" false
      "10" false
      "0" false
      "x" false
      "" false))
  (testing "wins and draws cover representative row, column, and diagonal outcomes"
    (is (= :x (core/winner [:x :x :x nil nil nil nil nil nil])))
    (is (= :o (core/winner [:o nil nil :o nil nil :o nil nil])))
    (is (= :x (core/winner [:x nil nil nil :x nil nil nil :x])))
    (is (true? (core/draw? [:x :o :x :x :o :o :o :x :x]))))
  (testing "scoreboard labels and order are exact"
    (is (= "X wins: 2\nO wins: 1\nDraws: 3"
           (core/scoreboard-string {:x-wins 2 :o-wins 1 :draws 3}))))
  (testing "marks themselves are ANSI colorized without pinning a specific color"
    (is (re-matches ansi-mark-pattern (core/colorize-mark :x)))
    (is (re-matches ansi-mark-pattern (core/colorize-mark :o)))))

(deftest startup-welcome-first-board-and-turn-order
  (let [output (run-game-output "1\n 5 \n")
        screens (rendered-screens output)
        preamble (first (screen-chunks output))]
    (is (str/includes? preamble "Welcome"))
    (is (= 3 (clear-screen-count output)))
    (is (str/includes? (first screens) "1 | 2 | 3"))
    (is (str/includes? (first screens) "4 | 5 | 6"))
    (is (str/includes? (first screens) "7 | 8 | 9"))
    (is (str/includes? (first screens) "Player X, choose a cell (1-9): "))
    (is (str/includes? (second screens) "Player O, choose a cell (1-9): "))
    (is (str/includes? (nth screens 2) "Player X, choose a cell (1-9): "))
    (is (re-find ansi-mark-pattern (second screens)))
    (is (re-find ansi-mark-pattern (nth screens 2)))
    (is (str/includes? output "EOF received. Exiting."))))

(deftest move-validation-refreshes-and-trimming-are-covered
  (testing "blank, whitespace, numeric-looking, out-of-range, non-numeric, and occupied inputs retry on the same turn"
    (let [output (run-game-output "\n  \n01\n10\nhello\n1\n1\n2\n")
          screens (rendered-screens output)]
      (is (= 9 (clear-screen-count output)))
      (is (str/includes? (nth screens 1) "Player X, choose a cell (1-9): "))
      (is (str/includes? (nth screens 2) "Player X, choose a cell (1-9): "))
      (is (str/includes? (nth screens 3) "Player X, choose a cell (1-9): "))
      (is (str/includes? (nth screens 4) "Player X, choose a cell (1-9): "))
      (is (str/includes? (nth screens 5) "Player X, choose a cell (1-9): "))
      (is (every? #(re-find #"Enter an open cell using exactly 1 through 9\." %)
                  (map screens [1 2 3 4 5])))
      (is (str/includes? (nth screens 6) "Player O, choose a cell (1-9): "))
      (is (str/includes? (nth screens 7) "That cell is already occupied."))
      (is (str/includes? (nth screens 7) "Player O, choose a cell (1-9): "))
      (is (re-find ansi-mark-pattern (nth screens 7)))
      (is (str/includes? output "EOF received. Exiting."))))
  (testing "whitespace around valid moves is trimmed before applying the move"
    (let [output (run-game-output " 1 \n\t2\t\nn\n")
          screens (rendered-screens output)]
      (is (str/includes? (second screens) "Player O, choose a cell (1-9): "))
      (is (str/includes? (nth screens 2) "Player X, choose a cell (1-9): ")))))

(deftest final-outcomes-scoreboard-persistence-and-play-again-flow
  (testing "row, column, and diagonal wins are all detected in completed rounds"
    (doseq [[input outcome-text]
            [["1\n4\n2\n5\n3\nn\n" "Round result: X wins."]
             ["1\n2\n4\n5\n8\n3\n7\nn\n" "Round result: X wins."]
             ["1\n2\n5\n3\n9\nn\n" "Round result: X wins."]]]
      (let [output (run-game-output input)]
        (is (str/includes? output outcome-text))
        (is (re-find (scoreboard-pattern 1 0 0) output)))))
  (testing "draws update the scoreboard and final presentation"
    (let [output (run-game-output "1\n2\n3\n5\n4\n6\n8\n7\n9\nn\n")
          final-screen (last (rendered-screens output))]
      (is (= 10 (clear-screen-count output)))
      (is (has-board? final-screen))
      (is (= 9 (ansi-mark-count final-screen)))
      (is (str/includes? output "Round result: Draw."))
      (is (re-find (scoreboard-pattern 0 0 1) output))
      (is (str/includes? output "Play again? (y/yes/n/no): "))))
  (testing "play-again accepts case-insensitive trimmed yes/no and keeps scoreboard counts across rounds"
    (let [output (run-game-output "1\n4\n2\n5\n3\n YeS \n1\n2\n4\n5\n7\n No \n")]
      (is (= 12 (clear-screen-count output)))
      (is (>= (count (re-seq #"Player X, choose a cell \(1-9\):" output)) 3))
      (is (re-find (scoreboard-pattern 1 0 0) output))
      (is (re-find (scoreboard-pattern 2 0 0) output))
      (is (str/includes? output "Thanks for playing."))))
  (testing "invalid play-again refresh retains final board, outcome, scoreboard, validation, and prompt"
    (let [output (run-game-output "1\n4\n2\n5\n3\n\nmaybe\n n \n")
          screens (rendered-screens output)
          invalid-blank-screen (nth screens 6)
          invalid-text-screen (nth screens 7)]
      (is (has-board? invalid-blank-screen))
      (is (>= (ansi-mark-count invalid-blank-screen) 5))
      (is (str/includes? invalid-blank-screen "Round result: X wins."))
      (is (re-find (scoreboard-pattern 1 0 0) invalid-blank-screen))
      (is (str/includes? invalid-blank-screen "Please answer y/yes/n/no."))
      (is (str/includes? invalid-blank-screen "Play again? (y/yes/n/no): "))
      (is (has-board? invalid-text-screen))
      (is (>= (ansi-mark-count invalid-text-screen) 5))
      (is (str/includes? invalid-text-screen "Round result: X wins."))
      (is (re-find (scoreboard-pattern 1 0 0) invalid-text-screen))
      (is (str/includes? invalid-text-screen "Please answer y/yes/n/no."))
      (is (str/includes? invalid-text-screen "Play again? (y/yes/n/no): ")))))

(deftest clear-plus-redraw-transitions-are-emitted-under-captured-output
  (testing "required transitions emit clear codes and redraw the expected semantic content"
    (let [output (run-game-output "1\n4\n1\n2\n5\n3\n\nYES")
          screens (rendered-screens output)]
      (is (= 9 (clear-screen-count output)))
      (is (str/includes? (first screens) "Player X, choose a cell (1-9): "))
      (is (str/includes? (second screens) "Player O, choose a cell (1-9): "))
      (is (str/includes? (nth screens 3) "That cell is already occupied."))
      (is (str/includes? (nth screens 3) "Player X, choose a cell (1-9): "))
      (is (has-board? (nth screens 6)))
      (is (>= (ansi-mark-count (nth screens 6)) 5))
      (is (str/includes? (nth screens 6) "Round result: X wins."))
      (is (re-find (scoreboard-pattern 1 0 0) (nth screens 6)))
      (is (str/includes? (nth screens 7) "Please answer y/yes/n/no."))
      (is (re-find (scoreboard-pattern 1 0 0) (nth screens 7)))
      (is (str/includes? (nth screens 8) "Player X, choose a cell (1-9): "))
      (is (str/includes? (nth screens 8) "1 | 2 | 3"))
      (is (str/includes? output "EOF received. Exiting.")))))

(deftest eof-behavior-is-covered
  (testing "meaningless EOF exits gracefully at move and play-again prompts"
    (doseq [input ["" "   " "1\n4\n2\n5\n3\n"]]
      (let [output (run-game-output input)]
        (is (str/includes? output "EOF received. Exiting.")))))
  (testing "valid partial EOF move that does not end the round refreshes once, then exits without play-again"
    (let [output (run-game-output "1")
          screens (rendered-screens output)]
      (is (= 2 (clear-screen-count output)))
      (is (str/includes? (second screens) "Player O, choose a cell (1-9): "))
      (is (not (str/includes? output "Play again? (y/yes/n/no): ")))
      (is (str/includes? output "EOF received. Exiting."))))
  (testing "valid partial EOF move that wins exits after final board, outcome, and updated scoreboard"
    (let [output (run-game-output "1\n4\n2\n5\n3")
          final-screen (last (rendered-screens output))]
      (is (= 6 (clear-screen-count output)))
      (is (has-board? final-screen))
      (is (>= (ansi-mark-count final-screen) 5))
      (is (str/includes? output "Round result: X wins."))
      (is (re-find (scoreboard-pattern 1 0 0) output))
      (is (not (str/includes? output "Play again? (y/yes/n/no): ")))
      (is (str/includes? output "EOF received. Exiting."))))
  (testing "valid partial EOF move that draws exits after final board, outcome, and updated scoreboard"
    (let [output (run-game-output "1\n2\n3\n5\n4\n6\n8\n7\n9")
          final-screen (last (rendered-screens output))]
      (is (has-board? final-screen))
      (is (= 9 (ansi-mark-count final-screen)))
      (is (str/includes? output "Round result: Draw."))
      (is (re-find (scoreboard-pattern 0 0 1) output))
      (is (not (str/includes? output "Play again? (y/yes/n/no): ")))
      (is (str/includes? output "EOF received. Exiting."))))
  (testing "invalid partial EOF move retries normally before the immediate EOF exit"
    (let [output (run-game-output "01")
          screens (rendered-screens output)]
      (is (= 2 (clear-screen-count output)))
      (is (str/includes? (second screens) "Enter an open cell using exactly 1 through 9."))
      (is (str/includes? (second screens) "Player X, choose a cell (1-9): "))
      (is (str/includes? output "EOF received. Exiting."))))
  (testing "valid partial EOF play-again yes starts a new round, then the immediate EOF exits"
    (let [output (run-game-output "1\n4\n2\n5\n3\ny")
          screens (rendered-screens output)]
      (is (= 7 (clear-screen-count output)))
      (is (str/includes? (last screens) "Player X, choose a cell (1-9): "))
      (is (str/includes? output "EOF received. Exiting."))))
  (testing "valid partial EOF play-again no uses the normal goodbye path"
    (let [output (run-game-output "1\n4\n2\n5\n3\nNo")]
      (is (str/includes? output "Thanks for playing."))
      (is (not (str/includes? output "EOF received. Exiting.")))))
  (testing "invalid partial EOF play-again retries normally before the immediate EOF exit"
    (let [output (run-game-output "1\n4\n2\n5\n3\nmaybe")
          screens (rendered-screens output)]
      (is (= 7 (clear-screen-count output)))
      (is (str/includes? (last screens) "Please answer y/yes/n/no."))
      (is (str/includes? (last screens) "Play again? (y/yes/n/no): "))
      (is (str/includes? output "EOF received. Exiting.")))))