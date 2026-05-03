---
title: Implement complete terminal tic-tac-toe gameplay
priority: 1
assignee: Ralph
depends_on:
- establish-clojure-cli-project-quality-entrypoint
acceptance_criteria:
- '`make run` starts a playable terminal tic-tac-toe game.'
- At startup, the game displays an informational welcome and then proceeds directly
  to the first board and move prompt without waiting for separate welcome-screen input.
- The board displays empty cells numbered 1 through 9 in reading order and displays
  placed X/O marks instead of those cell numbers.
- X starts every new round, players alternate turns after valid moves only, and invalid
  moves keep the same player to move.
- The game detects all standard row, column, and diagonal wins for X and O.
- The game detects a draw when all nine cells are filled without a winner.
- After a win or draw, the game displays the final board, a clear win/draw summary,
  and the in-memory session scoreboard.
- The scoreboard persists across play-again rounds in the same process and resets
  only when the program starts again.
- The play-again loop accepts y/yes/n/no case-insensitively with surrounding whitespace
  ignored.
- Invalid move retry refreshes show the current board, a validation message, and the
  same-player prompt.
- Invalid play-again retry refreshes show the final board, winner/draw summary, scoreboard,
  a validation message, and the play-again prompt.
- ANSI clear codes are emitted for refreshes even when output is redirected.
- X and O marks themselves are rendered with ANSI color escape sequences.
- EOF handling during move and play-again input exits gracefully with status 0 and
  a short message when there is no meaningful input.
- Non-whitespace partial input at EOF is submitted; if it is invalid move input, the
  normal invalid-move refreshed retry screen is shown before the next immediate EOF
  causes graceful status-0 exit.
- Whitespace-only partial input at EOF is treated as no meaningful input and exits
  gracefully with status 0.
- The implementation remains human-vs-human only with no AI, persistence, networking,
  or external service dependency.
---

Implement the human-vs-human terminal tic-tac-toe game using the project entrypoints established by the setup task.

Game scope:
- Human-vs-human only; no AI, network play, saved games, or persistence.
- Use a single in-memory session scoreboard across rounds within one process.
- Standard 3x3 tic-tac-toe rules: X and O alternate turns; X starts each round; first player with three marks in a row, column, or diagonal wins; if all nine cells are filled without a winner, the round is a draw.
- Cells are numbered 1-9 in reading order, left-to-right and top-to-bottom.

Terminal/UI behavior:
- Show an informational welcome screen at program start, then immediately proceed to the first board and move prompt without requiring a separate keypress.
- Render the board with numbered empty cells and colored X/O marks. Exact colors, wording, and board art are up to Ralph, but X and O marks themselves must be ANSI-colorized.
- Clear/refresh the screen between turns using ANSI clear codes. Always emit the ANSI clear codes, even when stdout is redirected or non-interactive.
- Prompt the current player for a move, validate input, and keep the same player's turn on invalid moves.
- Invalid move retry refresh must show the current board, a validation message, and the same-player prompt.
- After a win or draw, show a clear final presentation containing the final board, the winner or draw summary, and the current session scoreboard.
- Prompt whether to play again after each completed round.
- Play-again accepts `y`, `yes`, `n`, and `no`, case-insensitively, with surrounding whitespace ignored.
- On `y` or `yes`, begin a new round with an empty board, X to move first, and the existing in-memory scoreboard retained.
- On `n` or `no`, exit gracefully with status 0 after a short goodbye/exit message.
- Invalid play-again retry refresh must show the final board, winner/draw summary, scoreboard, a validation message, and the play-again prompt.

Input and EOF behavior:
- For move input, trim surrounding whitespace before validation.
- Valid moves are unoccupied cells 1 through 9 only.
- Non-whitespace partial input at EOF is submitted as if the line had been entered.
- Whitespace-only partial input followed by EOF exits gracefully as no meaningful input, status 0, with a short message.
- EOF during move input with no meaningful input exits gracefully with status 0 and a short message.
- EOF during play-again input with no meaningful input exits gracefully with status 0 and a short message.
- If invalid non-whitespace partial EOF is submitted during move input, show the normal invalid-input refreshed retry screen, then exit gracefully with status 0 on the immediate EOF retry.
- Apply equivalent graceful behavior for invalid play-again input followed by immediate EOF retry: show the invalid play-again refreshed retry screen, then exit gracefully with status 0 on the immediate EOF retry.

Implementation choices:
- Sensible implementation choices are approved where not otherwise specified.
- Do not make tests depend on exact prose, colors, or board art unless the behavior itself requires it.