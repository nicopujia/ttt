---
title: 'Pure game logic: board, moves, win/draw detection'
priority: 1
assignee: Ralph
depends_on:
- clojure-project-scaffolding-with-make-check
acceptance_criteria:
- Pure functions implemented correctly
- All win conditions covered
- Immutable vector board
- No side effects
- Comprehensive tests passing
---

Implement pure functions in src/ttt/core.clj (no I/O):
- new-board, valid-move?, make-move, winner, full?, game-over?
- Board = vector of 9 nils/:x/:o (index 0 = position 1)
- Winner checks all 8 combos (rows/cols/diags)
- Immutable updates only.
- Index conversion (1-9 -> 0-8).
These must be fully unit tested in core_test.clj.