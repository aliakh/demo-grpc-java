package com.example.grpc.nameresolve;

import com.example.grpc.Constants;
import com.example.grpc.EchoRequest;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Loggers;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolverRegistry;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NameResolveUnaryBlockingClient {

    private static final Logger logger = Logger.getLogger(NameResolveUnaryBlockingClient.class.getName());

    public static void main(String[] args) throws Exception {
        Loggers.init();

        NameResolverRegistry.getDefaultRegistry().register(new ExampleNameResolverProvider());

        useDnsResolver("localhost:50051");
        useExampleNameResolver("my-scheme:///my-service-name");
    }

    private static void useDnsResolver(String target) throws InterruptedException {
        logger.info("use default DNS resolver");
        var channel = ManagedChannelBuilder.forTarget(target)
            .usePlaintext()
            .build();
        try {
            for (int i = 0; i < 5; i++) {
                unaryEcho(channel, Constants.getNames().get(i));
            }
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private static void useExampleNameResolver(String target) throws InterruptedException {
        logger.info("use example name resolver");
        var channel = ManagedChannelBuilder.forTarget(target)
            .defaultLoadBalancingPolicy("round_robin")
            .usePlaintext()
            .build();
        try {
            for (int i = 0; i < 5; i++) {
                unaryEcho(channel, Constants.getNames().get(i));
            }
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private static void unaryEcho(ManagedChannel channel, String name) {
        try {
            var blockingStub = EchoServiceGrpc.newBlockingStub(channel);
            var request = EchoRequest.newBuilder().setMessage(name).build();
            var response = blockingStub.unaryEcho(request);
            logger.log(Level.INFO, "response: {0}", response.getMessage());
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC error: {0}", e.getStatus());
        }
    }
}
