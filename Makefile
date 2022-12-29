.PHONY: build
build: clean
	./gradlew build

.PHONY: clean
clean:
	./gradlew clean

.PHONY: cfg
cfg: build
	./gradlew run --args="cfg"

.PHONY: docker/build
docker/build:
	docker build -t gradle_test .

.PHONY: docker/run
docker/run:
	docker run --rm -it gradle_test make cfg

.PHONY: docker/cfg
docker/cfg: docker/build
	docker cp `docker create gradle_test`:/app/output/ .
