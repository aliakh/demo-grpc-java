package com.example.grpc.deadline;

import com.example.grpc.Delays;
import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Servers;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeadlineServerStreamingServer {

    private static final Logger logger = Logger.getLogger(DeadlineServerStreamingServer.class.getName());

    public static void main(String[] args) throws Exception {
        Servers.start(new EchoServiceImpl());
    }

    private static class EchoServiceImpl extends EchoServiceGrpc.EchoServiceImplBase {
        @Override
        public void serverStreamingEcho(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
            logger.log(Level.INFO, "request: {0}", request.getMessage());
            var context = Context.current();

            for (var i = 0; i < 10; i++) {
                if (context.isCancelled()) {
                    logger.log(Level.WARNING, "cancelled by client: {0}", context.cancellationCause());
                    return;
                }

                Delays.sleep(i);

                var response = EchoResponse.newBuilder().setMessage("hello " + request.getMessage() + " " + i).build();
                logger.log(Level.INFO, "response: {0}", response.getMessage());
                responseObserver.onNext(response);
            }
            responseObserver.onCompleted();
        }
    }
}
