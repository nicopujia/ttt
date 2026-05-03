(ns ttt.core
  (:require [clojure.string :as str])
  (:gen-class))

(def clear-screen "\u001b[2J\u001b[H")
(def x-color "\u001b[31m")
(def o-color "\u001b[34m")
(def color-reset "\u001b[0m")

(def empty-board (vec (repeat 9 nil)))
(def empty-scoreboard {:x-wins 0 :o-wins 0 :draws 0})
(def winning-lines [[0 1 2] [3 4 5] [6 7 8]
                    [0 3 6] [1 4 7] [2 5 8]
                    [0 4 8] [2 4 6]])
(def move-pattern #"[1-9]")

(defn emit! [out text]
  (.write out text)
  (.flush out))

(defn emit-line! [out text]
  (emit! out (str text "\n")))

(defn other-player [player]
  (if (= player :x) :o :x))

(defn mark-label [player]
  (if (= player :x) "X" "O"))

(defn colorize-mark [player]
  (str (if (= player :x) x-color o-color)
       (mark-label player)
       color-reset))

(defn cell-display [index cell]
  (if cell
    (colorize-mark cell)
    (str (inc index))))

(defn board-string [board]
  (str/join
   "\n---+---+---\n"
   (for [row (partition 3 (map-indexed cell-display board))]
     (str " " (str/join " | " row) " "))))

(defn scoreboard-string [{:keys [x-wins o-wins draws]}]
  (str "X wins: " x-wins "\n"
       "O wins: " o-wins "\n"
       "Draws: " draws))

(defn welcome-string []
  (str "Welcome to terminal tic-tac-toe!\n"
       "Players alternate entering cells 1-9. X starts every round."))

(defn move-prompt [player]
  (str "Player " (mark-label player) ", choose a cell (1-9): "))

(defn play-again-prompt []
  "Play again? (y/yes/n/no): ")

(defn outcome-string [outcome]
  (if (= outcome :draw)
    "Round result: Draw."
    (str "Round result: " (mark-label outcome) " wins.")))

(defn render-screen! [out sections]
  (emit! out (str clear-screen (str/join "\n\n" sections) "\n")))

(defn winner [board]
  (some (fn [[a b c]]
          (let [mark (nth board a)]
            (when (and mark (= mark (nth board b)) (= mark (nth board c)))
              mark)))
        winning-lines))

(defn draw? [board]
  (and (every? some? board)
       (nil? (winner board))))

(defn valid-move-text? [text]
  (boolean (re-matches move-pattern text)))

(defn update-scoreboard [scoreboard outcome]
  (case outcome
    :x (update scoreboard :x-wins inc)
    :o (update scoreboard :o-wins inc)
    :draw (update scoreboard :draws inc)))

(defn move-screen-sections [board player validation]
  (cond-> [(board-string board)]
    validation (conj validation)
    true (conj (move-prompt player))))

(defn final-screen-sections [board outcome scoreboard validation]
  (cond-> [(board-string board)
           (outcome-string outcome)
           (scoreboard-string scoreboard)]
    validation (conj validation)
    true (conj (play-again-prompt))))

(defn read-input! [reader]
  (loop [chars []]
    (let [code-point (.read reader)]
      (cond
        (= code-point -1)
        (if (empty? chars)
          {:kind :eof :text ""}
          {:kind :eof-partial :text (apply str chars)})

        (= (char code-point) \newline)
        {:kind :line :text (apply str chars)}

        (= (char code-point) \return)
        (let [next-code-point (.read reader)]
          (when (and (not= next-code-point -1)
                     (not= (char next-code-point) \newline))
            (.unread reader next-code-point))
          {:kind :line :text (apply str chars)})

        :else
        (recur (conj chars (char code-point)))))))

(declare play-round!)

(defn handle-play-again! [reader out board outcome scoreboard]
  (let [{:keys [kind text]} (read-input! reader)
        trimmed (str/trim text)]
    (cond
      (or (= kind :eof)
          (and (= kind :eof-partial) (str/blank? trimmed)))
      {:status :exit :message "EOF received. Exiting." :scoreboard scoreboard}

      (str/blank? trimmed)
      (do
        (render-screen! out (final-screen-sections board outcome scoreboard "Please answer y/yes/n/no."))
        (recur reader out board outcome scoreboard))

      :else
      (let [answer (str/lower-case trimmed)]
        (cond
          (#{"y" "yes"} answer)
          (do
            (render-screen! out (move-screen-sections empty-board :x nil))
            (play-round! reader out scoreboard))

          (#{"n" "no"} answer)
          {:status :goodbye :message "Thanks for playing." :scoreboard scoreboard}

          :else
          (do
            (render-screen! out (final-screen-sections board outcome scoreboard "Please answer y/yes/n/no."))
            (recur reader out board outcome scoreboard)))))))

(defn handle-move-input [board player trimmed]
  (cond
    (not (valid-move-text? trimmed))
    {:status :invalid :message "Enter an open cell using exactly 1 through 9."}

    :else
    (let [index (dec (Integer/parseInt trimmed))]
      (if (some? (nth board index))
        {:status :invalid :message "That cell is already occupied."}
        {:status :ok :board (assoc board index player)}))))

(defn play-round! [reader out scoreboard]
  (loop [board empty-board
         player :x]
    (let [{:keys [kind text]} (read-input! reader)
          trimmed (str/trim text)]
      (cond
        (or (= kind :eof)
            (and (= kind :eof-partial) (str/blank? trimmed)))
        {:status :exit :message "EOF received. Exiting." :scoreboard scoreboard}

        (str/blank? trimmed)
        (do
          (render-screen! out (move-screen-sections board player "Enter an open cell using exactly 1 through 9."))
          (recur board player))

        :else
        (let [{status :status next-board :board message :message}
              (handle-move-input board player trimmed)]
          (if (= status :invalid)
            (do
              (render-screen! out (move-screen-sections board player message))
              (recur board player))
            (let [outcome (or (winner next-board)
                              (when (draw? next-board) :draw))]
              (if outcome
                (let [scoreboard' (update-scoreboard scoreboard outcome)]
                  (if (= kind :eof-partial)
                    (do
                      (render-screen! out [(board-string next-board)
                                           (outcome-string outcome)
                                           (scoreboard-string scoreboard')])
                      {:status :exit :message "EOF received. Exiting." :scoreboard scoreboard'})
                    (do
                      (render-screen! out (final-screen-sections next-board outcome scoreboard' nil))
                      (handle-play-again! reader out next-board outcome scoreboard'))))
                (let [next-player (other-player player)]
                  (render-screen! out (move-screen-sections next-board next-player nil))
                  (recur next-board next-player))))))))))

(defn run-game! [reader out]
  (emit-line! out (welcome-string))
  (render-screen! out (move-screen-sections empty-board :x nil))
  (let [{:keys [message]} (play-round! reader out empty-scoreboard)]
    (emit-line! out message)))

(defn -main []
  (run-game! (java.io.PushbackReader. *in* 1) *out*))
