package com.example.grpc.manualflowcontrol;

import com.example.grpc.Delays;
import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Servers;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ManualFlowControlBidirectionalStreamingServer {

    private static final Logger logger = Logger.getLogger(ManualFlowControlBidirectionalStreamingServer.class.getName());

    public static void main(String[] args) throws InterruptedException, IOException {
        Servers.start(new EchoServiceImpl());
    }

    private static class EchoServiceImpl extends EchoServiceGrpc.EchoServiceImplBase {

        @Override
        public StreamObserver<EchoRequest> bidirectionalStreamingEcho(final StreamObserver<EchoResponse> responseObserver) {
            var serverCallStreamObserver = (ServerCallStreamObserver<EchoResponse>) responseObserver;
            serverCallStreamObserver.disableAutoRequest();

            var onReadyHandler = new OnReadyHandler(serverCallStreamObserver);
            serverCallStreamObserver.setOnReadyHandler(onReadyHandler);

            return new StreamObserver<>() {
                final AtomicInteger i = new AtomicInteger(0);

                @Override
                public void onNext(EchoRequest request) {
                    logger.log(Level.INFO, "next request: {0}", request.getMessage());

                    if (i.incrementAndGet() % 10 == 0) {
                        logger.info("wait 5 seconds...");
                        Delays.sleep(5);
                    }

                    var response = EchoResponse.newBuilder().setMessage("hello " + request.getMessage()).build();
                    responseObserver.onNext(response);

                    if (serverCallStreamObserver.isReady()) {
                        serverCallStreamObserver.request(1);
                    } else {
                        onReadyHandler.wasReady.set(false);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    logger.log(Level.WARNING, "error: {0}", Status.fromThrowable(t));
                }

                @Override
                public void onCompleted() {
                    logger.info("completed");
                    responseObserver.onCompleted();
                }
            };
        }

        private static class OnReadyHandler implements Runnable {

            private final ServerCallStreamObserver<EchoResponse> serverCallStreamObserver;
            private final AtomicBoolean wasReady;

            OnReadyHandler(ServerCallStreamObserver<EchoResponse> serverCallStreamObserver) {
                this.serverCallStreamObserver = serverCallStreamObserver;
                this.wasReady = new AtomicBoolean(false);
            }

            @Override
            public void run() {
                if (serverCallStreamObserver.isReady() && !wasReady.get()) {
                    wasReady.set(true);
                    logger.info("server is ready");
                    serverCallStreamObserver.request(1);
                }
            }
        }
    }
}
