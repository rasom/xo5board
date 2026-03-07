node_modules: package.json
	npm install
	touch node_modules

dev: node_modules
	npx shadow-cljs watch app

build: node_modules
	npx shadow-cljs release app

release: build
	@if ! git rev-parse --verify release >/dev/null 2>&1; then \
		git checkout --orphan release && git reset; \
	else \
		git checkout release; \
	fi
	cp public/index.html .
	mkdir -p js
	cp public/js/main.js js/
	git add index.html js/main.js
	git commit -m "Release build" --allow-empty || true
	git checkout -f master

clean:
	rm -rf public/js .shadow-cljs .cpcache node_modules
