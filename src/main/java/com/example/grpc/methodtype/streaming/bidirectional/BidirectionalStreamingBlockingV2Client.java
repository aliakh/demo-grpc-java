package com.example.grpc.methodtype.streaming.bidirectional;

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
public class BidirectionalStreamingBlockingV2Client {
    private static final Logger logger = Logger.getLogger(BidirectionalStreamingBlockingV2Client.class.getName());

    public static void main(String[] args) throws Exception {
        Loggers.init();

        var channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        var blockingStub = EchoServiceGrpc.newBlockingV2Stub(channel);
        var blockingClientCall = blockingStub.bidirectionalStreamingEcho();

        try {
            blockingClientCall.write(EchoRequest.newBuilder().setMessage("world").build());
            blockingClientCall.write(EchoRequest.newBuilder().setMessage("welt").build());
            blockingClientCall.write(EchoRequest.newBuilder().setMessage("monde").build());

            blockingClientCall.halfClose();

            for (EchoResponse response; (response = blockingClientCall.read()) != null; ) {
                logger.log(Level.INFO, "next response: {0}", response.getMessage());
            }
            logger.info("completed");
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC runtime error: {0}", e.getStatus());
        } catch (StatusException e) {
            logger.log(Level.WARNING, "RPC checked error: {0}", e.getStatus());
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
