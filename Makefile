FRONT_END_MAKE= cd front-end && make
PROJECT_NAME=accelerated-text
DEMO_TEST_ENV_TARGET=registry.gitlab.com/tokenmill/nlg/${PROJECT_NAME}/demo-test-env:latest

-include .env
export

.PHONY: test
test:
	${FRONT_END_MAKE} test

.PHONY: run
run:
	${FRONT_END_MAKE} run

.PHONY: build-app
build-app:
	${FRONT_END_MAKE} build

.PHONY: deploy-app
deploy-app:
	${FRONT_END_MAKE} deploy

.PHONY: clean
clean:
	${FRONT_END_MAKE} clean

.PHONY: npm-audit
npm-audit:
	${FRONT_END_MAKE} npm-audit

run-dev-env:
	docker-compose -p dev -f docker-compose.yml -f docker-compose.front-end.yml down && \
	docker-compose -p dev -f docker-compose.yml -f docker-compose.front-end.yml build && \
	docker-compose -p dev -f docker-compose.yml -f docker-compose.front-end.yml up --remove-orphans

run-dev-env-no-api:
	docker-compose -p dev -f docker-compose.yml -f docker-compose.front-end.yml down && \
	docker-compose -p dev -f docker-compose.yml -f docker-compose.front-end.yml build && \
	docker-compose -p dev -f docker-compose.yml -f docker-compose.front-end.yml up --remove-orphans localstack mock-shop front-end

.PHONY: run-front-end-dev-deps
run-front-end-dev-deps:
	docker-compose -p dev -f docker-compose.yml down && \
	docker-compose -p dev -f docker-compose.yml build && \
	docker-compose -p dev -f docker-compose.yml up --remove-orphans

.PHONY: run-front-end-dev-deps-no-api
run-front-end-dev-deps-no-api:
	docker-compose -p dev -f docker-compose.yml down && \
	docker-compose -p dev -f docker-compose.yml build && \
	docker-compose -p dev -f docker-compose.yml up --remove-orphans localstack mock-shop

.PHONY: run-front-end-dev
run-front-end-dev:
	cd front-end && \
      	ACC_TEXT_API_URL=http://0.0.0.0:3001 \
      	ACC_TEXT_GRAPHQL_URL=http://0.0.0.0:3001/_graphql \
      	MOCK_SHOP_API_URL=http://0:0:0:0:8090 \
		make run
