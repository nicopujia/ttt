---
title: include test execution in make check quality gate
priority: 0
assignee: Ralph
depends_on: []
acceptance_criteria:
- make check runs clj-kondo --lint src test
- make check also runs the tests via `clojure -M -m ttt.core-test` (or equivalent)
- The target fails the build if either step reports errors or test failures
- make check still succeeds cleanly on current codebase (no new failures introduced)
- No other changes to project behavior or structure
---

Update the Makefile so that `make check` runs both the existing clj-kondo linting (on src and test) **and** executes the full test suite. The command should fail (non-zero exit) if either linting finds issues or any test fails. This ensures the quality gate catches both static analysis problems and behavioral regressions.