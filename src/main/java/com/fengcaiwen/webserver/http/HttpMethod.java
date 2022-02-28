package com.fengcaiwen.webserver.http;

public enum HttpMethod {
    GET, HEAD;

    public static final int MAX_LENGTH;

    static {
        int tempMaxLength = -1;
        for (HttpMethod method : values()){
            tempMaxLength = Math.max(method.name().length(), tempMaxLength);
        }
        MAX_LENGTH = tempMaxLength;
    }
}
