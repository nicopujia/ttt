---
title: CLI game loop, rendering, and input handling
priority: 2
assignee: Ralph
depends_on:
- pure-game-logic-board-moves-win-draw-detection
acceptance_criteria:
- Correct ASCII board rendering with header and centered symbols
- Functional recursive game loop with proper turn alternation
- Robust input validation with descriptive errors and re-prompt
- Exact win/draw end conditions and messages
- No mutable state in core logic
- All tests pass and make check succeeds
- Matches the ultra-simple spec completely
---

Complete the user-facing part in core.clj:
- Pure render-board -> string (Tic Tac Toe header, turn info, centered X/O or padded numbers, exact ASCII separators).
- Recursive loop in -main for game flow (display, prompt "X's turn. Enter 1-9: ", parse, validate, error re-prompt same player, update, check win/draw).
- On end: print final board + "X wins! Game over." or "It's a draw! Game over."
- Keep all logic pure; I/O minimal in main.
- Add tests for render-board and any other pure helpers.
- Ensure full game works end-to-end and make check passes.