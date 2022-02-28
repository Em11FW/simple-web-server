package com.fengcaiwen.webserver.http;

public class HttpRequestException extends Exception{

    private final HttpStatusCode errorCode;

    public HttpRequestException(HttpStatusCode errorCode) {
        super(errorCode.MESSAGE);
        this.errorCode = errorCode;
    }

    public HttpStatusCode getErrorCode() {
        return errorCode;
    }
}
