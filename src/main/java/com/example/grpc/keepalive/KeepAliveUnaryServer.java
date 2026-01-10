package com.example.grpc.keepalive;

import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Loggers;
import com.example.grpc.Servers;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeepAliveUnaryServer {

    private static final Logger logger = Logger.getLogger(KeepAliveUnaryServer.class.getName());

    public static void main(String[] args) throws Exception {
        Loggers.initIoGrpc();

        var serverBuilder = ServerBuilder
            .forPort(50051)
            .addService(new EchoServiceImpl())
            .keepAliveTime(5, TimeUnit.SECONDS)
            .keepAliveTimeout(1, TimeUnit.SECONDS)
            .permitKeepAliveTime(5, TimeUnit.SECONDS)
            .permitKeepAliveWithoutCalls(true)
            .maxConnectionIdle(15, TimeUnit.SECONDS)
            .maxConnectionAge(30, TimeUnit.SECONDS)
            .maxConnectionAgeGrace(5, TimeUnit.SECONDS);

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
