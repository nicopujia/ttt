---
title: Add play-again loop with score tracking
priority: 2
assignee: Ralph
depends_on:
- add-colored-output-for-board-and-messages
acceptance_criteria:
- 'Score is tracked and displayed between games (X : O : Draws)'
- After win/draw, clear screen and show result + current score + highlighted winning
  line
- Prompt "Play again? (y/n)" with clear colored feedback
- Loop continues with new game (X always starts first move)
- Graceful exit when user declines
- No regression in core game logic
---

After a game ends, show final result with highlighted winning line, update score (X wins, O wins, draws), and ask if the player wants to play again. Keep playing until user declines. Display running score between games.