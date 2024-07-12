package com.alexkouzel.client.exceptions;

public class RequestFailedException extends Exception {

    public RequestFailedException(String message) {
        super(message);
    }

    public RequestFailedException() {
    }

}
