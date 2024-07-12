package com.alexkouzel.client;

import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.client.exceptions.RequestFailedException;
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

public abstract class DataClient {

    private final int attemptLimit;

    private final Consumer<Integer> errorHandler;

    protected final RateLimiter rateLimiter;

    private final ObjectMapper jsonMapper;

    private final XmlMapper xmlMapper;

    private final HttpClient client;

    public DataClient(
            int attemptLimit,
            Consumer<Integer> errorHandler,
            RateLimiter rateLimiter
    ) {
        this.attemptLimit = attemptLimit;
        this.errorHandler = errorHandler;
        this.rateLimiter = rateLimiter;
        this.jsonMapper = new ObjectMapper();
        this.xmlMapper = new XmlMapper();
        this.client = HttpClient.newHttpClient();

        JavaTimeModule timeModule = new JavaTimeModule();
        jsonMapper.registerModule(timeModule);
        xmlMapper.registerModule(timeModule);
    }

    protected abstract HttpRequest buildRequest(String url, String contentType);

    public <T> T loadJson(String url, Class<T> type) throws ParsingException, RequestFailedException {
        return loadType(url, type, jsonMapper);
    }

    public <T> T loadXml(String url, Class<T> type) throws ParsingException, RequestFailedException {
        return loadType(url, type, xmlMapper);
    }

    public <T> T loadType(String url, Class<T> type, ObjectMapper mapper)
            throws ParsingException, RequestFailedException {
        try {
            return mapper.readValue(loadText(url), type);
        } catch (JsonProcessingException e) {
            throw new ParsingException();
        }
    }

    public String loadText(String url) throws RequestFailedException, ParsingException {
        try (InputStream stream = loadStream(url, "text/html")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ParsingException();
        }
    }

    public InputStream loadStream(String url, String contentType) throws RequestFailedException {
        HttpRequest request = buildRequest(url, contentType);
        return loadStream(request, attemptLimit);
    }

    private InputStream loadStream(HttpRequest request, int attemptsLeft) throws RequestFailedException {
        if (attemptsLeft <= 0) {
            throw new RequestFailedException();
        }
        try {
            return loadStream(request);
        } catch (IOException | InterruptedException | StatusCodeException e) {
            return loadStream(request, attemptsLeft - 1);
        }
    }

    private InputStream loadStream(HttpRequest request)
            throws IOException, InterruptedException, StatusCodeException {

        if (rateLimiter != null)
            rateLimiter.acquirePermit();

        var responseType = HttpResponse.BodyHandlers.ofInputStream();
        HttpResponse<InputStream> response = client.send(request, responseType);

        if (response.statusCode() != 200) {
            errorHandler.accept(response.statusCode());
            throw new StatusCodeException();
        }
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
