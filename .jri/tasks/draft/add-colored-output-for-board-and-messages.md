---
title: Add colored output for board and messages
priority: 1
assignee: Ralph
depends_on:
- add-terminal-screen-clearing-between-turns
acceptance_criteria:
- X appears in bright red, O in bright blue
- Winning combination cells have background highlight (gold/yellow)
- Board grid and numbers use subtle/dimmed colors
- All feedback messages (win, draw, invalid move, turn) use appropriate colors
- Colors are defined in a central place for easy modification
- Output remains readable if colors are stripped
- Colors are always enabled (no --no-color or NO_COLOR support for now)
---

Add ANSI color support: X in bright red, O in bright blue, winning line highlighted with background color. Use subtle colors for grid. Support colored messages for wins, draws, errors, and turn indicators.