package com.example.grpc.deadline;

import com.example.grpc.EchoRequest;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Loggers;
import io.grpc.Deadline;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeadlineServerStreamingBlockingClient {

    private static final Logger logger = Logger.getLogger(DeadlineServerStreamingBlockingClient.class.getName());

    public static void main(String[] args) throws Exception {
        Loggers.init();

        var channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        try {
            var blockingStub = EchoServiceGrpc.newBlockingStub(channel)
                .withDeadline(Deadline.after(3, TimeUnit.SECONDS));

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
    }
}
