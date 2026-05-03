---
title: Document tic-tac-toe CLI usage and rules
priority: 3
assignee: Ralph
depends_on:
- implement-complete-terminal-tic-tac-toe-gameplay
acceptance_criteria:
- The root README exists and is readable as plain Markdown.
- The README documents `make run` for playing the game.
- The README documents `make check` for running tests, linting, and formatting checks.
- The README explains standard tic-tac-toe rules for this app, including cells 1-9
  in reading order, X starts, alternating turns, wins, and draws.
- The README documents valid move input as exact strings 1 through 9 after trimming,
  with numeric-looking variants such as 01, +1, and 1.0 invalid.
- The README documents accepted play-again inputs y/yes/n/no, case-insensitively with
  surrounding whitespace ignored.
- The README states that the scoreboard is in-memory for the current session and displays
  X wins, O wins, and Draws in that order.
- The README does not promise unsupported features such as AI, persistence, networking,
  or configurable themes.
---

Write a minimal root README for the completed project.

The README should document project-wide usage and expectations only, not implementation internals.

Required content:
- What the project is: a terminal human-vs-human tic-tac-toe game written in Clojure.
- How to run it with `make run`.
- How to run the full quality gate with `make check`.
- How to play: cells 1-9 in reading order, X starts, players alternate, standard win/draw rules.
- Valid move input is, after trimming surrounding whitespace, exactly one of `1` through `9`; examples like `01`, `+1`, and `1.0` are invalid.
- Play-again accepted answers: `y`, `yes`, `n`, `no`, case-insensitively, with surrounding whitespace ignored.
- Mention that the scoreboard is in-memory for the current program session and displays `X wins`, `O wins`, and `Draws` in that order.