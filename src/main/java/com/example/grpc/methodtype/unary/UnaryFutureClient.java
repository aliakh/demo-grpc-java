package com.example.grpc.methodtype.unary;

import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Loggers;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UnaryFutureClient {

    private static final Logger logger = Logger.getLogger(UnaryFutureClient.class.getName());

    public static void main(String[] args) throws Exception {
        Loggers.init();

        var channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        var futureStub = EchoServiceGrpc.newFutureStub(channel);
        var request = EchoRequest.newBuilder().setMessage("world").build();
        var responseFuture = futureStub.unaryEcho(request);

        var done = new CountDownLatch(1);
        Futures.addCallback(responseFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(EchoResponse response) {
                logger.log(Level.INFO, "response: {0}", response.getMessage());
                done.countDown();
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.log(Level.WARNING, "error: {0}", Status.fromThrowable(t));
                done.countDown();
            }
        }, MoreExecutors.directExecutor());

        try {
            done.await();
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
