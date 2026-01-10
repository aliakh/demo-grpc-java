package com.example.grpc.keepalive;

import com.example.grpc.Delays;
import com.example.grpc.EchoRequest;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Loggers;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeepAliveUnaryBlockingClient {

    private static final Logger logger = Logger.getLogger(KeepAliveUnaryBlockingClient.class.getName());

    public static void main(String[] args) throws Exception {
        Loggers.init();
        Loggers.initIoGrpc();

        var channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext()
            .keepAliveTime(10, TimeUnit.SECONDS)
            .keepAliveTimeout(1, TimeUnit.SECONDS)
            .keepAliveWithoutCalls(true)
            .build();

        try {
            var blockingStub = EchoServiceGrpc.newBlockingStub(channel);

            var request = EchoRequest.newBuilder().setMessage("world").build();
            var response = blockingStub.unaryEcho(request);
            logger.log(Level.INFO, "response: {0}", response.getMessage());
            Delays.sleep(30);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC error: {0}", e.getStatus());
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
