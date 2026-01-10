package com.example.grpc.methodtype.streaming.server;

import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Loggers;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
public class ServerStreamingBlockingV2Client {

    private static final Logger logger = Logger.getLogger(ServerStreamingBlockingV2Client.class.getName());

    public static void main(String[] args) throws Exception {
        Loggers.init();

        var channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        try {
            var blockingStub = EchoServiceGrpc.newBlockingV2Stub(channel);

            var request = EchoRequest.newBuilder().setMessage("world").build();
            var blockingClientCall = blockingStub.serverStreamingEcho(request);
            for (EchoResponse response; (response = blockingClientCall.read()) != null; ) {
                logger.log(Level.INFO, "next response: {0}", response.getMessage());
            }
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC runtime error: {0}", e.getStatus());
        } catch (StatusException e) {
            logger.log(Level.WARNING, "RPC checked error: {0}", e.getStatus());
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
