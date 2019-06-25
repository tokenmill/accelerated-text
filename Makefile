PREACT_MAKE= cd perform && make
PROJECT_NAME=accelerated-text
PYTEST_DOCKER="registry.gitlab.com/tokenmill/nlg/${PROJECT_NAME}/pytest:latest"

-include .env
export

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

build-dynamodb-docker:
	docker pull amazon/dynamodb-local
	docker run -d -p 8000:8000 --name dynamo-build amazon/dynamodb-local -jar DynamoDBLocal.jar -sharedDb
	aws dynamodb create-table --table-name lexicon --attribute-definitions AttributeName=key,AttributeType=S --key-schema AttributeName=key,KeyType=HASH --billing-mode=PAY_PER_REQUEST  --endpoint-url http://localhost:8000
	aws dynamodb create-table --table-name data --attribute-definitions AttributeName=key,AttributeType=S --key-schema AttributeName=key,KeyType=HASH --billing-mode=PAY_PER_REQUEST  --endpoint-url http://localhost:8000
	aws dynamodb create-table --table-name blockly-workspace --attribute-definitions AttributeName=id,AttributeType=S --key-schema AttributeName=id,KeyType=HASH --billing-mode=PAY_PER_REQUEST  --endpoint-url http://localhost:8000
	aws dynamodb create-table --table-name nlg-results --attribute-definitions AttributeName=key,AttributeType=S --key-schema AttributeName=key,KeyType=HASH --billing-mode=PAY_PER_REQUEST  --endpoint-url http://localhost:8000
	aws dynamodb create-table --table-name dictionary-combined --attribute-definitions AttributeName=key,AttributeType=S --key-schema AttributeName=key,KeyType=HASH --billing-mode=PAY_PER_REQUEST  --endpoint-url http://localhost:8000
	aws dynamodb create-table --table-name reader-flag --attribute-definitions AttributeName=key,AttributeType=S --key-schema AttributeName=key,KeyType=HASH --billing-mode=PAY_PER_REQUEST  --endpoint-url http://localhost:8000
	aws dynamodb put-item --table-name lexicon --item '{"key": {"S": "good.1"}, "word": {"S": "good"}, "synonyms": {"L": [{"S":"good"}, {"S": "amazing"}, {"S": "superb"}, {"S": "peachy"}]}, "createdAt": {"N": "1558959895059"},"updatedAt": {"N": "1558959895059"}}' --endpoint-url http://localhost:8000
	aws dynamodb put-item --table-name lexicon --item '{"key": {"S": "good.2"}, "word": {"S": "good"}, "synonyms": {"L": [{"S":"good"}, {"S": "complete"}]}, "createdAt": {"N": "1558959895059"},"updatedAt": {"N": "1558959895059"}}' --endpoint-url http://localhost:8000
	docker commit dynamo-build registry.gitlab.com/tokenmill/nlg/accelerated-text/dynamodb-local:latest
	docker stop dynamo-build
	docker rm dynamo-build

build-s3-docker:
	docker pull scality/s3server
	docker run -d -p 8000:8000 --name s3-build -e SCALITY_ACCESS_KEY_ID=${NLG_AWS_ACCESS_KEY_ID} -e SCALITY_SECRET_ACCESS_KEY=${NLG_AWS_SECRET_ACCESS_KEY} -e S3BACKEND="mem" scality/s3server
	aws s3 mb s3://accelerated-text-data-files/example-user/ --endpoint-url http://localhost:8000 
	aws s3 cp nlg-api/resources/data-example.csv s3://accelerated-text-data-files/example-user/ --endpoint-url http://localhost:8000 
	docker commit s3-build registry.gitlab.com/tokenmill/nlg/accelerated-text/s3-local:latest
	docker stop s3-build
	docker rm s3-build

publish-s3-docker:
	docker push registry.gitlab.com/tokenmill/nlg/accelerated-text/s3-local:latest

publish-dynamodb-docker:
	docker push registry.gitlab.com/tokenmill/nlg/accelerated-text/dynamodb-local:latest

run-dev-env:
	docker-compose -p dev -f docker-compose.yml down && \
	docker-compose -p dev -f docker-compose.yml build && \
	docker-compose -p dev -f docker-compose.yml up --remove-orphans
