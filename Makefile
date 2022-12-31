.PHONY: build
build: clean
	./gradlew build

.PHONY: clean
clean:
	./gradlew clean

.PHONY: cfg
cfg: build
	./gradlew run --args="cfg 0" > logs/cfg_0.txt
	./gradlew run --args="cfg 1" > logs/cfg_1.txt
	./gradlew run --args="cfg 2" > logs/cfg_2.txt
	./gradlew run --args="cfg 3" > logs/cfg_3.txt
	./gradlew run --args="cfg 4" > logs/cfg_4.txt
	./gradlew run --args="cfg 5" > logs/cfg_5.txt

.PHONY: docker/build
docker/build:
	docker build -t gradle_test .

.PHONY: docker/run
docker/run:
	docker run --rm -it gradle_test make cfg

.PHONY: docker/cfg
docker/cfg: docker/build
	docker cp `docker create gradle_test`:/app/output/ .
