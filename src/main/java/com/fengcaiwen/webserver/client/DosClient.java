package com.fengcaiwen.webserver.client;

import java.io.IOException;
import java.net.Socket;

/**
 * This class is only for testing purpose.
 * Run this main method after starting the server.
 * TODO Need to be moved to unit test later.
 * TODO more client classes need to be done for testing purpose.
 */
public class DosClient {

    public static void main (String[] args) {
        for (int i = 0; i < 3000; i++) {
            try {
                new Socket("localhost", 8080);
                System.out.println(i);
            } catch (IOException e) {
                System.err.println("could not connect ..." + e);
            }
        }
    }
}
