# Terminal Tic-Tac-Toe

A terminal human-vs-human tic-tac-toe game written in Clojure.

## Usage

- Play the game: `make run`
- Run the full quality gate: `make check` (tests, linting, and formatting checks)

## How to play

- The board uses cells `1` through `9` in reading order, left to right and top to bottom.
- `X` starts every round.
- Players alternate turns after valid moves.
- A player wins by placing three marks in a row, column, or diagonal.
- If all nine cells are filled without a winner, the round is a draw.

## Input rules

- After trimming surrounding whitespace, a move must be exactly one of `1` through `9`.
- Numeric-looking variants such as `01`, `+1`, and `1.0` are invalid.
- Play-again answers accept `y`, `yes`, `n`, and `no`, case-insensitively, with surrounding whitespace ignored.

## Scoreboard

The scoreboard is in-memory for the current program session only. It displays `X wins`, `O wins`, and `Draws` in that order.

## Scope

This game is human-vs-human only. It does not provide AI, persistence, networking, or configurable themes.
