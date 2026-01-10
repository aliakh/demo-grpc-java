# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Prerequisites

- **Java 17** — the Gradle toolchain is pinned to Java 17 (`build.gradle`); earlier JDKs will be rejected at build time.

## Build Commands

`shadowJar` runs the full pipeline in one step (proto generation → compilation → packaging). Use the individual tasks only when you need an incremental rebuild.

```bash
# Full build: generate stubs, compile, and produce the über JAR
./gradlew shadowJar

# Generate Java stubs from echo.proto only (incremental)
./gradlew generateProto

# Compile without packaging (incremental)
./gradlew build
```

Generated sources land in `build/generated/source/proto/main/java/`.

## Running Examples

Start a server and client pair from separate terminals. Most clients connect immediately and will fail if the server is not already running; start the server first.

```bash
# Example: unary method type
java -cp build/libs/demo-grpc-java-*.jar com.example.grpc.methodtype.unary.UnaryServer
java -cp build/libs/demo-grpc-java-*.jar com.example.grpc.methodtype.unary.UnaryBlockingClient

# Stop a server with Ctrl+C (triggers graceful shutdown via JVM shutdown hook)
```

All servers listen on **port 50051** by default (via `Servers.java`).

All channels use **plaintext** (no TLS) — suitable for local development only.

Pressing Ctrl+C sends `SIGINT` to the server process. The JVM shutdown hook calls `server.shutdown()` and waits up to 10 seconds for in-flight calls to complete before forcing termination. Shutdown progress is printed to **stderr** because the JUL logging framework may already have been reset by its own shutdown hook at that point.

## Architecture

**Single proto contract** — `src/main/proto/echo.proto` defines all four gRPC communication patterns (`UnaryEcho`, `ServerStreamingEcho`, `ClientStreamingEcho`, `BidirectionalStreamingEcho`) for the `EchoService`. This one file is shared across all examples.

**Two dimensions of examples:**

1. `src/main/java/com/example/grpc/methodtype/` — demonstrates the four RPC patterns, each with multiple client variants:
   - `EchoServiceBlockingStub` → `*BlockingClient` (blocks the calling thread; unary + server-streaming only)
   - `EchoServiceBlockingV2Stub` → `*BlockingV2Client` (throws `StatusException` instead of `StatusRuntimeException`; all patterns as experimental)
   - `EchoServiceStub` → `*AsynchronousClient` (non-blocking via `StreamObserver`; all patterns)
   - `EchoServiceFutureStub` → `*FutureClient` (unary only)

2. `src/main/java/com/example/grpc/<feature>/` — each subdirectory demonstrates one gRPC feature:
   - **cancellation** — shows how a client signals to the server that it no longer needs the result of an ongoing call, and how the server detects and honours that signal. Any abandonment of an in-flight RPC — explicit cancel, expired deadline, or I/O failure — counts as cancellation; the service is not told which reason triggered it
   - **deadline** — shows how to set a time limit on a call so the client stops waiting after a fixed duration. If the server does not respond in time, the call fails with `DEADLINE_EXCEEDED`, freeing both sides from doing further work on a result no one will use
   - **errorhandling** — three self-contained programs showing how gRPC propagates errors from server to client: as a plain status code, as a rich typed `google.rpc.Status` with structured details, and as trailing metadata. Each program embeds its own server so no separate process is needed
   - **headers** — shows how to attach custom key-value metadata (authentication tokens, tracing IDs, rate-limiting hints) to calls without touching the service method, using client and server interceptors that operate transparently on every request
   - **healthservice** — shows how to expose the standard `grpc.health.v1` health-check protocol alongside a business service so that load balancers and orchestrators can probe liveness and readiness. Demonstrates dynamic SERVING ↔ NOT_SERVING transitions driven by service conditions
   - **keepalive** — shows how HTTP/2 PING frames keep an otherwise-idle connection alive across firewalls and proxies that silently drop quiet connections. Without keepalive, a long-lived channel with no traffic may be closed undetected; periodic pings catch this before the next real call fails
   - **loadbalance** — shows how gRPC distributes calls across multiple server instances entirely on the client side. Compares `pick_first` (always route to the first reachable server) with `round_robin` (spread load evenly), using a custom name resolver to supply the server list
   - **manualflowcontrol** — shows how to take explicit control of backpressure in a bidirectional streaming call. Instead of automatically pulling the next message after each `onNext`, the client requests one message at a time, preventing a fast sender from outrunning a slow consumer
   - **nameresolve** — shows how gRPC resolves a logical service name into concrete server addresses, and how to plug in a custom name resolver for non-DNS registries. Contrasts the built-in DNS resolver with a custom `my-scheme` resolver side-by-side in a single client run
   - **retrying** — shows how a client retry policy with exponential backoff transparently recovers from transient `UNAVAILABLE` failures without any retry logic in application code. The server intentionally rejects a fraction of requests so the retries are clearly observable; an env-var toggle lets you compare behaviour with and without the policy
   - **waitforready** — shows how a client can queue calls instead of failing immediately when the server is not yet reachable. Start the client before the server and the calls will complete once the server comes up, as long as it does so within the deadline — useful for services with uncertain startup ordering

**Shared utilities:**
- `Servers.java` — starts a server on port 50051 with a graceful 10-second shutdown hook; all servers delegate to this
- `Loggers.java` — initializes JUL logging; called by `Servers.start()`
- `Constants.java` — NATO phonetic alphabet names used as test message payloads
- `Delays.java` — sleep helpers used by examples to simulate processing delays

**No test sources** — there is no `src/test/` directory; verification is done by running the server and client pair manually.
