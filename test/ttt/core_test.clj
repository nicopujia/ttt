(ns ttt.core-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests testing]]
            [ttt.core :as core]))

(def winning-positions
  [[1 2 3]
   [4 5 6]
   [7 8 9]
   [1 4 7]
   [2 5 8]
   [3 6 9]
   [1 5 9]
   [3 5 7]])

(def draw-board
  [:x :o :x
   :x :o :o
   :o :x :x])

(defn board-with-moves
  [moves]
  (reduce (fn [board [position player]]
            (core/make-move board position player))
          (core/new-board)
          moves))

(defn board-with-player-positions
  [player positions]
  (reduce (fn [board position]
            (assoc board (dec position) player))
          (core/new-board)
          positions))

(deftest new-board-test
  (let [board (core/new-board)]
    (is (vector? board))
    (is (= 9 (count board)))
    (is (every? nil? board))))

(deftest valid-move?-test
  (testing "open positions are valid"
    (doseq [position [1 5 9]]
      (is (true? (core/valid-move? (core/new-board) position)))))
  (testing "occupied positions are invalid"
    (let [board (core/make-move (core/new-board) 5 :x)]
      (is (false? (core/valid-move? board 5)))))
  (testing "out-of-range positions are invalid"
    (doseq [position [0 10 -1]]
      (is (false? (core/valid-move? (core/new-board) position)))))
  (testing "non-integer positions are invalid"
    (doseq [position [nil "1" 1.0 :one]]
      (is (false? (core/valid-move? (core/new-board) position)))))
  (testing "malformed boards are invalid"
    (doseq [board [nil [] [nil nil nil] [:x :o :z nil nil nil nil nil nil]]]
      (is (false? (core/valid-move? board 1))))))

(deftest make-move-test
  (testing "make-move maps positions to zero-based indexes"
    (doseq [position (range 1 10)]
      (let [board (core/make-move (core/new-board) position :x)
            expected-board (assoc (core/new-board) (dec position) :x)]
        (is (= expected-board board)))))
  (testing "make-move does not mutate the original board"
    (let [board (core/new-board)
          updated-board (core/make-move board 5 :x)]
      (is (= (core/new-board) board))
      (is (= [nil nil nil nil :x nil nil nil nil] updated-board))))
  (testing "sequential legal moves produce the expected board"
    (is (= [:x nil nil nil :o nil nil nil :x]
           (board-with-moves [[1 :x] [5 :o] [9 :x]]))))
  (testing "invalid moves throw"
    (let [board (core/make-move (core/new-board) 5 :x)]
      (is (thrown? clojure.lang.ExceptionInfo
                   (core/make-move board 5 :o)))
      (is (thrown? clojure.lang.ExceptionInfo
                   (core/make-move board 0 :o)))
      (is (thrown? clojure.lang.ExceptionInfo
                   (core/make-move board 1 :z)))
      (is (thrown? clojure.lang.ExceptionInfo
                   (core/make-move [nil nil nil] 1 :x))))))

(deftest winner-test
  (testing "all winning lines are detected for both players"
    (doseq [player [:x :o]
            line winning-positions]
      (is (= player
             (core/winner (board-with-player-positions player line))))))
  (testing "non-winning boards return nil"
    (is (nil? (core/winner (board-with-moves [[1 :x] [2 :x] [5 :o]]))))
    (is (nil? (core/winner draw-board)))
    (is (nil? (core/winner [nil nil nil])))))

(deftest full?-test
  (is (false? (core/full? (core/new-board))))
  (is (true? (core/full? draw-board)))
  (is (true? (core/full? [:x :x :x :o :o :x :o :x :o])))
  (is (false? (core/full? [nil nil nil]))))

(deftest game-over?-test
  (let [winning-board (board-with-moves [[1 :x] [2 :o] [5 :x] [3 :o] [9 :x]])]
    (is (true? (core/game-over? winning-board)))
    (is (true? (core/game-over? draw-board)))
    (is (false? (core/game-over? (board-with-moves [[1 :x] [5 :o]]))))
    (is (true? (core/game-over? (board-with-moves [[1 :x] [2 :x] [3 :x]]))))
    (is (false? (core/game-over? [nil nil nil])))))

(deftest next-player-test
  (is (= :o (core/next-player :x)))
  (is (= :x (core/next-player :o)))
  (is (thrown? clojure.lang.ExceptionInfo
               (core/next-player :z))))

