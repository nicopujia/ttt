---
title: Clojure project scaffolding with make check
priority: 0
assignee: Ralph
depends_on: []
acceptance_criteria:
- deps.edn created and functional
- src/ttt/core.clj with ns ttt.core and -main
- .clj-kondo/config.edn present
- Makefile `check` target runs linter successfully
- make check passes cleanly
- Project runnable via Clojure CLI
- Pure functions enforced by structure
---

Establish the base Clojure CLI project structure as per spec:
- Create deps.edn with :aliases for running the main and any dev tools.
- Ensure src/ttt/core.clj exists with proper namespace and (-main [& args]) entry point (can be stub for now).
- Configure .clj-kondo/config.edn for linting Clojure code.
- Update Makefile so `make check` runs `clj-kondo --lint src test` (or equivalent) and exits with proper status.
- The setup must allow running the project via `clojure -M:run -m ttt.core`.
- Strictly separate pure game logic functions (no println, no I/O) from any I/O in -main and helpers. Board representation: vector of 9 elements (nil/:x/:o).
- This task is priority 0; everything else depends on it succeeding.