package com.example.grpc.loadbalance;

import com.google.common.collect.ImmutableMap;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class ExampleNameResolver extends NameResolver {

    private final URI targetUri;
    private final Map<String, List<InetSocketAddress>> serviceNameToSocketAddresses;

    private Listener2 listener;

    ExampleNameResolver(URI targetUri) {
        this.targetUri = targetUri;
        this.serviceNameToSocketAddresses = ImmutableMap.<String, List<InetSocketAddress>>builder()
            .put("my-service-name",
                Arrays.stream(new int[]{50051, 50052, 50053})
                    .mapToObj(port -> new InetSocketAddress("localhost", port))
                    .collect(Collectors.toList())
            )
            .build();
    }

    @Override
    public String getServiceAuthority() {
        if (targetUri.getHost() != null) {
            return targetUri.getHost();
        }
        return "no host";
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void start(Listener2 listener) {
        this.listener = listener;
        this.resolve();
    }

    @Override
    public void refresh() {
        this.resolve();
    }

    private void resolve() {
        try {
            var serviceName = targetUri.getPath().substring(1);
            var addresses = serviceNameToSocketAddresses.get(serviceName);
            if (addresses == null) {
                throw new IllegalArgumentException("Unknown service name: " + serviceName);
            }
            var equivalentAddressGroups = addresses.stream()
                .map(address -> (SocketAddress) address)
                .map(Arrays::asList)
                .map(EquivalentAddressGroup::new)
                .collect(Collectors.toList());

            var resolutionResult = ResolutionResult.newBuilder()
                .setAddresses(equivalentAddressGroups)
                .build();
            this.listener.onResult(resolutionResult);
        } catch (Exception e) {
            this.listener.onError(Status.UNAVAILABLE.withDescription("Unable to resolve host").withCause(e));
        }
    }

}
