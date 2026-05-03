---
title: Establish Clojure CLI project quality entrypoint
priority: 0
assignee: Ralph
depends_on: []
acceptance_criteria:
- A root deps.edn exists and defines a Clojure CLI project suitable for source and
  test code.
- The root Makefile has a `run` target that invokes the app through Clojure CLI.
- The root Makefile has a `check` target that runs, in some order, automated Clojure
  tests, clj-kondo linting, and cljfmt formatting check.
- Running `make check` in the repository succeeds after this setup task is complete.
- The previous placeholder `check` behavior that prints an unconfigured message and
  fails is gone.
- No nonessential application features, external services, UI frameworks, or persistence
  layers are introduced by this setup task.
---

Set up the empty repository as a Clojure CLI/deps.edn project and replace the existing stub Makefile with durable developer entrypoints. This is the greenfield setup task and must establish the quality gate Ralph and future contributors use before and after feature work.

Scope:
- Use Clojure CLI with a root deps.edn.
- Provide a Makefile where `make run` starts the terminal tic-tac-toe game and `make check` runs every configured quality gate.
- `make check` must run Clojure tests, clj-kondo linting, and cljfmt formatting check.
- Configure whatever minimal source/test layout, aliases, lint config, and formatting setup are needed for those commands to work in a fresh checkout.
- Replace the current stub Makefile; do not preserve its failing placeholder behavior.
- Keep setup minimal and conventional for a small CLI app; do not add unrelated frameworks or services.

Behavioral constraints:
- This task does not need to implement the tic-tac-toe game beyond whatever harmless placeholder is required for `make run` and `make check` to execute cleanly before later tasks.
- The final project after all dependent tasks must still use these same entrypoints.