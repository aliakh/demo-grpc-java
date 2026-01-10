package com.example.grpc.methodtype.streaming.bidirectional;

import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Loggers;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BidirectionalStreamingAsynchronousClient {

    private static final Logger logger = Logger.getLogger(BidirectionalStreamingAsynchronousClient.class.getName());

    public static void main(String[] args) throws Exception {
        Loggers.init();

        var channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        var asyncStub = EchoServiceGrpc.newStub(channel);

        var done = new CountDownLatch(1);
        var requestObserver = asyncStub.bidirectionalStreamingEcho(new StreamObserver<>() {
            @Override
            public void onNext(EchoResponse response) {
                logger.log(Level.INFO, "next response: {0}", response.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.WARNING, "error: {0}", Status.fromThrowable(t));
                done.countDown();
            }

            @Override
            public void onCompleted() {
                logger.info("completed");
                done.countDown();
            }
        });

        requestObserver.onNext(EchoRequest.newBuilder().setMessage("world").build());
        requestObserver.onNext(EchoRequest.newBuilder().setMessage("welt").build());
        requestObserver.onNext(EchoRequest.newBuilder().setMessage("monde").build());
        requestObserver.onCompleted();

        try {
            done.await();
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
