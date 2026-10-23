package com.alexkouzel.testing;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Local HTTP server for client tests. Serves queued responses in order (the last
 * one repeats once the queue runs dry) and records every request it receives.
 */
public class LocalHttpServer implements AutoCloseable {
    private final HttpServer server;
    private final ConcurrentLinkedQueue<Response> queue = new ConcurrentLinkedQueue<>();
    private final List<Request> requests = Collections.synchronizedList(new ArrayList<>());
    private volatile Response last = Response.ok("");

    public record Request(long nanoTime, String path, Headers headers) {
    }

    public record Response(int status, Map<String, String> headers, byte[] body, boolean dropConnection) {

        public static Response ok(String body) {
            return status(200, body);
        }

        public static Response status(int status, String body) {
            return new Response(status, Map.of(), body.getBytes(StandardCharsets.UTF_8), false);
        }

        public static Response gzip(String body) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                gzip.write(body.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return new Response(200, Map.of("Content-Encoding", "gzip"), out.toByteArray(), false);
        }

        public static Response deflate(String body) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (DeflaterOutputStream deflate = new DeflaterOutputStream(out)) {
                deflate.write(body.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return new Response(200, Map.of("Content-Encoding", "deflate"), out.toByteArray(), false);
        }

        /** Closes the connection without sending a response, so the client sees an I/O error. */
        public static Response dropped() {
            return new Response(0, Map.of(), new byte[0], true);
        }
    }

    public LocalHttpServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    public LocalHttpServer enqueue(Response... responses) {
        queue.addAll(List.of(responses));
        return this;
    }

    public String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    public List<Request> requests() {
        synchronized (requests) {
            return List.copyOf(requests);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.add(new Request(System.nanoTime(), exchange.getRequestURI().toString(), exchange.getRequestHeaders()));
        Response response = queue.poll();
        if (response == null) {
            response = last;
        } else {
            last = response;
        }
        if (response.dropConnection()) {
            exchange.close();
            return;
        }
        response.headers().forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
        exchange.sendResponseHeaders(response.status(), response.body().length == 0 ? -1 : response.body().length);
        try (OutputStream body = exchange.getResponseBody()) {
            body.write(response.body());
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
