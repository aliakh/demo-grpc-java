package com.example.grpc.retrying;

import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Servers;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RetryingUnaryServer {

    private static final Logger logger = Logger.getLogger(RetryingUnaryServer.class.getName());

    public static void main(String[] args) throws Exception {
        Servers.start(new EchoServiceImpl());
    }

    private static class EchoServiceImpl extends EchoServiceGrpc.EchoServiceImplBase {

        private static final float UNAVAILABLE_PERCENTAGE = 0.5F;

        private final AtomicInteger i = new AtomicInteger(0);

        @Override
        public void unaryEcho(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
            var random = ThreadLocalRandom.current();
            i.incrementAndGet();
            if (random.nextFloat() < UNAVAILABLE_PERCENTAGE) {
                logger.log(Level.INFO, "returning unavailable error: #{0}", i.get());
                responseObserver.onError(Status.UNAVAILABLE.withDescription("Server temporarily unavailable").asRuntimeException());
            } else {
                logger.log(Level.INFO, "returning successful response: #{0}", i.get());
                var response = EchoResponse.newBuilder().setMessage("hello " + request.getMessage()).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
    }
}
