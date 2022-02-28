package com.fengcaiwen.webserver.util;

/**
 *
 * basic server configuration.
 * Keep-alive setting is only enable for nio server.
 *
 */

public abstract class Configuration {

    public static final int PORT = 8080;
    public static final String ROOT = "tmp/";

    public static final int KEEP_ALIVE_MAX_REQUESTS = 10;
    public static final long KEEP_ALIVE_TIMEOUT = 10000L;

}
