package com.example.grpc.healthservice;

import com.example.grpc.Delays;
import com.example.grpc.EchoRequest;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Loggers;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthGrpc;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HealthServiceUnaryBlockingClient {

    private static final Logger logger = Logger.getLogger(HealthServiceUnaryBlockingClient.class.getName());

    public static void main(String[] args) throws Exception {
        Loggers.init();

        var channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        try {
            var echoBlockingStub = EchoServiceGrpc.newBlockingStub(channel);
            var healthBlockingStub = HealthGrpc.newBlockingStub(channel);

            var names = new String[]{"Alpha", "Beta", "Gamma"};
            checkHealth(healthBlockingStub);

            for (var name : names) {
                unaryEcho(echoBlockingStub, name);
                Thread.sleep(100);
                checkHealth(healthBlockingStub);
            }

            logger.info("wait 10 seconds...");
            Delays.sleep(10);
            checkHealth(healthBlockingStub);

            unaryEcho(echoBlockingStub, "Delta");
            checkHealth(healthBlockingStub);
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private static void checkHealth(HealthGrpc.HealthBlockingStub healthBlockingStub) {
        try {
            var request = HealthCheckRequest.getDefaultInstance();
            var response = healthBlockingStub.check(request);
            logger.log(Level.INFO, "health is: {0}", response.getStatus());
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC error: {0}", e.getStatus());
        }
    }

    private static void unaryEcho(EchoServiceGrpc.EchoServiceBlockingStub echoBlockingStub, String name) {
        try {
            logger.log(Level.INFO, "try to request: {0}", name);
            var request = EchoRequest.newBuilder().setMessage(name).build();
            var response = echoBlockingStub.unaryEcho(request);
            logger.log(Level.INFO, "response: {0}", response.getMessage());
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC error: {0}", e.getStatus());
        }
    }
}
