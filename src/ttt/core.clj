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

(def welcome-title-sections
  [[color/x-style
    ["████████╗██╗ ██████╗"
     "╚══██╔══╝██║██╔════╝"
     "   ██║   ██║██║     "
     "   ██║   ██║██║     "
     "   ██║   ██║╚██████╗"
     "   ╚═╝   ╚═╝ ╚═════╝"]
   ]
   [color/o-style
    ["████████╗ █████╗  ██████╗"
     "╚══██╔══╝██╔══██╗██╔════╝"
     "   ██║   ███████║██║     "
     "   ██║   ██╔══██║██║     "
     "   ██║   ██║  ██║╚██████╗"
     "   ╚═╝   ╚═╝  ╚═╝ ╚═════╝"]
   ]
   [color/warning-style
    ["████████╗ ██████╗ ███████╗"
     "╚══██╔══╝██╔═══██╗██╔════╝"
     "   ██║   ██║   ██║█████╗  "
     "   ██║   ██║   ██║██╔══╝  "
     "   ██║   ╚██████╔╝███████╗"
     "   ╚═╝    ╚═════╝ ╚══════╝"]]])

(def row-separator (color/dim-style "---+---+---"))

(def no-cell-selected-message (color/x-style "Please enter a number from 1 to 9."))

(def out-of-range-message (color/x-style "That move isn't on the board. Choose a number from 1 to 9."))

(def invalid-input-message (color/x-style "Please enter a valid number from 1 to 9."))

(def occupied-cell-message (color/x-style "That cell is already taken. Choose one of the open numbers."))

(def no-input-message "No input received. Exiting the game.")

(def play-again-prompt (color/warning-style "Play another round? (y/n) "))

(def invalid-play-again-message (color/x-style "Please answer with y or n."))

(def goodbye-message (color/success-style "Thanks for playing Tic Tac Toe!"))

(def welcome-start-prompt (color/bold-style (color/success-style "Press Enter to begin.")))

(def clear-screen-sequence "\u001B[2J\u001B[H")

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

(defn- player-name
  [player]
  (str "Player " (player->label player)))

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

(defn- render-board-display
  [board header winning-cells]
  (str (color/bold-style title-line)
       "\n\n"
       header
       "\n\n"
       (render-row board [1 2 3] winning-cells)
       "\n"
       row-separator
       "\n"
       (render-row board [4 5 6] winning-cells)
       "\n"
       row-separator
       "\n"
       (render-row board [7 8 9] winning-cells)))

(defn- boxed-message
  [message style]
  (let [content (str "  " message "  ")
        width (count content)
        border (apply str (repeat width "═"))]
    (str (style (str "╔" border "╗"))
         "\n"
         (style (str "║" content "║"))
         "\n"
         (style (str "╚" border "╝")))))

(defn render-board
  ([board player]
   (render-board board player nil))
  ([board player winner]
   (when-not (game/board? board)
     (throw (ex-info "Invalid board" {:board board})))
   (let [winning-cells (when winner
                          (winning-positions board))
         header (if winner
                  (str (color/bold-style (color/success-style (str (player-name winner) " wins!")))
                       "\n"
                       (color/success-style "Three in a row seals the round."))
                  (str (color/bold-style (str (player-name player) " to move"))
                       "\n"
                       "Choose an open cell (1-9)."))]
     (render-board-display board header winning-cells))))

(defn parse-position
  [input]
  (let [trimmed-input (some-> input str/trim)]
    (when (and trimmed-input
               (re-matches #"[1-9]" trimmed-input))
      (Integer/parseInt trimmed-input))))

(defn validate-input
  [board input]
  (let [trimmed-input (some-> input str/trim)]
    (cond
      (or (nil? trimmed-input)
          (str/blank? trimmed-input))
      {:error no-cell-selected-message}

      (re-matches #"\d+" trimmed-input)
      (let [position (Integer/parseInt trimmed-input)]
        (cond
          (not (valid-position? position))
          {:error out-of-range-message}

          (valid-move? board position)
          {:position position}

          :else
          {:error occupied-cell-message}))

      :else
      {:error invalid-input-message})))

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
      :won (color/success-style (str (player-name winner) " wins the round!"))
      :draw (color/warning-style "The round ends in a draw.")
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
  (str "Scoreboard: X " x " | O " o " | Draws " draws))

(defn render-welcome-screen
  []
  (let [title-block (->> welcome-title-sections
                         (mapcat (fn [[style lines]]
                                   (concat (map #(color/bold-style (style %)) lines)
                                           [""])))
                         butlast
                         (str/join "\n"))
        instructions [(str (color/bold-style "Rules")
                           ": "
                           (color/x-style "X")
                           " goes first, then "
                           (color/o-style "O")
                           ".")
                      (str "Choose open cells with "
                           (color/dim-style "1-9")
                           " on the board.")
                      "Match three in a row to win."]]
    (str title-block
         "\n\n"
         (str/join "\n" instructions)
         "\n\n"
         welcome-start-prompt)))

(defn render-final-screen
  [board scores]
  (let [{:keys [status winner]} (game-result board)
        winning-cells (when winner
                        (winning-positions board))
        banner (case status
                 :won (boxed-message (str (player-name winner) " wins!") color/success-style)
                 :draw (boxed-message "Round ends in a draw" color/warning-style)
                 (throw (ex-info "Game is not over" {:board board :status status})))]
    (str (render-board-display board banner winning-cells)
         "\n\n"
         (final-message board)
         "\n"
         (render-score scores))))

(defn- prompt-input
  [player]
  (print (str (player-name player) " - choose an open cell (1-9): "))
  (flush)
  (read-line))

(defn- prompt-play-again
  []
  (loop []
    (print play-again-prompt)
    (flush)
    (if-some [input (read-line)]
      (if-some [choice (parse-play-again-choice input)]
        choice
        (do
          (println invalid-play-again-message)
          (recur)))
      :eof)))

(defn- display-screen
  [content]
  (print clear-screen-sequence)
  (println content))

(defn- show-welcome-screen
  []
  (display-screen (render-welcome-screen))
  (if (some? (read-line))
    :start
    :eof))

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
            (display-screen (render-board board player))
            (let [{:keys [status board next-player current-player winner]} (resolve-turn board player)]
              (case status
                :playing (recur board next-player)
                :won {:status :won
                      :board board
                      :current-player current-player
                      :winner winner}
                :draw {:status :draw
                       :board board
                       :current-player current-player}
                :eof {:status :eof})))
          (session-loop [scores]
            (let [{:keys [status board] :as result} (game-loop (new-board) :x)]
              (case status
                :won (let [updated-scores (update-scores scores result)]
                       (display-screen (render-final-screen board updated-scores))
                       (case (prompt-play-again)
                         :yes (recur updated-scores)
                         :no (println goodbye-message)
                         :eof (println no-input-message)))
                :draw (let [updated-scores (update-scores scores result)]
                        (display-screen (render-final-screen board updated-scores))
                        (case (prompt-play-again)
                          :yes (recur updated-scores)
                          :no (println goodbye-message)
                          :eof (println no-input-message)))
                :eof (println no-input-message))))]
    (case (show-welcome-screen)
      :start (session-loop initial-scores)
      :eof (println no-input-message))))
