.PHONY: run test lint format-check check

run:
	clojure -M:run

test:
	clojure -M:test

lint:
	clj-kondo --lint src test

format-check:
	clojure -M:fmt check src test

check: test lint format-check
