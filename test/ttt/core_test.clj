(ns ttt.core-test
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [are deftest is testing]]
            [ttt.core :as core]))

(def ansi-mark-pattern #"\u001b\[[0-9;]*m([XO])\u001b\[[0-9;]*m")

(defn run-game-output [input]
  (let [reader (java.io.PushbackReader. (java.io.StringReader. input) 1)
        writer (java.io.StringWriter.)]
    (core/run-game! reader writer)
    (str writer)))

(defn run-cli-process [input]
  (shell/sh "clojure" "-M:run" :in input))

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

(defn strip-ansi [text]
  (str/replace text #"\u001b\[[0-9;]*m" ""))

(defn screen-sections [screen]
  (str/split screen #"\n\n"))

(defn board-section [screen]
  (first (screen-sections screen)))

(defn board-token-count [screen]
  (count (re-seq #"\b(?:[1-9]|X|O)\b" (strip-ansi (board-section screen)))))

(defn has-board? [screen]
  (= 9 (board-token-count screen)))

(defn move-prompt-for? [screen player]
  (boolean (re-find (re-pattern (str "(?is)player\\s+" (name player) ".*1-9"))
                    screen)))

(defn play-again-prompt? [screen]
  (boolean (re-find #"(?is)play\s+again.*y.*yes.*n.*no" screen)))

(defn scoreboard-screen? [screen x-wins o-wins draws]
  (boolean (re-find (scoreboard-pattern x-wins o-wins draws) screen)))

(defn win-outcome-screen? [screen winner]
  (boolean (re-find (re-pattern (str "(?is)\\b" (str/upper-case (name winner)) "\\b.*win"))
                    (strip-ansi screen))))

(defn draw-outcome-screen? [screen]
  (boolean (re-find #"(?is)draw" (strip-ansi screen))))

(defn invalid-retry-screen? [screen expected-sections]
  (let [sections (screen-sections screen)]
    (and (= expected-sections (count sections))
         (every? (complement str/blank?) sections))))

(defn validation-section [screen]
  (let [sections (screen-sections screen)]
    (nth sections (- (count sections) 2))))

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
    (is (re-find #"(?i)welcome|tic-tac-toe" preamble))
    (is (= 3 (clear-screen-count output)))
    (is (has-board? (first screens)))
    (is (re-find #"(?s)\b1\b.*\b2\b.*\b3\b.*\b4\b.*\b5\b.*\b6\b.*\b7\b.*\b8\b.*\b9\b"
                 (strip-ansi (board-section (first screens)))))
    (is (move-prompt-for? (first screens) :x))
    (is (move-prompt-for? (second screens) :o))
    (is (move-prompt-for? (nth screens 2) :x))
    (is (re-find ansi-mark-pattern (second screens)))
    (is (re-find ansi-mark-pattern (nth screens 2)))
    (is (str/includes? output "EOF received. Exiting."))))

(deftest move-validation-refreshes-and-trimming-are-covered
  (testing "blank, whitespace, numeric-looking, out-of-range, non-numeric, and occupied inputs retry on the same turn"
    (let [output (run-game-output "\n  \n01\n10\nhello\n1\n1\n2\n")
          screens (rendered-screens output)]
      (is (= 9 (clear-screen-count output)))
      (is (every? #(move-prompt-for? % :x) (map screens [1 2 3 4 5])))
      (is (every? #(invalid-retry-screen? % 3) (map screens [1 2 3 4 5])))
      (is (every? #(not (str/blank? (validation-section %))) (map screens [1 2 3 4 5])))
      (is (move-prompt-for? (nth screens 6) :o))
      (is (invalid-retry-screen? (nth screens 7) 3))
      (is (not (str/blank? (validation-section (nth screens 7)))))
      (is (move-prompt-for? (nth screens 7) :o))
      (is (re-find ansi-mark-pattern (nth screens 7)))
      (is (str/includes? output "EOF received. Exiting."))))
  (testing "whitespace around valid moves is trimmed before applying the move"
    (let [output (run-game-output " 1 \n\t2\t\nn\n")
          screens (rendered-screens output)]
      (is (move-prompt-for? (second screens) :o))
      (is (move-prompt-for? (nth screens 2) :x)))))

(deftest final-outcomes-scoreboard-persistence-and-play-again-flow
  (testing "row, column, and diagonal wins are all detected in completed rounds"
    (doseq [input ["1\n4\n2\n5\n3\nn\n"
                   "1\n2\n4\n5\n8\n3\n7\nn\n"
                   "1\n2\n5\n3\n9\nn\n"]]
      (let [output (run-game-output input)
            final-screen (last (rendered-screens output))]
        (is (win-outcome-screen? final-screen :x))
        (is (scoreboard-screen? final-screen 1 0 0)))))
  (testing "draws update the scoreboard and final presentation"
    (let [output (run-game-output "1\n2\n3\n5\n4\n6\n8\n7\n9\nn\n")
          final-screen (last (rendered-screens output))]
      (is (= 10 (clear-screen-count output)))
      (is (has-board? final-screen))
      (is (= 9 (ansi-mark-count final-screen)))
      (is (draw-outcome-screen? final-screen))
      (is (scoreboard-screen? final-screen 0 0 1))
      (is (play-again-prompt? final-screen))))
  (testing "play-again accepts case-insensitive trimmed yes/no and keeps scoreboard counts across rounds"
    (let [output (run-game-output "1\n4\n2\n5\n3\n YeS \n1\n2\n4\n5\n7\n No \n")]
      (is (= 12 (clear-screen-count output)))
      (is (>= (count (filter #(move-prompt-for? % :x) (rendered-screens output))) 3))
      (is (scoreboard-screen? output 1 0 0))
      (is (scoreboard-screen? output 2 0 0))
      (is (str/includes? output "Thanks for playing."))))
  (testing "invalid play-again refresh retains final board, outcome, scoreboard, validation, and prompt"
    (let [output (run-game-output "1\n4\n2\n5\n3\n\n  \nmaybe\n n \n")
          screens (rendered-screens output)
          invalid-blank-screen (nth screens 6)
          invalid-whitespace-screen (nth screens 7)
          invalid-text-screen (nth screens 8)]
      (doseq [screen [invalid-blank-screen invalid-whitespace-screen invalid-text-screen]]
        (is (has-board? screen))
        (is (>= (ansi-mark-count screen) 5))
        (is (win-outcome-screen? screen :x))
        (is (scoreboard-screen? screen 1 0 0))
        (is (play-again-prompt? screen))
        (is (invalid-retry-screen? screen 5))
        (is (not (str/blank? (validation-section screen))))))))

(deftest clear-plus-redraw-transitions-are-emitted-under-captured-output
  (testing "required transitions emit clear codes and redraw the expected semantic content"
    (let [output (run-game-output "1\n4\n1\n2\n5\n3\n\nYES")
          screens (rendered-screens output)]
      (is (= 9 (clear-screen-count output)))
      (is (move-prompt-for? (first screens) :x))
      (is (move-prompt-for? (second screens) :o))
      (is (invalid-retry-screen? (nth screens 3) 3))
      (is (not (str/blank? (validation-section (nth screens 3)))))
      (is (move-prompt-for? (nth screens 3) :x))
      (is (has-board? (nth screens 6)))
      (is (>= (ansi-mark-count (nth screens 6)) 5))
      (is (win-outcome-screen? (nth screens 6) :x))
      (is (scoreboard-screen? (nth screens 6) 1 0 0))
      (is (invalid-retry-screen? (nth screens 7) 5))
      (is (scoreboard-screen? (nth screens 7) 1 0 0))
      (is (move-prompt-for? (nth screens 8) :x))
      (is (has-board? (nth screens 8)))
      (is (str/includes? output "EOF received. Exiting.")))))

(deftest eof-behavior-is-covered
  (testing "meaningless EOF exits gracefully at move and play-again prompts"
    (doseq [input ["" "   " "1\n4\n2\n5\n3\n"]]
      (let [output (run-game-output input)]
        (is (str/includes? output "EOF received. Exiting.")))))
  (testing "graceful EOF paths return status 0 from the CLI"
    (doseq [input ["" "   " "1" "1\n4\n2\n5\n3\n"]]
      (let [{:keys [exit out err]} (run-cli-process input)]
        (is (= 0 exit))
        (is (str/includes? (str out err) "EOF received. Exiting.")))))
  (testing "valid partial EOF move that does not end the round refreshes once, then exits without play-again"
    (let [output (run-game-output "1")
          screens (rendered-screens output)]
      (is (= 2 (clear-screen-count output)))
      (is (move-prompt-for? (second screens) :o))
      (is (not (play-again-prompt? output)))
      (is (str/includes? output "EOF received. Exiting."))))
  (testing "valid partial EOF move that wins exits after final board, outcome, and updated scoreboard"
    (let [output (run-game-output "1\n4\n2\n5\n3")
          final-screen (last (rendered-screens output))]
      (is (= 6 (clear-screen-count output)))
      (is (has-board? final-screen))
      (is (>= (ansi-mark-count final-screen) 5))
      (is (win-outcome-screen? final-screen :x))
      (is (scoreboard-screen? output 1 0 0))
      (is (not (play-again-prompt? output)))
      (is (str/includes? output "EOF received. Exiting."))))
  (testing "valid partial EOF move that draws exits after final board, outcome, and updated scoreboard"
    (let [output (run-game-output "1\n2\n3\n5\n4\n6\n8\n7\n9")
          final-screen (last (rendered-screens output))]
      (is (has-board? final-screen))
      (is (= 9 (ansi-mark-count final-screen)))
      (is (draw-outcome-screen? final-screen))
      (is (scoreboard-screen? output 0 0 1))
      (is (not (play-again-prompt? output)))
      (is (str/includes? output "EOF received. Exiting."))))
  (testing "invalid partial EOF move retries normally before the immediate EOF exit"
    (let [output (run-game-output "01")
          screens (rendered-screens output)]
      (is (= 2 (clear-screen-count output)))
      (is (invalid-retry-screen? (second screens) 3))
      (is (not (str/blank? (validation-section (second screens)))))
      (is (move-prompt-for? (second screens) :x))
      (is (str/includes? output "EOF received. Exiting."))))
  (testing "valid partial EOF play-again y and yes start a new round, then the immediate EOF exits"
    (doseq [input ["1\n4\n2\n5\n3\ny"
                   "1\n4\n2\n5\n3\nyes"]]
      (let [output (run-game-output input)
            screens (rendered-screens output)]
        (is (= 7 (clear-screen-count output)))
        (is (move-prompt-for? (last screens) :x))
        (is (str/includes? output "EOF received. Exiting.")))))
  (testing "valid partial EOF play-again n and no use the normal goodbye path"
    (doseq [input ["1\n4\n2\n5\n3\nn"
                   "1\n4\n2\n5\n3\nNo"]]
      (let [output (run-game-output input)]
        (is (str/includes? output "Thanks for playing."))
        (is (not (str/includes? output "EOF received. Exiting."))))))
  (testing "invalid partial EOF play-again retries normally before the immediate EOF exit"
    (let [output (run-game-output "1\n4\n2\n5\n3\nmaybe")
          screens (rendered-screens output)]
      (is (= 7 (clear-screen-count output)))
      (is (play-again-prompt? (last screens)))
      (is (invalid-retry-screen? (last screens) 5))
      (is (not (str/blank? (validation-section (last screens)))))
      (is (str/includes? output "EOF received. Exiting.")))))