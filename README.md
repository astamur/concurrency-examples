# Java Concurrency examples

Personal examples and investigation code on Java concurrency: hand-rolled primitives
(a blocking queue, a bounded set, an atomic double, a tiny executor) and classic
coordination problems (producer/consumer, ping-pong, latches).

## Requirements

- **JDK 25** (see [`.sdkmanrc`](.sdkmanrc) — `sdk env` picks it up automatically)
- Gradle is provided via the wrapper; no local install needed.

## Build & test

```bash
./gradlew build      # compile + run the test suite
./gradlew test       # tests only
```

Dependency versions are centralized in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Running an example

Each example under `src/main/java/.../primitives` and `.../primitives/other` has its own
`main` method. Run one from your IDE, or from the command line after `./gradlew build`, e.g.:

```bash
java -cp build/classes/java/main dev.astamur.concurrency.primitives.other.PingPongWithQueue
```

(Examples that use logging also need SLF4J/Logback on the classpath; running from the IDE
is the simplest way to get the full runtime classpath.)

## Layout

- `primitives/structure` — reusable building blocks: `SimpleBlockingQueue`,
  `SimpleBlockingQueueWithLock`, `BoundedHashSet`, `AtomicDouble`, `SimpleExecutor`
- `primitives/other` — runnable demos: producer/consumer, ping-pong, `CountDownLatch`
- `src/test` — JUnit 5 tests for the reusable structures
