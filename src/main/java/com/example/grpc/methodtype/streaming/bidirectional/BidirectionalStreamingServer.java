package com.example.grpc.methodtype.streaming.bidirectional;

import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Servers;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BidirectionalStreamingServer {

    private static final Logger logger = Logger.getLogger(BidirectionalStreamingServer.class.getName());

    public static void main(String[] args) throws Exception {
        Servers.start(new EchoServiceImpl());
    }

    private static class EchoServiceImpl extends EchoServiceGrpc.EchoServiceImplBase {
        @Override
        public StreamObserver<EchoRequest> bidirectionalStreamingEcho(StreamObserver<EchoResponse> responseObserver) {
            return new StreamObserver<>() {
                @Override
                public void onNext(EchoRequest request) {
                    var name = request.getMessage();
                    logger.log(Level.INFO, "next request: {0}", name);
                    responseObserver.onNext(EchoResponse.newBuilder().setMessage("hello " + name).build());
                    responseObserver.onNext(EchoResponse.newBuilder().setMessage("guten tag " + name).build());
                    responseObserver.onNext(EchoResponse.newBuilder().setMessage("bonjour " + name).build());
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
    }
}
