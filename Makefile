.PHONY: build
build: clean
	./gradlew build

.PHONY: clean
clean:
	./gradlew clean

.PHONY: cfg
cfg: build
	./gradlew run --args="cfg"
