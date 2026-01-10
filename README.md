### Introduction to gRPC for Java developers

#### What is gRPC?

gRPC is a multi-language and cross-platform remote procedure call (RPC) framework initially developed by Google. gRPC is designed for high-performance inter-service communication, whether on-premises, in the cloud, in containers, on mobile and IoT devices, or in browsers.

gRPC uses HTTP/2 as a transport protocol along with Protocol Buffers (Protobuf) as a binary serialization framework and RPC interface definition language. These foundations give gRPC performance and capability advantages over RESTful services, which typically transfer textual JSON messages over the HTTP/1.1 protocol.

#### Why not REST?

RPC (Remote Procedure Call) is a distinct approach to building inter-service communication, quite different from REST (Representational State Transfer). REST is an architectural style based on the concept of resources. A resource is identified by a URI, and clients can create, read, update, or delete the *state* of the resource by *transferring* its *representation*.

However, with REST architecture, problems arise when implementing client-server interactions that go beyond client-initiated reading or writing of the state of a single resource, for example:

* Reading and writing complex data structures comprising multiple resources.
* High-throughput and low-latency communication.
* Client streaming or bidirectional streaming.

RPC is based on the technique of calling methods in another process (either on the same machine or on a different machine over the network) as if they were local methods. RPC frameworks provide code generation tools that create client and server stubs based on a given RPC service. These stubs handle message serialization and network communication. As a result, when a client calls a remote method with parameters and receives a return value, it appears to be a local method call. RPC frameworks aim to hide the complexity of serialization and communication from developers. (However, developers using RPC should be aware that the network is inherently [unreliable](https://en.wikipedia.org/wiki/Fallacies_of_distributed_computing) and should implement retry/deadline/cancellation and exception handling to manage partial and total network failures.)

![Remote Procedure Call](/images/Remote_Procedure_Call.png)

#### The problem

When developing an effective RPC framework, developers had to address two primary challenges. First, it is necessary to ensure efficient cross-language and cross-platform serialization. Solutions based on textual formats (such as JSON, YAML, or XML) are typically an order of magnitude less efficient than binary formats. They require additional computational overhead for serialization and additional network bandwidth for transmitting larger messages. To reduce the size of transmitted messages, there is no alternative to using binary formats. (However, such solutions are often not portable between different languages and platforms, and ensuring backward and especially forward compatibility presents significant challenges.)

Second, no effective application-layer protocol existed that was specifically designed for modern inter-service communication. Initially, the HTTP protocol was designed to allow clients (typically browsers) to request resources, such as HTML documents, images, and scripts, from servers in hypermedia systems. It was not designed to support high-speed, bidirectional, simultaneous communication. Various workarounds based on HTTP/1.0 (such as short polling, long polling, and streaming) were inherently inefficient in their utilization of computational and network resources. Even new features introduced in HTTP/1.1 (persistent connections, pipelining, and chunked transfer encoding) proved insufficient for these purposes. (The TCP transport-layer protocol would have provided high-performance full-duplex communication, but it is too low-level to implement an efficient RPC framework based on it.)

#### The solution

Since 2001, Google had been developing an internal RPC framework named Stubby. It was designed to connect almost all internal microservices, both within and across Google data centers. Stubby was a high-performance multi-language and cross-platform framework that used Protobuf for serialization.

In 2015, with the emergence of the HTTP/2 protocol, Google decided to take advantage of HTTP/2's features in a redesigned version of Stubby. References to Google's internal infrastructure (mainly name resolution and load balancing) were removed from the framework, and the project was redesigned to comply with open source standards. The framework has also been adapted for use in cloud-native applications and on resource-constrained mobile and IoT devices. This updated version was released as gRPC (which recursively stands for **g**RPC **R**emote **P**rocedure **C**alls).

At the time of writing, gRPC remains the primary mechanism for inter-service communication at Google and many other major technology companies. Additionally, Google offers gRPC interfaces alongside REST interfaces for many of its public services. This is because gRPC provides significant performance benefits and supports bidirectional streaming — a feature that is not achievable with standard HTTP/1.1-based RESTful services.

#### gRPC foundations

The gRPC framework is based on two main components:

* HTTP/2 — an application-layer protocol used as a transport protocol.
* Protocol Buffers — a binary serialization framework and RPC interface definition language.

![gRPC workflow](/images/gRPC_workflow.png)

##### HTTP/2

HTTP/2 is the second major version of the HTTP application-layer protocol. HTTP/2 evolved from SPDY, an experimental protocol developed by Google starting in 2009 with the primary goal of reducing web latency. HTTP/2 retains the semantics of the previous version of the protocol (such as methods, response codes, and headers) but introduces significant changes in implementation. While HTTP/2 brings several changes that benefit various platforms (such as browsers and mobile devices), only some of these improvements are relevant to gRPC.

The first improvement is multiplexing, which allows multiple concurrent requests and responses to be sent over a single TCP connection. This solves the HTTP *head-of-line blocking* problem, where a slow response to one request delays subsequent requests on the same connection. In HTTP/2, requests and responses are divided into frames that can be transmitted independently of each other within a stream. This approach enables efficient streaming from client to server, from server to client, and simultaneous bidirectional streaming.

The second improvement is the transition from text-based headers and bodies to a binary format. The binary framing layer encodes all communication between the client and server (headers, data, control, and other frame types) into a binary format. This approach reduces the number of bytes transmitted over the network and lowers computational overhead for encoding.

The third improvement is header compression using the HPACK algorithm, which uses static and dynamic header tables together with Huffman encoding to reduce the size of headers transferred over the network. This is particularly beneficial when multiple consecutive requests and responses share the same headers, which is common in inter-service communication.

##### Protocol Buffers

Protocol Buffers (Protobuf) is a multi-language serialization framework and RPC interface definition language, also developed by Google. By default, gRPC uses Protobuf to describe the RPC contract between client and service, including methods exposed by the server, and the structure of request and response messages. This contract is strongly typed and explicitly designed to support backward and forward compatibility.

As a serialization framework, Protobuf is designed to encode structured data (which is common in object-oriented languages) into a compact binary format. Protobuf is highly optimized to minimize network overhead by reducing the message size. (However, if developers have to minimize computational and memory overhead at the expense of increased message size, they can use gRPC with zero-copy serialization frameworks like FlatBuffers or Cap’n Proto.)

As an interface definition language (IDL), the Protobuf compiler with the language-specific plugin generates client and service stubs from declared RPC services, which developers should use to implement their applications. Generated stubs rely on language-specific runtime libraries that transparently handle binary serialization and transmission of messages over the network.

Streaming with flow control is one of the most important features of gRPC, enabled by the underlying HTTP/2 protocol. Depending on whether the client sends a single request or a stream of requests, and whether the service returns a single response or a stream of responses, there are four supported communication patterns:

* Unary: the client sends a single request, and the server replies with a single response.
* Server-side streaming: the client sends a single request, and the server replies with multiple responses.
* Client-side streaming: the client sends multiple requests, and the server replies with a single response.
* Bidirectional streaming: the client sends multiple requests and the server sends multiple responses concurrently.

#### gRPC in practice

The following example demonstrates how to build a simple server-streaming gRPC application using plain Java. The application consists of an echo client that sends one request, and an echo server that receives this request and returns multiple responses. The client receives the responses and displays them. (Client and server examples using the other communication patterns are available in the GitHub [repository](https://github.com/aliakh/demo-grpc-java).)

![gRPC server-side streaming](/images/gRPC_server_side_streaming.png)

To implement this application, complete the following steps:

1. Define an RPC service interface in a *.proto* file.
2. Generate client and server stubs using the Protobuf compiler.
3. Implement a server that provides this service.
4. Implement a client that consumes this service.

##### The contract between a server and a client

The *.proto* file defines the contract between a client and a server. This example shows the *.proto* file used by both the client and the server in the application. Beyond the message and service definitions, the file also contains additional metadata. The *syntax* option defines the use of Protobuf version 3 (by default, version 2 is used for backward compatibility). The *package* option defines the global cross-language Protobuf package namespace. Additionally, each language can have its own Protobuf options. For Java, the *java_package* option defines the package where the generated Java classes are placed, and the *java_multiple_files = true* option defines generating separate Java files for each message and service defined in the *.proto* file.

```
syntax = "proto3";

package example.grpc;

option java_package = "com.example.grpc";
option java_multiple_files = true;

message EchoRequest {
  string message = 1;
}

message EchoResponse {
  string message = 1;
}

service EchoService {
  rpc UnaryEcho(EchoRequest) returns (EchoResponse);
  rpc ServerStreamingEcho(EchoRequest) returns (stream EchoResponse);
  rpc ClientStreamingEcho(stream EchoRequest) returns (EchoResponse);
  rpc BidirectionalStreamingEcho(stream EchoRequest) returns (stream EchoResponse);
}
```

##### Generating client and server stubs

To use gRPC in your Gradle project, place your *.proto* file in the *src/main/proto* directory, add the required implementation and runtime dependencies to the Gradle project, and configure the Protobuf Gradle plugin. Next, execute the Gradle task *generateProto*, and the generated Java classes will be placed in a designated directory (in our example, *build/generated/source/proto/main/java*). These generated classes fall into two categories: message classes and service classes.

For the `EchoRequest` message, an immutable `EchoRequest` class is generated to handle data storage and serialization, along with its inner `Builder` class to create the `EchoRequest` class using the Builder pattern. Similar classes are generated for the `EchoResponse` message.

For the `EchoService` service, an `EchoServiceGrpc` class is generated, containing inner classes for both providing and consuming the remote service. For the server-side, an abstract inner `EchoServiceImplBase` class is generated as the server stub, which you should extend to provide the service logic. For the client-side, four types of client stubs as inner classes are generated:

* `EchoServiceStub`: to make asynchronous calls using the `StreamObserver` interface (it supports all four communication patterns).
* `EchoServiceBlockingStub`: to make blocking calls (it supports unary and server-streaming calls only).
* `EchoServiceBlockingV2Stub`: to make blocking calls (it supports unary calls as a stable feature and all 3 streaming calls as experimental features), and it can throw a checked `StatusException` instead of a runtime `StatusRuntimeException`.
* `EchoServiceFutureStub`: to make asynchronous calls with the `ListenableFuture` interface (it supports unary calls only).

The `StreamObserver` interface is the preferred gRPC Java API for handling streaming, for both transmitting and receiving. For outbound messages, the gRPC runtime library provides a stream observer instance, and the sender should call its methods to transmit messages. To send the next message, you should call the `onNext` method; to complete the RPC with an exception, you should call the `onError` method; to successfully complete the RPC, you should call the `onCompleted` method. For inbound messages, the receiver implements this interface and passes it to the gRPC runtime library, which will call the appropriate methods when the corresponding events occur:

```
public interface StreamObserver<V> {
  void onNext(V value);
  void onError(Throwable t);
  void onCompleted();
}
```

You should always choose an asynchronous stub in production applications, because communication over the network is inherently asynchronous. Blocking a thread when calling a blocking stub wastes limited computing resources (unless virtual threads are in use). However, using a blocking stub is entirely justified in specific cases, such as integration tests, prototyping, and one-off command-line scripts.

##### Creating the server

The next step in the application implementation is to create an echo server. To implement a server that provides this service, complete the following steps:

1. Override the service methods in the generated service stub.
2. Start a server to listen for client requests.

We create the `EchoServiceImpl` class that extends the auto-generated abstract `EchoServiceGrpc.EchoServiceImplBase` class. The class overrides the `serverStreamingEcho` method, which receives the request as an `EchoRequest` instance to read from, and a provided `EchoResponse` stream observer to write to.

To process a client request, the server executes the following steps. For each message, it constructs an `EchoResponse` using the builder and sends it to the client by calling the `onNext` method. After all messages have been sent, the server calls the `onCompleted` method to indicate that the call finished successfully. (Had an error occurred while processing the response, the server would have called the `onError` method to indicate that the call finished exceptionally.)

```
private static class EchoServiceImpl extends EchoServiceGrpc.EchoServiceImplBase {
   @Override
   public void serverStreamingEcho(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
       var name = request.getMessage();
       logger.log(Level.INFO, "request: {0}", name);

       responseObserver.onNext(EchoResponse.newBuilder().setMessage("hello " + name).build());
       responseObserver.onNext(EchoResponse.newBuilder().setMessage("guten tag " + name).build());
       responseObserver.onNext(EchoResponse.newBuilder().setMessage("bonjour " + name).build());
       responseObserver.onCompleted();
   }
}
```

To implement a server that provides this service, use the `ServerBuilder` class. First, specify the port to listen for client requests by calling the `forPort` method. Next, create an instance of the `EchoServiceImpl` service and register it in the server using the `addService` method (a server can provide multiple services). Finally, build and start the server using `grpc-netty-shaded`, the embedded Netty transport included in the runtime dependency.

```
var server = ServerBuilder
   .forPort(50051)
   .addService(new EchoServiceImpl())
   .build()
   .start();

logger.log(Level.INFO, "server started, listening on {0,number,#}", server.getPort());

Runtime.getRuntime().addShutdownHook(new Thread(() -> {
   System.err.println("server is shutting down");
   try {
       server.shutdown().awaitTermination(10, TimeUnit.SECONDS);
   } catch (InterruptedException e) {
       Thread.currentThread().interrupt();
       server.shutdownNow();
   }
   System.err.println("server has been shut down");
}));

server.awaitTermination();
```

##### Creating the client

The next step in the application implementation is to create an echo client. To implement a client that consumes this service, complete the following steps:

* Create a channel to connect to the service.
* Obtain a client stub for the required communication pattern.
* Call the service method using the obtained client stub.

We create a channel using the `ManagedChannelBuilder` class, specifying the server host and port to which we want to connect. In the first client example, we demonstrate the use of the server-streaming service with a synchronous blocking stub. This stub is obtained from the generated `EchoServiceGrpc` class by calling the `newBlockingStub` factory method and passing the channel as an argument.

In this example, the request is provided as a method parameter, and responses are returned as an iterator. With this communication pattern, the client blocks on each iteration while waiting for the next response from the server. A call to this stub either returns responses from the server or throws a `StatusRuntimeException`, in which case the gRPC error is encoded as a `Status` instance. After the call is completed, the channel is shut down to ensure that the underlying resources (threads and TCP connections) are released.

```
var channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();

try {
    var blockingStub = EchoServiceGrpc.newBlockingStub(channel);

   var request = EchoRequest.newBuilder().setMessage("world").build();
   var responses = blockingStub.serverStreamingEcho(request);
   while (responses.hasNext()) {
       logger.log(Level.INFO, "next response: {0}", responses.next().getMessage());
   }
} catch (StatusRuntimeException e) {
    logger.log(Level.WARNING, "RPC error: {0}", e.getStatus());
} finally {
   channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
}
```

In the second client example, we demonstrate the use of the same server-streaming service with an asynchronous non-blocking stub. This stub is obtained from the same auto-generated `EchoServiceGrpc` class by calling the `newStub` factory method and passing the same channel as an argument. As in the previous example, the request is provided as the first method parameter. The response is handled through a stream observer, which the client implements and passes as the second method parameter.

With this communication pattern, the client does not block on the `serverStreamingEcho` method. To wait for the asynchronous communication to complete (either successfully or with an exception), we use a `CountDownLatch` as a thread barrier. The main thread will be blocked until the `countDown` method is called, which occurs in either the `onError` or `onCompleted` handler of the response stream observer. The `onNext` method is called each time the client receives a response from the server. The `onError` method can be called once if the call has finished exceptionally. The `onCompleted` method is called once after the server has successfully sent all responses. After the call ends, the channel is similarly shut down.

```
var channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();

var asyncStub = EchoServiceGrpc.newStub(channel);
var request = EchoRequest.newBuilder().setMessage("world").build();

var done = new CountDownLatch(1);
asyncStub.serverStreamingEcho(request, new StreamObserver<>() {
   @Override
   public void onNext(EchoResponse response) {
       logger.log(Level.INFO, "next response: {0}", response.getMessage());
   }

   @Override
   public void onError(Throwable t) {
       logger.log(Level.WARNING, "error: {0}", Status.fromThrowable(t));
       done.countDown();
   }

   @Override
   public void onCompleted() {
       logger.info("completed");
       done.countDown();
   }
});

try {
    done.await();
} finally {
    channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
}
```

##### Running the client and the server

To build the application, run the Gradle *shadowJar* task to produce a self-contained (über) JAR. Then, start the client and server in any order. Because the client stub is configured to wait for server readiness, it will wait until the server becomes available or the specified deadline is reached.

After the client has sent the request to the server and received the responses, the client closes the channel and shuts itself down. To stop the server, press *Ctrl+C* to send a `SIGINT` signal to it. The server then shuts down gracefully as the JVM executes its registered shutdown hooks. (We use logging to *stderr* here since the logger may have been reset by its JVM shutdown hook.)

#### Conclusion

gRPC is a high-performance framework for implementing inter-service communication. However, like any technology, it is not a universal solution and is designed to address specific problems. You should consider migrating your application from REST to gRPC if it meets most of the following criteria:

* The application has performance requirements, including high throughput and low latency.
* The application requires client streaming or bidirectional streaming, which cannot be efficiently implemented using HTTP/1.1.
* Automatic generation of gRPC client and server stubs is available for all required languages and platforms.
* The client and the server are developed within the same organization, and the application operates in a controlled environment.
* Your organization has strict engineering standards that require clearly defined client-server contracts.
* Development will benefit from built-in gRPC features, such as retry/deadline/cancellation, manual flow control, error propagation, interceptors, authentication, name resolution, client-side load balancing, health checking, and proxyless service mesh.

However, REST is a more appropriate architecture if the application meets most of the following conditions:

* The application is simple and operates under low load, and there is simply no need to increase its performance.
* The application uses unary requests/responses and does not require streaming. (Or the application *does* use streaming using the WebSockets protocol, but you consider this does not violate the REST architecture.)
* Requests to the server are made directly from a browser, but using the gRPC-Web proxy is not technically justified.
* The application exposes a public API for consumption by a broad audience of external developers beyond your organization.
* Your organization can achieve successful backward and forward compatibility without strict constraints.

As a rule of thumb, you should migrate your RESTful services to gRPC when you need high-performance inter-service communication, especially with client streaming or bidirectional streaming.
