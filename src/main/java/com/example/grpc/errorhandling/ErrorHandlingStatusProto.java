package com.example.grpc.errorhandling;

import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import com.google.common.base.VerifyException;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import com.google.rpc.DebugInfo;
import com.google.rpc.Status;
import io.grpc.Channel;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.ManagedChannelBuilder;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Verify.verify;

public class ErrorHandlingStatusProto {

    private static final DebugInfo DEBUG_INFO =
        DebugInfo.newBuilder()
            .addStackEntries("stack_entry_1")
            .addStackEntries("stack_entry_2")
            .addStackEntries("stack_entry_3")
            .setDetail("Error detail").build();

    private static final String STATUS_MESSAGE = "Error description";

    public static void main(String[] args) throws Exception {
        var server = Grpc.newServerBuilderForPort(0, InsecureServerCredentials.create())
            .addService(new EchoServiceGrpc.EchoServiceImplBase() {
                @Override
                public void unaryEcho(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
                    var status = Status.newBuilder()
                        .setCode(Code.INVALID_ARGUMENT.getNumber())
                        .setMessage(STATUS_MESSAGE)
                        .addDetails(Any.pack(DEBUG_INFO))
                        .build();
                    responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                }
            })
            .build()
            .start();

        var channel = ManagedChannelBuilder.forAddress("localhost", server.getPort()).usePlaintext().build();

        blockingCall(channel);
        futureCallDirect(channel);
        futureCallCallback(channel);
        asyncCall(channel);

        channel.shutdown();
        server.shutdown();
        channel.awaitTermination(1, TimeUnit.SECONDS);
        server.awaitTermination(1, TimeUnit.SECONDS);
    }

    private static void verifyErrorResponse(Throwable t) {
        var status = StatusProto.fromThrowable(t);

        verify(status.getCode() == Code.INVALID_ARGUMENT.getNumber());
        verify(status.getMessage().equals(STATUS_MESSAGE));
        try {
            var unpackedMessage = status.getDetails(0).unpack(DebugInfo.class);
            verify(unpackedMessage.equals(DEBUG_INFO));
        } catch (InvalidProtocolBufferException e) {
            throw new VerifyException("Protobuf message is of a different type than expected", e);
        }
    }

    private static void blockingCall(Channel channel) {
        var stub = EchoServiceGrpc.newBlockingStub(channel);

        try {
            stub.unaryEcho(EchoRequest.newBuilder().build());
        } catch (Exception e) {
            verifyErrorResponse(e);
            System.out.println("Blocking call received expected error response");
        }
    }

    private static void futureCallDirect(Channel channel) {
        var stub = EchoServiceGrpc.newFutureStub(channel);
        var response = stub.unaryEcho(EchoRequest.newBuilder().build());

        try {
            response.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            verifyErrorResponse(e.getCause());
            System.out.println("Future direct call received expected error response");
        }
    }

    private static void futureCallCallback(Channel channel) {
        var stub = EchoServiceGrpc.newFutureStub(channel);
        var responseFuture = stub.unaryEcho(EchoRequest.newBuilder().build());

        var done = new CountDownLatch(1);
        Futures.addCallback(responseFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(EchoResponse response) {
                    // won't be called
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    verifyErrorResponse(t);
                    System.out.println("Future callback received expected error response");
                    done.countDown();
                }
            },
            MoreExecutors.directExecutor());

        awaitCompletion(done);
    }

    private static void asyncCall(Channel channel) {
        var stub = EchoServiceGrpc.newStub(channel);
        var request = EchoRequest.newBuilder().build();

        var done = new CountDownLatch(1);
        var responseObserver = new StreamObserver<EchoResponse>() {
            @Override
            public void onNext(EchoResponse value) {
                // won't be called
            }

            @Override
            public void onError(Throwable t) {
                verifyErrorResponse(t);
                System.out.println("Async call received expected error response");
                done.countDown();
            }

            @Override
            public void onCompleted() {
                // won't be called
            }
        };
        stub.unaryEcho(request, responseObserver);

        awaitCompletion(done);
    }

    private static void awaitCompletion(CountDownLatch done) {
        if (!Uninterruptibles.awaitUninterruptibly(done, 1, TimeUnit.SECONDS)) {
            throw new RuntimeException("Await failed to complete within 1 second");
        }
    }
}
