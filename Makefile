# One entry point for every common task.
MVN := mvn -B -ntp
NETWORK := $(notdir $(CURDIR))_default

.PHONY: help build test up down logs smoke perf perf-contention docs

help:            ## List targets
	@grep -E '^[a-z-]+:.*##' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  %-16s %s\n", $$1, $$2}'

build:           ## Install all Java modules locally (needed before running a single service)
	$(MVN) install -DskipTests

test:            ## All backend tests + coverage (target/site/jacoco per module)
	$(MVN) verify
	@if [ -d frontend ]; then cd frontend && npm test; fi

up:              ## Full stack in Docker -> UI :3000, API :8080
	docker compose up --build -d
	@echo "Waiting for the gateway..." && until curl -sf localhost:8080/actuator/health >/dev/null; do sleep 3; done && echo "Ready"

down:            ## Stop the stack (keeps data)
	docker compose down

logs:            ## Follow logs of all services
	docker compose logs -f --tail=50

smoke:           ## End-to-end API check against the running stack
	./scripts/smoke-test.sh

perf:            ## k6 load test: availability search (rate limit off for the run, back on after)
	RATE_LIMIT_ENABLED=false docker compose up -d --wait gateway-service
	docker run --rm --network $(NETWORK) -v $(CURDIR)/perf:/perf grafana/k6 run \
	  -e BASE_URL=http://gateway-service:8080 --summary-export=/perf/results/availability.json /perf/k6/availability.js; \
	status=$$?; docker compose up -d --wait gateway-service; exit $$status

perf-contention: ## k6: 500 parallel bookings racing for 10 slots -> expect exactly 10 created
	RATE_LIMIT_ENABLED=false docker compose up -d --wait gateway-service
	docker run --rm --network $(NETWORK) -v $(CURDIR)/perf:/perf grafana/k6 run \
	  -e BASE_URL=http://gateway-service:8080 --summary-export=/perf/results/contention.json /perf/k6/booking-contention.js; \
	status=$$?; docker compose up -d --wait gateway-service; exit $$status

docs:            ## Serve the docs site -> http://localhost:8000
	pip install --user -r docs/requirements.txt && mkdocs serve
