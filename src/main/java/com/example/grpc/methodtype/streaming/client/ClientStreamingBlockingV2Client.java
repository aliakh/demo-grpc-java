package com.example.grpc.methodtype.streaming.client;

import com.example.grpc.EchoRequest;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Loggers;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
public class ClientStreamingBlockingV2Client {

    private static final Logger logger = Logger.getLogger(ClientStreamingBlockingV2Client.class.getName());

    public static void main(String[] args) throws Exception {
        Loggers.init();

        var channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        var blockingStub = EchoServiceGrpc.newBlockingV2Stub(channel);
        var blockingClientCall = blockingStub.clientStreamingEcho();

        try {
            for (var message : new String[]{"world", "welt", "monde"}) {
                var request = EchoRequest.newBuilder().setMessage(message).build();
                logger.log(Level.INFO, "request: {0}", request.getMessage());
                blockingClientCall.write(request);
            }

            blockingClientCall.halfClose();

            var response = blockingClientCall.read();
            logger.log(Level.INFO, "response: {0}", response.getMessage());
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC runtime error: {0}", e.getStatus());
        } catch (StatusException e) {
            logger.log(Level.WARNING, "RPC checked error: {0}", e.getStatus());
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
