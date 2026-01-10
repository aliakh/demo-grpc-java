package com.example.grpc.healthservice;

import com.example.grpc.Delays;
import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Loggers;
import com.example.grpc.Servers;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class HealthServiceUnaryServer {

    private static final Logger logger = Logger.getLogger(HealthServiceUnaryServer.class.getName());

    public static void main(String[] args) throws Exception {
        Loggers.init();

        var health = new HealthStatusManager();
        health.setStatus("", ServingStatus.SERVING);

        var serverBuilder = ServerBuilder
            .forPort(50051)
            .addService(new EchoServiceImpl(health))
            .addService(health.getHealthService());

        Servers.start(serverBuilder);
    }

    private static class EchoServiceImpl extends EchoServiceGrpc.EchoServiceImplBase {

        private final HealthStatusManager health;
        private final AtomicBoolean isServing = new AtomicBoolean(true);

        public EchoServiceImpl(HealthStatusManager health) {
            this.health = health;
        }

        @Override
        public void unaryEcho(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
            if (!isServing.get()) {
                responseObserver.onError(Status.UNAVAILABLE.withDescription("Not serving right now").asRuntimeException());
                return;
            }

            if (shouldServe(request)) {
                var response = EchoResponse.newBuilder().setMessage("hello " + request.getMessage()).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                isServing.set(false);
                logger.warning("short message received, will not serve for 10 seconds");
                health.setStatus("", ServingStatus.NOT_SERVING);

                new Thread(() -> {
                    Delays.sleep(10);

                    isServing.set(true);
                    logger.info("continue to serve");
                    health.setStatus("", ServingStatus.SERVING);
                }).start();

                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Paused by short message").asRuntimeException());
            }
        }

        private boolean shouldServe(EchoRequest request) {
            return isServing.get() && request.getMessage().length() >= 5;
        }
    }
}
