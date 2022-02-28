package com.fengcaiwen.webserver;

import com.fengcaiwen.webserver.core.nio.NioHttpServer;
import com.fengcaiwen.webserver.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 *
 * Driver Class for the Http Server with non-blocking io.
 *
 */

public class NioWebServer {

    private final static Logger LOGGER = LoggerFactory.getLogger(NioHttpServer.class);

    public static void main(String[] args) {

        LOGGER.info("Server started ...");
        LOGGER.info("Listen on port: " + Configuration.PORT);

        try {
            NioHttpServer server = new NioHttpServer(Configuration.PORT);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
