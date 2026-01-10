package com.example.grpc.cancellation;

import com.example.grpc.EchoRequest;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Loggers;
import io.grpc.Context;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CancellationServerStreamingBlockingClient {

    private static final Logger logger = Logger.getLogger(CancellationServerStreamingBlockingClient.class.getName());

    public static void main(String[] args) throws Exception {
        Loggers.init();

        var channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        try (var cancellableContext = Context.current().withCancellation()) {
            var blockingStub = EchoServiceGrpc.newBlockingStub(channel);
            var request = EchoRequest.newBuilder().setMessage("world").build();

            cancellableContext.run(() -> {
                var responses = blockingStub.serverStreamingEcho(request);

                var i = 0;
                while (responses.hasNext()) {
                    logger.log(Level.INFO, "next response: {0}", responses.next().getMessage());
                    if (++i > 3) {
                        cancellableContext.cancel(new Exception("Client cancelled the context"));
                        break;
                    }
                }
            });
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC error: {0}", e.getStatus());
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
