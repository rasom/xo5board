node_modules: package.json
	npm install
	touch node_modules

dev: node_modules
	npx shadow-cljs watch app

build: node_modules
	npx shadow-cljs release app

release: build
	mkdir -p /tmp/gomoku-release
	cp public/index.html /tmp/gomoku-release/
	cp public/js/main.js /tmp/gomoku-release/
	git stash --include-untracked || true
	@if ! git rev-parse --verify release >/dev/null 2>&1; then \
		git checkout --orphan release && git reset; \
	else \
		git checkout release; \
	fi
	cp /tmp/gomoku-release/index.html .
	mkdir -p js
	cp /tmp/gomoku-release/main.js js/
	rm -rf /tmp/gomoku-release
	git add index.html js/main.js
	git commit -m "Release build" --allow-empty || true
	git checkout -f master
	git stash pop || true

clean:
	rm -rf public/js .shadow-cljs .cpcache node_modules
