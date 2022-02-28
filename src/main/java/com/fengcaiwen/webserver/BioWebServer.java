package com.fengcaiwen.webserver;

import com.fengcaiwen.webserver.core.bio.HttpListener;
import com.fengcaiwen.webserver.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 *
 * Driver Class for the Http Server with blocking io.
 *
 */

public class BioWebServer {

    private final static Logger LOGGER = LoggerFactory.getLogger(BioWebServer.class);

    public static void main (String[] args) {

        LOGGER.info("Server started ...");
        LOGGER.info("Listen on port: " + Configuration.PORT);

        try {
            HttpListener httpListener = new HttpListener(Configuration.PORT, Configuration.ROOT);
            httpListener.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
