(ns ttt.core
  (:require [clojure.string :as str]
            [ttt.color :as color]
            [ttt.game :as game]))

(def board-size 9)

(def winning-lines
  [[0 1 2]
   [3 4 5]
   [6 7 8]
   [0 3 6]
   [1 4 7]
   [2 5 8]
   [0 4 8]
   [2 4 6]])

(def players #{:x :o})

(def title-line "Tic Tac Toe")

(def row-separator (color/dim-style "---+---+---"))

(def invalid-input-message (color/x-style "Invalid input. Enter a number 1-9."))

(def occupied-cell-message (color/x-style "Cell already occupied. Choose another position."))

(def no-input-message "No input received. Exiting.")

(def initial-scores {:x 0 :o 0 :draws 0})

(defn new-board
  []
  (game/empty-board))

(defn- valid-position?
  [position]
  (and (integer? position)
       (<= 1 position board-size)))

(defn- position->index
  [position]
  (dec position))

(defn valid-move?
  [board position]
  (and (game/board? board)
       (valid-position? position)
       (nil? (nth board (position->index position)))))

(defn make-move
  [board position player]
  (cond
    (not (game/board? board))
    (throw (ex-info "Invalid board" {:board board}))

    (not (players player))
    (throw (ex-info "Invalid player" {:player player}))

    (not (valid-position? position))
    (throw (ex-info "Invalid position" {:position position}))

    (not (valid-move? board position))
    (throw (ex-info "Invalid move" {:board board :position position :player player}))

    :else
    (assoc board (position->index position) player)))

(defn winner
  [board]
  (when (game/board? board)
    (some (fn [[a b c]]
            (let [line [(nth board a) (nth board b) (nth board c)]
                  player (first line)]
              (when (and player
                         (= line [player player player]))
                player)))
          winning-lines)))

(defn full?
  [board]
  (and (game/board? board)
       (not-any? nil? board)))

(defn game-over?
  [board]
  (boolean (or (winner board)
               (full? board))))

(defn- player->label
  [player]
  (case player
    :x "X"
    :o "O"
    (throw (ex-info "Invalid player" {:player player}))))

(defn next-player
  [player]
  (case player
    :x :o
    :o :x
    (throw (ex-info "Invalid player" {:player player}))))

(defn- cell-display
  [board position winning-positions]
  (let [cell-value (nth board (position->index position))
        is-winning-cell? (contains? winning-positions position)]
    (case cell-value
      :x (let [styled (color/x-style " X ")]
           (if is-winning-cell?
             (color/win-highlight styled)
             styled))
      :o (let [styled (color/o-style " O ")]
           (if is-winning-cell?
             (color/win-highlight styled)
             styled))
      (if is-winning-cell?
        (color/win-highlight (format " %d " position))
        (color/dim-style (format " %d " position))))))

(defn- winning-positions
  [board]
  (when (game/board? board)
    (some (fn [[a b c]]
            (let [line [(nth board a) (nth board b) (nth board c)]
                  player (first line)]
              (when (and player
                         (= line [player player player]))
                #{(inc a) (inc b) (inc c)})))
          winning-lines)))

(defn- render-row
  [board positions winning-cells]
  (str/join (color/dim-style "|") (map #(cell-display board % winning-cells) positions)))

(defn render-board
  ([board player]
   (render-board board player nil))
  ([board player winner]
   (when-not (game/board? board)
     (throw (ex-info "Invalid board" {:board board})))
   (let [winning-cells (when winner
                         (winning-positions board))
         turn-message (if winner
                        (str (color/bold-style (color/success-style (str (player->label winner) " wins!"))))
                        (str (player->label player) "'s turn"))]
     (str (color/bold-style title-line)
          "\n\n"
          turn-message
          "\n\n"
          (render-row board [1 2 3] winning-cells)
          "\n"
          row-separator
          "\n"
          (render-row board [4 5 6] winning-cells)
          "\n"
          row-separator
          "\n"
          (render-row board [7 8 9] winning-cells)))))

(defn parse-position
  [input]
  (let [trimmed-input (some-> input str/trim)]
    (when (and trimmed-input
               (re-matches #"[1-9]" trimmed-input))
      (Integer/parseInt trimmed-input))))

(defn validate-input
  [board input]
  (if-some [position (parse-position input)]
    (if (valid-move? board position)
      {:position position}
      {:error occupied-cell-message})
    {:error invalid-input-message}))

(defn game-result
  [board]
  (when-not (game/board? board)
    (throw (ex-info "Invalid board" {:board board})))
  (if-some [winning-player (winner board)]
    {:status :won :winner winning-player}
    (if (full? board)
      {:status :draw}
      {:status :playing})))

(defn final-message
  [board]
  (let [{:keys [status winner]} (game-result board)]
    (case status
      :won (color/success-style (str (player->label winner) " wins! Game over."))
      :draw (color/warning-style "It's a draw! Game over.")
      (throw (ex-info "Game is not over" {:board board :status status})))))

(defn parse-play-again-choice
  [input]
  (let [normalized-input (some-> input str/trim str/lower-case)]
    (case normalized-input
      "y" :yes
      "n" :no
      nil)))

(defn update-scores
  [scores {:keys [status winner]}]
  (case status
    :won (update scores winner inc)
    :draw (update scores :draws inc)
    scores))

(defn render-score
  [{:keys [x o draws]}]
  (str "Score (X : O : Draws) " x " : " o " : " draws))

(defn- prompt-input
  [player]
  (print (str (player->label player) "'s turn. Enter 1-9: "))
  (flush)
  (read-line))

(defn -main
  [& _args]
  (letfn [(resolve-turn [board player]
            (if-some [input (prompt-input player)]
              (let [{:keys [position error]} (validate-input board input)]
                (if error
                  (do
                    (println error)
                    (recur board player))
                  (let [updated-board (make-move board position player)
                        {:keys [status winner]} (game-result updated-board)]
                    (case status
                      :playing {:status :playing
                                :board updated-board
                                :next-player (next-player player)}
                      :won {:status :won
                            :board updated-board
                            :current-player player
                            :winner winner}
                      :draw {:status :draw
                             :board updated-board
                             :current-player player}))))
              {:status :eof}))
          (game-loop [board player]
            (println (render-board board player))
            (let [{:keys [status board next-player current-player winner]} (resolve-turn board player)]
              (case status
                :playing (recur board next-player)
                :won (do
                       (println (render-board board current-player winner))
                       (println (final-message board)))
                :draw (do
                        (println (render-board board current-player))
                        (println (final-message board)))
                :eof (println no-input-message))))]
    (game-loop (new-board) :x)))
