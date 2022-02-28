package com.fengcaiwen.webserver.http;

import com.fengcaiwen.webserver.util.Configuration;
import com.fengcaiwen.webserver.util.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

/**
 * assuming we only generate response for get request.
 * TODO file path handling might be improper now, Use URLConnection.guessContentTypeFromStream() at the moment.
 */
public class HttpResponse {

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpResponse.class);

    private static final int BUFFER_SIZE = 65535;

    private String requestTarget;
    private HttpStatusCode statusCode;
    private String contentType;
    private long contentLength;
    private boolean isKeepAlive;
    private int requestCount;
    private SocketChannel channel;

    private static final int SP = 0x20;
    private static final int CR = 0x0D;
    private static final int LF = 0x0A;

    public HttpResponse(String requestTarget) {
        this.requestTarget = requestTarget;
    }

    public HttpResponse(String requestTarget, boolean isKeepAlive, int requestCount) {
        this.requestTarget = requestTarget;
        this.isKeepAlive = isKeepAlive;
        this.requestCount = requestCount;
    }

    public void generate(OutputStream outputStream) throws Exception {
        File file = requestTarget.endsWith("/")?
                new File(Configuration.ROOT + "index.html") :
                new File(Configuration.ROOT + requestTarget);
        BufferedOutputStream bos = new BufferedOutputStream(outputStream);
        if (file.exists()) {
            contentLength = file.length();
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            contentType = URLConnection.guessContentTypeFromStream(bis);

            byte[] headerBytes = createHeaderBytes("HTTP/1.1 200 OK", contentLength, contentType, isKeepAlive);
            bos.write(headerBytes);

            byte[] buf = new byte[BUFFER_SIZE];
            int blockLen;
            while ((blockLen = bis.read(buf)) != -1) {
                bos.write(buf, 0, blockLen);
            }
            bis.close();

        } else {
            LOGGER.info("return 404 not found");
            byte[] headerBytes = createHeaderBytes("HTTP/1.0 404 Not Found", -1, null, isKeepAlive);
            bos.write(headerBytes);
        }
        bos.flush();
        bos.close();

    }

    public void generate() throws Exception {
        File file = requestTarget.endsWith("/")?
                new File(Configuration.ROOT + "index.html") :
                new File(Configuration.ROOT + requestTarget);
        Queue<ByteBuffer> writeQueue = new LinkedList<>();
        if (file.exists()) {
            contentLength = file.length();
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            contentType = URLConnection.guessContentTypeFromStream(bis);

            byte[] headerBytes = createHeaderBytes("HTTP/1.1 200 OK", contentLength, contentType, isKeepAlive);
            writeQueue.add(ByteBuffer.wrap(headerBytes));

            byte[] buf = new byte[BUFFER_SIZE];
            int blockLen;
            while ((blockLen = bis.read(buf)) != -1) {
                writeQueue.add(ByteBuffer.wrap(buf, 0, blockLen));
            }
            bis.close();

        } else {
            LOGGER.info("return 404 not found");
            byte[] headerBytes = createHeaderBytes("HTTP/1.1 404 Not Found", -1, null, isKeepAlive);
            writeQueue.add(ByteBuffer.wrap(headerBytes));
        }
        ConcurrentUtils.writeQueueMap.get(channel).put(requestCount, writeQueue);
        LOGGER.info("response count:" + ConcurrentUtils.requestAndResponseCountMap.get(channel)[1]);
        if (ConcurrentUtils.requestAndResponseCountMap.get(channel)[1] == requestCount - 1){
            int i = requestCount;
            while (ConcurrentUtils.writeQueueMap.get(channel).containsKey(i)){
                while(!ConcurrentUtils.writeQueueMap.get(channel).get(i).isEmpty()) {
                    channel.write(ConcurrentUtils.writeQueueMap.get(channel).get(i).poll());
                }
                i ++;
            }

            synchronized (this) {
                ConcurrentUtils.requestAndResponseCountMap.get(channel)[1] = i - 1;
            }
            LOGGER.info("response count:" + ConcurrentUtils.requestAndResponseCountMap.get(channel)[1]);
        } else {
            return;
        }
        if (!isKeepAlive || ConcurrentUtils.requestAndResponseCountMap.get(channel)[1] >= 10) {
            ConcurrentUtils.requestAndResponseCountMap.remove(channel);
            ConcurrentUtils.writeQueueMap.remove(channel);
            ConcurrentUtils.requestAndResponseCountMap.remove(channel);
            ConcurrentUtils.lastActiveTimeMap.remove(channel);
            channel.close();
        }

    }

    private byte[] createHeaderBytes(String content, long length, String contentType, boolean isKeepAlive) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(baos));
        bw.write(content + "\r\n");
        if (length >= 0) {
            bw.write("Content-Length: " + length + "\r\n");
        }
        if (contentType != null) {
            bw.write("Content-Type: " + contentType + "\r\n");
        }
        if (!isKeepAlive) {
            bw.write("Connection: " + "close" + "\r\n");
        }
        bw.write("\r\n");
        bw.flush();
        byte[] data = baos.toByteArray();
        bw.close();
        return data;
    }


    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public boolean isKeepAlive() {
        return isKeepAlive;
    }


}
