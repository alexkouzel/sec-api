package com.alexkouzel.testing;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Reads EDGAR responses captured under src/test/resources/edgar (see its README).
 */
public final class Fixtures {

    private Fixtures() {
    }

    public static InputStream stream(String path) {
        InputStream stream = Fixtures.class.getResourceAsStream("/edgar/" + path);
        if (stream == null) {
            throw new IllegalArgumentException("No fixture: edgar/" + path);
        }
        return stream;
    }

    public static String text(String path) {
        try (InputStream stream = stream(path)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] bytes(String path) {
        try (InputStream stream = stream(path)) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
