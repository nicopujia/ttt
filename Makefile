.PHONY: check

check:
	@clj-kondo --lint src test
	@clojure -M -m ttt.core-test
