node_modules: package.json
	npm install
	touch node_modules

dev: node_modules
	npx shadow-cljs watch app

build: node_modules
	npx shadow-cljs release app

clean:
	rm -rf public/js .shadow-cljs .cpcache node_modules
