package com.example.grpc.headers;

import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Servers;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.stub.StreamObserver;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HeadersUnaryServer {

    private static final Logger logger = Logger.getLogger(HeadersUnaryServer.class.getName());

    public static void main(String[] args) throws Exception {
        var serverBuilder = ServerBuilder
            .forPort(50051)
            .addService(ServerInterceptors.intercept(new EchoServiceImpl(), new HeadersServerInterceptor()));

        Servers.start(serverBuilder);
    }

    private static class EchoServiceImpl extends EchoServiceGrpc.EchoServiceImplBase {
        @Override
        public void unaryEcho(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
            logger.log(Level.INFO, "request: {0}", request.getMessage());
            var response = EchoResponse.newBuilder().setMessage("hello " + request.getMessage()).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
