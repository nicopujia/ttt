# ttt - Ultra Simple Tic Tac Toe

A command-line Tic Tac Toe game for two human players (hotseat).

**Features**
- X goes first, then O.
- Pure functional game logic (board is a 9-element vector).
- ASCII board display with numbers for empty cells.
- Input validation with re-prompt on errors.
- Detects wins (8 lines) and draws.
- Play-again loop with running score tracking.

## Usage

```bash
make check
clojure -M:run -m ttt.core
```

Enter numbers 1-9 when prompted, then choose whether to play again.

See `.jri/tasks/todo/` for detailed implementation tasks and acceptance criteria. All core logic is pure functions; I/O is isolated to `-main`.

This is a learning project to practice Clojure.
