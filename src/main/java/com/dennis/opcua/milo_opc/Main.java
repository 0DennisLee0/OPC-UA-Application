package com.dennis.opcua.milo_opc;

import server.ExampleServer;

import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) throws Exception {
        ExampleServer server = ExampleServer.getInstance();

        server.startup().get();

        final CompletableFuture<Void> future = new CompletableFuture<>();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> future.complete(null)));

        future.get();
    }
}

