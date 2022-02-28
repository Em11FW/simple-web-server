package com.fengcaiwen.webserver.util;

import com.fengcaiwen.webserver.http.HttpRequest;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

/**
 *
 * some data collections to deal with multi-thread concurrency
 * and request and response ordering in the same TCP connection.
 *
 * TODO need to double check if the static maps lead to memory leak.
 *
 */

public class ConcurrentUtils {


    /**
     *  a map storing selection keys with their latest active time in order to handle keep-alive timeout.
     */
    public static ConcurrentHashMap<SelectionKey, Long> lastActiveTimeMap = new ConcurrentHashMap<>();

    /**
     *  a map storing active socket channels with processed requests with ordering information
     */
    public static ConcurrentHashMap<SocketChannel, LinkedBlockingQueue<RequestWithId>> readQueueMap = new ConcurrentHashMap<>();

    /**
     *  a map storing active socket channels with generated response with ordering information
     */
    public static ConcurrentHashMap<SocketChannel, Map<Integer, Queue<ByteBuffer>>> writeQueueMap = new ConcurrentHashMap<>();

    /**
     *  a map storing active socket channels with an array including real-time request and response counting
     */
    public static ConcurrentHashMap<SocketChannel, int[]> requestAndResponseCountMap = new ConcurrentHashMap<>();

    static ThreadFactory myThreadFactory = Thread::new;

    /**
     *  initialization of the thread pool
     */
    public static ExecutorService pool = new ThreadPoolExecutor(5, 10,
            1000L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1024), myThreadFactory, new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     *  a data class to keep track on request ordering information in the same TCP connection.
     */
    public static class RequestWithId {

        public HttpRequest request;

        public int requestId;

        public RequestWithId(HttpRequest request, int requestId) {
            this.request = request;
            this.requestId = requestId;
        }
    }
}
