package com.alexkouzel.client.exceptions;

public class HttpRequestException extends Exception {

    public HttpRequestException(String message) {
        super(message);
    }

    public HttpRequestException() {
    }
}
