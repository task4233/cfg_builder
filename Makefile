.PHONY: build
build: clean
	./gradlew build

.PHONY: clean
clean:
	./gradlew clean

.PHONY: cfg
cfg:
	./gradlew run --args="cfg"