(defn- strip-ansi
  "Remove ANSI escape codes from text"
  [text]
  (str/replace text #"\u001B\[[0-9;]*m" ""))

(deftest render-board-test
  (testing "an empty board renders numbered cells and the turn header"
    (is (= "Tic Tac Toe\n\nX's turn\n\n 1 | 2 | 3 \n---+---+---\n 4 | 5 | 6 \n---+---+---\n 7 | 8 | 9 "
           (strip-ansi (core/render-board (core/new-board) :x)))))
  (testing "a partial board renders symbols in centered cells"
    (let [board (board-with-moves [[1 :x] [2 :o] [5 :x] [7 :o]])]
      (is (= "Tic Tac Toe\n\nO's turn\n\n X | O | 3 \n---+---+---\n 4 | X | 6 \n---+---+---\n O | 8 | 9 "
             (strip-ansi (core/render-board board :o))))))
  (testing "render-board validates inputs"
    (is (thrown? clojure.lang.ExceptionInfo
                 (core/render-board [nil nil nil] :x)))
    (is (thrown? clojure.lang.ExceptionInfo
                 (core/render-board (core/new-board) :z)))))

(deftest parse-position-test
  (is (= 1 (core/parse-position "1")))
  (is (= 9 (core/parse-position " 9 ")))
  (doseq [input [nil "" "  " "0" "10" "1.0" "x" "1x"]]
    (is (nil? (core/parse-position input)))))

(deftest validate-input-test
  (testing "open cells return a parsed position"
    (is (= {:position 5}
           (core/validate-input (core/new-board) "5"))))
  (testing "occupied cells return a descriptive error"
    (let [board (core/make-move (core/new-board) 5 :x)]
      (is (= {:error core/occupied-cell-message}
             (core/validate-input board "5")))))
  (testing "malformed input returns a descriptive error"
    (is (= {:error core/invalid-input-message}
           (core/validate-input (core/new-board) "ten")))))

(deftest final-message-test
  (is (= "X wins! Game over."
         (strip-ansi (core/final-message (board-with-moves [[1 :x] [4 :o] [2 :x] [5 :o] [3 :x]])))))
  (is (= "It's a draw! Game over."
         (strip-ansi (core/final-message draw-board))))
  (is (thrown? clojure.lang.ExceptionInfo
               (core/final-message (core/new-board)))))

(deftest parse-play-again-choice-test
  (doseq [input ["y" "Y" " y "]]
    (is (= :yes (core/parse-play-again-choice input))))
  (doseq [input ["n" "N" " n "]]
    (is (= :no (core/parse-play-again-choice input))))
  (doseq [input [nil "" "  " "yes" "no" "1" "maybe"]]
    (is (nil? (core/parse-play-again-choice input)))))

(deftest update-scores-test
  (is (= {:x 1 :o 0 :draws 0}
         (core/update-scores core/initial-scores {:status :won :winner :x})))
  (is (= {:x 0 :o 1 :draws 0}
         (core/update-scores core/initial-scores {:status :won :winner :o})))
  (is (= {:x 0 :o 0 :draws 1}
         (core/update-scores core/initial-scores {:status :draw})))
  (is (= core/initial-scores
         (core/update-scores core/initial-scores {:status :playing}))))

(deftest render-score-test
  (is (= "Score (X : O : Draws) 0 : 0 : 0"
         (core/render-score core/initial-scores)))
  (is (= "Score (X : O : Draws) 2 : 1 : 3"
         (core/render-score {:x 2 :o 1 :draws 3}))))

(deftest cli-win-flow-test
  (let [output (with-in-str "1\n4\n2\n5\n3\n"
                 (with-out-str (core/-main)))
        stripped-output (strip-ansi output)]
    (is (str/includes? stripped-output "X wins! Game over."))
    (is (str/includes? stripped-output " X | X | X "))
    (is (= 3 (count (re-seq #"X's turn\. Enter 1-9: " stripped-output))))
    (is (= 2 (count (re-seq #"O's turn\. Enter 1-9: " stripped-output))))))

(deftest cli-draw-flow-test
  (let [output (with-in-str "1\n2\n3\n5\n4\n6\n8\n7\n9\n"
                 (with-out-str (core/-main)))
        stripped-output (strip-ansi output)]
    (is (str/includes? stripped-output "It's a draw! Game over."))
    (is (= 5 (count (re-seq #"X's turn\. Enter 1-9: " stripped-output))))
    (is (= 4 (count (re-seq #"O's turn\. Enter 1-9: " stripped-output))))))

(deftest cli-invalid-input-reprompt-test
  (let [output (with-in-str "q\n10\n1\n4\n2\n5\n3\n"
                 (with-out-str (core/-main)))
        stripped-output (strip-ansi output)]
    (is (= 2 (count (re-seq #"Invalid input\. Enter a number 1-9\." stripped-output))))
    (is (str/includes? stripped-output "X wins! Game over."))
    (is (= 5 (count (re-seq #"X's turn\. Enter 1-9: " stripped-output))))
    (is (= 2 (count (re-seq #"O's turn\. Enter 1-9: " stripped-output))))))

(deftest cli-occupied-cell-reprompt-test
  (let [output (with-in-str "1\n1\n4\n2\n5\n3\n"
                 (with-out-str (core/-main)))
        stripped-output (strip-ansi output)]
    (is (= 1 (count (re-seq #"Cell already occupied\. Choose another position\." stripped-output))))
    (is (str/includes? stripped-output "X wins! Game over."))
    (is (= 3 (count (re-seq #"X's turn\. Enter 1-9: " stripped-output))))
    (is (= 3 (count (re-seq #"O's turn\. Enter 1-9: " stripped-output))))))

(defn -main
  [& _args]
  (let [{:keys [fail error]} (run-tests 'ttt.core-test)]
    (System/exit (if (zero? (+ fail error)) 0 1))))
