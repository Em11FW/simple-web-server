package com.fengcaiwen.webserver.core.bio;

import com.fengcaiwen.webserver.http.HttpRequest;
import com.fengcaiwen.webserver.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class HttpWorker implements Runnable{

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpWorker.class);
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    public HttpWorker(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            HttpRequest request = new HttpRequest(inputStream);
            HttpResponse response = new HttpResponse(request.getRequestTarget());
            response.generate(outputStream);

        } catch (Exception e) {
            LOGGER.error("Problem with processing socket {}", e.getMessage());
        } finally {
            try {
                inputStream.close();
                outputStream.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
