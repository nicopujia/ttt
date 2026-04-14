(ns ttt.core
  (:require [clojure.string :as str]
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

(def row-separator "---+---+---")

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
  [board position]
  (case (nth board (position->index position))
    :x " X "
    :o " O "
    (format " %d " position)))

(defn- render-row
  [board positions]
  (str/join "|" (map #(cell-display board %) positions)))

(defn render-board
  [board player]
  (when-not (game/board? board)
    (throw (ex-info "Invalid board" {:board board})))
  (str title-line
       "\n\n"
       (player->label player)
       "'s turn\n\n"
       (render-row board [1 2 3])
       "\n"
       row-separator
       "\n"
       (render-row board [4 5 6])
       "\n"
       row-separator
       "\n"
       (render-row board [7 8 9])))

(defn startup-message
  []
  (str "ttt scaffold ready with " (count (new-board)) " cells."))

(defn -main
  [& _args]
  (println (startup-message)))
