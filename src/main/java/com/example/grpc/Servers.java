package com.example.grpc;

import io.grpc.BindableService;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Servers {

    private static final Logger logger = Logger.getLogger(Servers.class.getName());

    public static void start(ServerBuilder<?> serverBuilder) throws IOException, InterruptedException {
        Loggers.init();

        var server = serverBuilder
            .build()
            .start();

        logger.log(Level.INFO, "server started, listening on {0,number,#}", server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("server is shutting down");
            try {
                server.shutdown().awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                server.shutdownNow();
            }
            System.err.println("server has been shut down");
        }));

        server.awaitTermination();
    }

    public static void start(BindableService bindableService) throws IOException, InterruptedException {
        var serverBuilder = ServerBuilder
            .forPort(50051)
            .addService(bindableService);

        start(serverBuilder);
    }
}
