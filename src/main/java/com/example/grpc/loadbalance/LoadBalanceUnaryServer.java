package com.example.grpc.loadbalance;

import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Loggers;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoadBalanceUnaryServer {

    private static final Logger logger = Logger.getLogger(LoadBalanceUnaryServer.class.getName());

    public static void main(String[] args) throws Exception {
        Loggers.init();

        var servers = new ArrayList<Server>();
        for (var port : new int[]{50051, 50052, 50053}) {
            servers.add(
                ServerBuilder.forPort(port)
                    .addService(new EchoServiceImpl(port))
                    .build()
                    .start()
            );
            logger.log(Level.INFO, "server started, listening on {0,number,#}", port);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (var server : servers) {
                System.err.println("server is shutting down");
                try {
                    server.shutdown().awaitTermination(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    server.shutdownNow();
                }
                System.err.println("server has been shut down");
            }
        }));

        for (var server : servers) {
            server.awaitTermination();
        }
    }

    private static class EchoServiceImpl extends EchoServiceGrpc.EchoServiceImplBase {

        private final int port;

        EchoServiceImpl(int port) {
            this.port = port;
        }

        @Override
        public void unaryEcho(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
            var response = EchoResponse.newBuilder().setMessage("hello " + request.getMessage() + " from server on " + this.port).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
