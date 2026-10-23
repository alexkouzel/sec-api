package com.alexkouzel.client;

import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.testing.LocalHttpServer;
import com.alexkouzel.testing.LocalHttpServer.Request;
import com.alexkouzel.testing.LocalHttpServer.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EdgarClientTest {
    private static final String USER_AGENT = "Test Company test@example.com";
    private static final Duration BASE_RETRY_DELAY = Duration.ofMillis(100);

    private final List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());
    private LocalHttpServer server;
    private EdgarClient client;

    public record Item(String name) {
    }

    @BeforeEach
    public void setUp() throws Exception {
        server = new LocalHttpServer();
        // Short base delay keeps retry tests fast; the cap still allows Retry-After: 2.
        client = new EdgarClient(USER_AGENT, statusCodes::add, BASE_RETRY_DELAY, Duration.ofSeconds(5));
    }

    @AfterEach
    public void tearDown() {
        server.close();
    }

    private static Duration gap(Request first, Request second) {
        return Duration.ofNanos(second.nanoTime() - first.nanoTime());
    }

    @Test
    public void identifiesItselfAndAcceptsCompression() throws Exception {
        server.enqueue(Response.ok("hello"));

        client.loadText(server.url("/a"));

        Request request = server.requests().getFirst();
        assertEquals(USER_AGENT, request.headers().getFirst("User-Agent"));
        assertEquals("gzip, deflate", request.headers().getFirst("Accept-Encoding"));
    }

    @Test
    public void returnsPlainBody() throws Exception {
        server.enqueue(Response.ok("plain body"));

        assertEquals("plain body", client.loadText(server.url("/a")));
        assertEquals(List.of(200), statusCodes);
    }

    @Test
    public void decodesGzipBody() throws Exception {
        server.enqueue(Response.gzip("zipped body"));

        assertEquals("zipped body", client.loadText(server.url("/a")));
    }

    @Test
    public void mapsJson() throws Exception {
        server.enqueue(Response.ok("{\"name\": \"x\", \"addedLater\": true}"));

        assertEquals(new Item("x"), client.loadJson(server.url("/a"), Item.class));
    }

    @Test
    public void mapsXml() throws Exception {
        server.enqueue(Response.ok("<item><name>x</name><addedLater>1</addedLater></item>"));

        assertEquals(new Item("x"), client.loadXml(server.url("/a"), Item.class));
    }

    @Test
    public void retriesServerErrorThenSucceeds() throws Exception {
        server.enqueue(Response.status(500, "oops"), Response.ok("done"));

        assertEquals("done", client.loadText(server.url("/a")));
        assertEquals(2, server.requests().size());
        assertEquals(List.of(500, 200), statusCodes);
    }

    @Test
    public void givesUpAfterThreeAttempts() {
        server.enqueue(Response.status(503, "down"));

        assertThrows(HttpRequestException.class, () -> client.loadText(server.url("/a")));
        assertEquals(3, server.requests().size());
    }

    @Test
    public void retriesDroppedConnection() throws Exception {
        server.enqueue(Response.dropped(), Response.ok("done"));

        assertEquals("done", client.loadText(server.url("/a")));
    }

    @Test
    public void staysUnderTenRequestsPerSecond() throws Exception {
        server.enqueue(Response.ok("x"));

        for (int i = 0; i < 25; i++) {
            client.loadText(server.url("/" + i));
        }

        List<Request> requests = server.requests();
        assertEquals(25, requests.size());
        for (int i = 10; i < requests.size(); i++) {
            Duration window = gap(requests.get(i - 10), requests.get(i));
            assertTrue(window.toMillis() >= 900, "11 requests within " + window.toMillis() + " ms");
        }
    }

    @Test
    public void decodesDeflateBody() throws Exception {
        server.enqueue(Response.deflate("deflated body"));

        assertEquals("deflated body", client.loadText(server.url("/a")));
    }

    @Test
    public void decodesRawDeflateBody() throws Exception {
        server.enqueue(Response.rawDeflate("raw deflated body"));

        assertEquals("raw deflated body", client.loadText(server.url("/a")));
    }

    @Test
    @Timeout(20)
    public void recoversFromTooManyRequestsWithoutHanging() throws Exception {
        server.enqueue(Response.status(429, "slow down"), Response.ok("done"));

        assertEquals("done", client.loadText(server.url("/a")));
        assertEquals(List.of(429, 200), statusCodes);
    }

    @Test
    public void waitsForRetryAfterOnTooManyRequests() throws Exception {
        server.enqueue(Response.status(429, "slow down").withHeader("Retry-After", "2"), Response.ok("done"));

        client.loadText(server.url("/a"));

        List<Request> requests = server.requests();
        Duration gap = gap(requests.get(0), requests.get(1));
        assertTrue(gap.toMillis() >= 1900, "retried after " + gap.toMillis() + " ms");
    }

    @Test
    @Timeout(20)
    public void capsRetryAfter() throws Exception {
        try (EdgarClient capped = new EdgarClient(USER_AGENT, statusCodes::add,
                Duration.ofMillis(100), Duration.ofMillis(500))) {
            server.enqueue(Response.status(429, "slow down").withHeader("Retry-After", "3600"), Response.ok("done"));

            assertEquals("done", capped.loadText(server.url("/a")));
            Duration gap = gap(server.requests().get(0), server.requests().get(1));
            assertTrue(gap.toMillis() >= 450 && gap.toMillis() < 5000, "retried after " + gap.toMillis() + " ms");
        }
    }

    @Test
    public void backsOffBeforeRetryingDroppedConnection() throws Exception {
        server.enqueue(Response.dropped(), Response.dropped(), Response.ok("done"));

        assertEquals("done", client.loadText(server.url("/a")));

        // The JDK client silently retries a dropped connection once itself, so the two
        // drops are one attempt from our side; our retry comes after the base delay.
        List<Request> requests = server.requests();
        assertEquals(3, requests.size());
        Duration gap = gap(requests.get(1), requests.get(2));
        assertTrue(gap.toMillis() >= BASE_RETRY_DELAY.toMillis() - 10, "retried after " + gap.toMillis() + " ms");
    }

    @Test
    public void doesNotRetryClientErrors() {
        server.enqueue(Response.status(404, "missing"));

        HttpRequestException error = assertThrows(HttpRequestException.class, () -> client.loadText(server.url("/a")));
        assertTrue(error.getMessage().contains("404"), error.getMessage());
        assertEquals(1, server.requests().size());
    }

    @Test
    public void reportsInvalidJsonAsParsingError() {
        server.enqueue(Response.ok("not json"));

        assertThrows(ParsingException.class, () -> client.loadJson(server.url("/a"), Map.class));
    }

    @Test
    public void failsRequestsAfterClose() {
        server.enqueue(Response.ok("x"));
        client.close();

        assertThrows(HttpRequestException.class, () -> client.loadText(server.url("/a")));
    }
}
