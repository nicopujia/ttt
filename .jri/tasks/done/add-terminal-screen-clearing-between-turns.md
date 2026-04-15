---
title: Add terminal screen clearing between turns
priority: 1
assignee: Ralph
depends_on: []
acceptance_criteria:
- Screen clears cleanly before every new board, welcome screen, and end-game screen
- Only the current game state is visible during play
- Uses pure ANSI escape codes (no extra dependencies)
- Graceful fallback (e.g. multiple newlines) if clearing is unsupported
- Works on Linux/macOS terminals
- All existing tests continue to pass
---

Implement reliable screen clearing before every board render using ANSI escape codes. This eliminates scrolling accumulation of previous boards. Include a fallback if ANSI fails.