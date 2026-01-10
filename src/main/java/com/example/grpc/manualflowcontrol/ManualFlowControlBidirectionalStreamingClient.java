package com.example.grpc.manualflowcontrol;

import com.example.grpc.Constants;
import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Loggers;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ManualFlowControlBidirectionalStreamingClient {

    private static final Logger logger = Logger.getLogger(ManualFlowControlBidirectionalStreamingClient.class.getName());

    public static void main(String[] args) throws Exception {
        Loggers.init();

        var channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        var stub = EchoServiceGrpc.newStub(channel);

        var done = new CountDownLatch(1);
        stub.bidirectionalStreamingEcho(new ClientResponseObserver<EchoRequest, EchoResponse>() {

            private ClientCallStreamObserver<EchoRequest> requestStream;

            @Override
            public void beforeStart(ClientCallStreamObserver<EchoRequest> requestStream) {
                this.requestStream = requestStream;
                requestStream.disableAutoRequestWithInitial(1);
                requestStream.setOnReadyHandler(new OnReadyHandler(requestStream, Constants.getNames().iterator()));
            }

            @Override
            public void onNext(EchoResponse response) {
                logger.log(Level.INFO, "next response: {0}", response.getMessage());
                requestStream.request(1);
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.WARNING, "error: {0}", Status.fromThrowable(t));
                done.countDown();
            }

            @Override
            public void onCompleted() {
                logger.info("responses completed");
                done.countDown();
            }
        });

        try {
            done.await();
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private static class OnReadyHandler implements Runnable {

        private final ClientCallStreamObserver<EchoRequest> requestStream;
        private final Iterator<String> names;

        private OnReadyHandler(ClientCallStreamObserver<EchoRequest> requestStream, Iterator<String> names) {
            this.requestStream = requestStream;
            this.names = names;
        }

        @Override
        public void run() {
            while (requestStream.isReady()) {
                if (names.hasNext()) {
                    var request = EchoRequest.newBuilder().setMessage(names.next()).build();
                    logger.log(Level.INFO, "next request: {0}", request.getMessage());
                    requestStream.onNext(request);
                } else {
                    logger.info("requests completed");
                    requestStream.onCompleted();
                    return;
                }
            }
        }
    }
}
