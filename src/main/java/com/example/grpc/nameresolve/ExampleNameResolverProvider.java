package com.example.grpc.nameresolve;

import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import java.net.URI;

class ExampleNameResolverProvider extends NameResolverProvider {

    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        if (!getDefaultScheme().equals(targetUri.getScheme())) {
            return null;
        }
        return new ExampleNameResolver(targetUri);
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5;
    }

    @Override
    public String getDefaultScheme() {
        return "my-scheme";
    }
}
