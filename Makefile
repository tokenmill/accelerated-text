PREACT_MAKE= cd perform && make
PROJECT_NAME=accelerated-text
PYTEST_DOCKER="registry.gitlab.com/tokenmill/nlg/${PROJECT_NAME}/pytest:latest"

.PHONY: test
test:
	${PREACT_MAKE} test

.PHONY: run
run:
	${PREACT_MAKE} run

.PHONY: build-app
build-app:
	${PREACT_MAKE} build

.PHONY: deploy-app
deploy-app:
	${PREACT_MAKE} deploy

.PHONY: clean
clean:
	${PREACT_MAKE} clean

.PHONY: npm-audit
npm-audit:
	${PREACT_MAKE} npm-audit

docker-repo-login:
	docker login registry.gitlab.com

build-demo-test-env:
	(cd dockerfiles && docker build -f Dockerfile.test-env -t registry.gitlab.com/tokenmill/nlg/${PROJECT_NAME}/demo-test-env:latest .)

publish-demo-test-env: build-demo-test-env
	docker push registry.gitlab.com/tokenmill/nlg/${PROJECT_NAME}/demo-test-env:latest


build-pytest-docker:
	(cd dockerfiles && docker build -f Dockerfile.pytest -t ${PYTEST_DOCKER} .)

publish-pytest-docker: build-pytest-docker
	docker push ${PYTEST_DOCKER}

build-legacy-rest-api-image:
	docker pull ardoq/leiningen:3.8-8u172-2.7.1
	docker build -f Dockerfile.legacy-api -t registry.gitlab.com/tokenmill/nlg/accelerated-text/nlg-rest-api:latest nlg-api/
