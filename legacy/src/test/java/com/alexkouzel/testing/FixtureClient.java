package com.alexkouzel.testing;

import com.alexkouzel.client.HttpDataClient;
import com.alexkouzel.client.exceptions.HttpRequestException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client that serves fixtures for exact URLs instead of calling EDGAR.
 * <p>
 * Loaders build URLs themselves, so routing by exact URL also checks that
 * they ask for the right one: an unrouted URL fails the request.
 */
public class FixtureClient extends HttpDataClient {
    private final Map<String, String> routes = new HashMap<>();
    private final List<String> requestedUrls = new ArrayList<>();

    public FixtureClient() {
        super(1, statusCode -> {
        }, null);
    }

    public FixtureClient route(String url, String fixturePath) {
        routes.put(url, fixturePath);
        return this;
    }

    public List<String> requestedUrls() {
        return requestedUrls;
    }

    @Override
    protected HttpRequest buildRequest(String url, String contentType) {
        throw new UnsupportedOperationException("FixtureClient never sends HTTP requests");
    }

    @Override
    public String loadText(String url) throws HttpRequestException {
        return new String(load(url), StandardCharsets.UTF_8);
    }

    @Override
    public InputStream loadStream(String url, String contentType) throws HttpRequestException {
        return new ByteArrayInputStream(load(url));
    }

    private byte[] load(String url) throws HttpRequestException {
        requestedUrls.add(url);
        String path = routes.get(url);
        if (path == null) {
            throw new HttpRequestException("No fixture routed for " + url);
        }
        return Fixtures.bytes(path);
    }
}
