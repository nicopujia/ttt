.PHONY: check

check:
	@clj-kondo --lint src test
