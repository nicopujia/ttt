---
title: Add automated coverage for terminal tic-tac-toe behavior
priority: 2
assignee: Ralph
depends_on:
- implement-complete-terminal-tic-tac-toe-gameplay
acceptance_criteria:
- Automated Clojure tests cover normal gameplay, wins, draws, invalid moves including
  numeric-looking variants, play-again behavior, exact scoreboard fields/order/count
  updates/persistence, ANSI clearing, ANSI-colored marks, input trimming, and EOF
  edge cases described in the task body.
- Tests prove X and O marks themselves include ANSI color escape sequences without
  requiring a specific color choice.
- Tests prove clear/refresh ANSI clear codes are emitted even when output is captured
  or redirected.
- Tests avoid brittle assertions on exact prose, exact board art, or exact color choices
  except where checking required behavior.
- '`make check` runs the full automated test suite along with clj-kondo and cljfmt
  checks.'
- '`make check` succeeds after the tests are added.'
---

Add automated tests covering the terminal tic-tac-toe behavior implemented by the gameplay task. Tests should assert durable behavior while avoiding brittle dependence on exact prose, exact ANSI colors, or exact board art unless that detail is part of the requirement.

Required test coverage:
- Startup welcome proceeds immediately to first board/move prompt.
- Board has 1-9 reading-order numbered empty cells before moves.
- X and O alternate on valid moves; X starts each round.
- Move input trims surrounding whitespace.
- After trimming, only exact strings `1` through `9` are syntactically valid move inputs; numeric-looking variants such as `01`, `+1`, and `1.0` are rejected.
- Invalid move input keeps the same player and refreshes with current board, validation message, and same-player prompt.
- Occupied cells and out-of-range/non-numeric inputs are rejected.
- Representative row, column, and diagonal wins are detected.
- Draw is detected when the board fills without a winner.
- Final win/draw presentation includes final board, outcome summary, and scoreboard.
- Scoreboard displays exactly `X wins`, `O wins`, and `Draws` in that order; counts start at 0, update after completed rounds, and persist across multiple rounds in one process.
- Play-again accepts y/yes/n/no case-insensitively with surrounding whitespace ignored.
- Invalid play-again retry refresh includes final board, outcome summary, scoreboard, validation message, and play-again prompt.
- ANSI clear codes are emitted even under redirected/non-interactive output.
- X and O marks themselves are ANSI-colorized.
- EOF during move or play-again exits gracefully with status 0 and a short message when there is no meaningful input.
- Non-whitespace partial input at EOF is submitted, including valid partial move input that wins/draws or advances to the next turn before graceful exit, valid partial play-again `y`/`yes` and `n`/`no` behavior, and the specified invalid-partial-input-then-immediate-EOF retry behavior.
- Whitespace-only partial EOF exits gracefully as no meaningful input.

Testing approach:
- Prefer tests around pure game logic and a testable CLI/session boundary where practical.
- Avoid asserting exact welcome text, exact validation prose, exact color codes, or exact board ASCII art unless the assertion only checks the required semantic signal.