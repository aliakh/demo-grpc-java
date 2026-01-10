package com.example.grpc.cancellation;

import com.example.grpc.Delays;
import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Servers;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CancellationServerStreamingServer {

    private static final Logger logger = Logger.getLogger(CancellationServerStreamingServer.class.getName());

    public static void main(String[] args) throws Exception {
        Servers.start(new EchoServiceImpl());
    }

    private static class EchoServiceImpl extends EchoServiceGrpc.EchoServiceImplBase {
        @Override
        public void serverStreamingEcho(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
            logger.log(Level.INFO, "request: {0}", request.getMessage());
            var serverObserver = (ServerCallStreamObserver<EchoResponse>) responseObserver;

            for (var i = 0; i < 10; i++) {
                if (serverObserver.isCancelled()) {
                    logger.info("server received cancellation");
                    return;
                }

                Delays.sleep(1);

                var response = EchoResponse.newBuilder().setMessage("hello " + request.getMessage() + " " + i).build();
                responseObserver.onNext(response);
            }
            responseObserver.onCompleted();
        }
    }
}
