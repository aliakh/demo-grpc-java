package com.example.grpc.methodtype.streaming.server;

import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import com.example.grpc.Servers;
import io.grpc.stub.StreamObserver;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerStreamingServer {

    private static final Logger logger = Logger.getLogger(ServerStreamingServer.class.getName());

    public static void main(String[] args) throws Exception {
        Servers.start(new EchoServiceImpl());
    }

    private static class EchoServiceImpl extends EchoServiceGrpc.EchoServiceImplBase {
        @Override
        public void serverStreamingEcho(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
            var name = request.getMessage();
            logger.log(Level.INFO, "request: {0}", name);

            responseObserver.onNext(EchoResponse.newBuilder().setMessage("hello " + name).build());
            responseObserver.onNext(EchoResponse.newBuilder().setMessage("guten tag " + name).build());
            responseObserver.onNext(EchoResponse.newBuilder().setMessage("bonjour " + name).build());
            responseObserver.onCompleted();
        }
    }
}
