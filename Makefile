.PHONY: build
build: clean
	./gradlew build 

.PHONY: clean
clean:
	./gradlew clean 

.PHONY: cfg
cfg: build
	./gradlew run --args="cfg 0"
	./gradlew run --args="cfg 1" 
	./gradlew run --args="cfg 2" 
	./gradlew run --args="cfg 3" 
	./gradlew run --args="cfg 4" 
	./gradlew run --args="cfg 5" 
	./gradlew run --args="cfg 6" 
	./gradlew run --args="cfg 7" 
	./gradlew run --args="cfg 8" 
	./gradlew run --args="cfg 9" 

.PHONY: docker/build
docker/build:
	docker build -t gradle_test .

.PHONY: docker/run
docker/run:
	docker run --rm -it gradle_test make cfg

.PHONY: docker/cfg
docker/cfg: docker/build
	docker cp `docker create gradle_test`:/app/output/ .
