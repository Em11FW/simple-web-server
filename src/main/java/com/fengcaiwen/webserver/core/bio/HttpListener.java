package com.fengcaiwen.webserver.core.bio;

import com.fengcaiwen.webserver.util.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class HttpListener extends Thread {

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpListener.class);

    private int port;
    private String webroot;
    private ServerSocket serverSocket;
    private final ExecutorService executorService;

    public HttpListener(int port, String webroot) throws IOException {
        this.port = port;
        this.webroot = webroot;
        serverSocket = new ServerSocket(this.port);
        executorService = ConcurrentUtils.pool;
    }


    @Override
    public void run() {
        try {

            while ( serverSocket.isBound() && !serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();

                LOGGER.info(" * Connection accepted: " + socket.getInetAddress().getHostAddress());

                executorService.submit(new HttpWorker(socket));

            }

        } catch (IOException e) {
            LOGGER.error("problem with accepting TCP connection", e);
        } finally {
            try {
                if (serverSocket != null){
                    serverSocket.close();
                }
            } catch (IOException e) {}
        }
    }
}
