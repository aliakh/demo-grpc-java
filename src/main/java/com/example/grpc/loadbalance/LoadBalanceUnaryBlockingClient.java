package com.example.grpc.loadbalance;

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

public class LoadBalanceUnaryBlockingClient {

    private static final Logger logger = Logger.getLogger(LoadBalanceUnaryBlockingClient.class.getName());

    public static void main(String[] args) throws Exception {
        Loggers.init();

        NameResolverRegistry.getDefaultRegistry().register(new ExampleNameResolverProvider());

        var target = "my-scheme:///my-service-name";
        useFirstPickPolicy(target);
        useRoundRobinPolicy(target);
    }

    private static void useFirstPickPolicy(String target) throws InterruptedException {
        logger.info("use default first_pick load balance policy");
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

    private static void useRoundRobinPolicy(String target) throws InterruptedException {
        logger.info("use round_robin load balance policy");
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
