package com.alexkouzel.client;

import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.client.exceptions.StatusCodeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

public abstract class HttpDataClient {

    private final int attemptLimit;

    private final Consumer<Integer> statusCodeHandler;

    protected final RateLimiter rateLimiter;

    private final ObjectMapper jsonMapper;

    private final XmlMapper xmlMapper;

    private final HttpClient client;

    public HttpDataClient(
            int attemptLimit,
            Consumer<Integer> statusCodeHandler,
            RateLimiter rateLimiter
    ) {
        this.attemptLimit = attemptLimit;
        this.statusCodeHandler = statusCodeHandler;
        this.rateLimiter = rateLimiter;
        this.jsonMapper = new ObjectMapper();
        this.xmlMapper = new XmlMapper();
        this.client = HttpClient.newHttpClient();

        JavaTimeModule timeModule = new JavaTimeModule();
        jsonMapper.registerModule(timeModule);
        xmlMapper.registerModule(timeModule);
    }

    protected abstract HttpRequest buildRequest(String url, String contentType);

    public <T> T loadJson(String url, Class<T> type) throws HttpRequestException {
        return loadType(url, type, jsonMapper);
    }

    public <T> T loadXml(String url, Class<T> type) throws HttpRequestException {
        return loadType(url, type, xmlMapper);
    }

    public <T> T loadType(String url, Class<T> type, ObjectMapper mapper) throws HttpRequestException {
        try {
            return mapper.readValue(loadText(url), type);
        } catch (JsonProcessingException e) {
            throw new HttpRequestException(e.getMessage());
        }
    }

    public String loadText(String url) throws HttpRequestException {
        try (InputStream stream = loadStream(url, "text/html")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new HttpRequestException(e.getMessage());
        }
    }

    public InputStream loadStream(String url, String contentType) throws HttpRequestException {
        HttpRequest request = buildRequest(url, contentType);
        return loadStream(request, attemptLimit);
    }

    private InputStream loadStream(HttpRequest request, int attemptsLeft) throws HttpRequestException {
        try {
            return loadStream(request);
        } catch (StatusCodeException | IOException e) {
            attemptsLeft--;

            if (attemptsLeft == 0)
                throw new HttpRequestException("Last attempt error :: " + e.getMessage());

            return loadStream(request, attemptsLeft);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpRequestException("Request interrupted :: " + e.getMessage());
        }
    }

    private InputStream loadStream(HttpRequest request)
            throws InterruptedException, IOException, StatusCodeException {

        if (rateLimiter != null)
            rateLimiter.acquirePermit();

        var responseType = HttpResponse.BodyHandlers.ofInputStream();
        HttpResponse<InputStream> response = client.send(request, responseType);

        int statusCode = response.statusCode();
        statusCodeHandler.accept(statusCode);

        if (statusCode != 200)
            throw new StatusCodeException("Invalid HTTP code: " + statusCode);

        return extractStream(response);
    }

    private InputStream extractStream(HttpResponse<InputStream> response) throws IOException {
        if (rateLimiter != null)
            rateLimiter.resetBackoff();

        Optional<String> encoding = response.headers().firstValue("Content-Encoding");
        InputStream stream = response.body();

        if (encoding.isEmpty())
            return stream;

        return switch (encoding.get()) {
            case "gzip" -> new GZIPInputStream(stream);
            case "deflate" -> new DeflaterInputStream(stream);
            default -> throw new UnsupportedEncodingException();
        };
    }

}
