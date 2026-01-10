package com.example.grpc;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public interface Loggers {

    static void init() {
        System.setProperty(
            "java.util.logging.SimpleFormatter.format",
            "%1$tH:%1$tM:%1$tS.%1$tL %4$s %2$s - %5$s%6$s%n"
        );
    }

    static void initIoGrpc() {
        var grpcLogger = Logger.getLogger("io.grpc");
        grpcLogger.setLevel(Level.FINE);

        var rootLogger = LogManager.getLogManager().getLogger("");
        for (var handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.setLevel(Level.FINE);
            }
        }
    }
}
