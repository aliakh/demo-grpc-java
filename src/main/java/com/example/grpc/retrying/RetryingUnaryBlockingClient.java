package com.example.grpc.retrying;

import com.example.grpc.Constants;
import com.example.grpc.EchoRequest;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Loggers;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RetryingUnaryBlockingClient {

    private static final Logger logger = Logger.getLogger(RetryingUnaryBlockingClient.class.getName());

    public static void main(String[] args) throws Exception {
        Loggers.init();

        var enableRetries = !Boolean.parseBoolean(System.getenv("EXAMPLE_GRPC_DISABLE_RETRYING"));
        var channel = buildChannel(enableRetries);
        var blockingStub = EchoServiceGrpc.newBlockingStub(channel);

        var sentCalls = new AtomicInteger();
        var failedCalls = new AtomicInteger();

        try (var executor = new ForkJoinPool()) {
            for (var name : Constants.getNames()) {
                executor.execute(() -> {
                    try {
                        var request = EchoRequest.newBuilder().setMessage(name).build();
                        var response = blockingStub.unaryEcho(request);
                        logger.log(Level.INFO, "response: {0}", response.getMessage());
                    } catch (StatusRuntimeException e) {
                        failedCalls.incrementAndGet();
                        logger.log(Level.WARNING, "error: {0}", e.getStatus());
                    } finally {
                        sentCalls.incrementAndGet();
                    }
                });
            }
            executor.awaitQuiescence(120, TimeUnit.SECONDS);

            logger.log(Level.INFO, "retrying: {0}, sent calls: {1}, failed calls: {2}",
                new Object[]{enableRetries ? "enabled" : "disabled", sentCalls.get(), failedCalls.get()});
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private static ManagedChannel buildChannel(boolean enableRetries) {
        var channelBuilder = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext();
        if (enableRetries) {
            var serviceConfig = getRetryingServiceConfig();
            logger.log(Level.INFO, "service configuration: {0}", serviceConfig);
            channelBuilder.defaultServiceConfig(serviceConfig).enableRetry();
        }
        return channelBuilder.build();
    }

    private static Map<String, ?> getRetryingServiceConfig() {
        return Map.of(
            "methodConfig", List.of(
                Map.of(
                    "name", List.of(
                        Map.of(
                            "service", "example.grpc.EchoService",
                            "method", "UnaryEcho"
                        )
                    ),
                    "retryPolicy", Map.of(
                        "maxAttempts", 5,
                        "initialBackoff", "0.5s",
                        "maxBackoff", "30s",
                        "backoffMultiplier", 2.0,
                        "retryableStatusCodes", List.of("UNAVAILABLE")
                    )
                )
            )
        );
    }
}
