package com.fengcaiwen.webserver.core.nio;

import com.fengcaiwen.webserver.http.HttpRequest;
import com.fengcaiwen.webserver.http.HttpResponse;
import com.fengcaiwen.webserver.util.Configuration;
import com.fengcaiwen.webserver.util.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 *  Implemented Http Server using java nio with default HTTP/1.1 keep-alive behavior.
 *
 */

public class NioHttpServer {

    private final static Logger LOGGER = LoggerFactory.getLogger(NioHttpServer.class);

    private int port;

    private Selector selector;

    public NioHttpServer(int port) {
        this.port = port;
    }

    /**
     *  initialize key nio components, start listening to the configured port.
     */
    public void init() {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(this.port));
            this.selector = Selector.open();
            serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
            LOGGER.info("Nio Http Listener initialized ... ");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Main thread running
     * @throws IOException
     */
    public void start() throws IOException {
        init();
        cleanupTimedOutChannels();
        while (true) {
            int events = this.selector.select();
            LOGGER.debug("events: {}", events);
            if (events>0) {
                Iterator<SelectionKey> iterator = this.selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        LOGGER.info("accept connection ... ");
                        accept(key);
                    }
                    else if (key.isReadable()){
                        read(key);
                    }
                    else if (key.isWritable()) {
                        write(key);
                    }
                }
            }
        }
    }

    public void accept(SelectionKey key) {
        try {
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel sc = server.accept();
            sc.configureBlocking(false);
            sc.register(key.selector(), SelectionKey.OP_READ);
            ConcurrentUtils.lastActiveTimeMap.put(key, System.currentTimeMillis());
            LOGGER.info("accept a client : " + sc.socket().getInetAddress().getHostAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * non-blocking reading request
     * @param key
     */
    private void read(SelectionKey key) {

        final SocketChannel sc = (SocketChannel) key.channel();
        final Selector s = key.selector();

        // initialize some util maps to store request ordering information
        if (!ConcurrentUtils.requestAndResponseCountMap.containsKey(sc)) {
            ConcurrentUtils.requestAndResponseCountMap.put(sc, new int[]{1, 0});
            LinkedBlockingQueue<ConcurrentUtils.RequestWithId> readQueue = new LinkedBlockingQueue<>();
            ConcurrentUtils.readQueueMap.put(sc, readQueue);
        } else {
            ConcurrentUtils.requestAndResponseCountMap.get(sc)[0]++;
        }
        int requestCount = ConcurrentUtils.requestAndResponseCountMap.get(sc)[0];

        LOGGER.info("request count:" + ConcurrentUtils.requestAndResponseCountMap.get(sc)[0]);
        ConcurrentUtils.lastActiveTimeMap.put(key, System.currentTimeMillis());

        //prevent meaningless reading event while the working thread is parsing the request
        key.cancel();

        //invoke another thread to read the request, the main thread would not be blocked.
        ConcurrentUtils.pool.submit(() -> {

            HttpRequest request = new HttpRequest();

            //TODO parsing the buffer data directly to avoid unnecessary format converting
            try {
                String reqMsg = "";
                ByteBuffer receivingBuffer = ByteBuffer.allocate(2048);
                int bytesRead = sc.read(receivingBuffer);
                if (bytesRead > 0) {
                    receivingBuffer.flip();
                    byte[] array = new byte[receivingBuffer.limit()];
                    receivingBuffer.get(array);
                    reqMsg = new String(array);
                    LOGGER.info("Server received request. ");
                }
                if (!reqMsg.isEmpty()) {
                    request.parse(new ByteArrayInputStream(reqMsg.getBytes()));
                }
            } catch (Exception e) {
                try {
                    sc.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                key.cancel();
                LOGGER.error(e.getMessage());
                return;
            }

            // result of bad requests.
            // TODO should be handled in a lower level;
            if (request.getMethod() == null) {
                try {
                    key.channel().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                key.cancel();
                return;
            }
            LOGGER.info("get request headline: {} {}", request.getMethod().name(), request.getRequestTarget());

            ConcurrentUtils.readQueueMap.get(sc).add(new ConcurrentUtils.RequestWithId(request, requestCount));

            // "re-invoke" previous canceled key.
            try {
                s.wakeup();
                sc.register(s, SelectionKey.OP_WRITE, System.currentTimeMillis());
                ConcurrentUtils.lastActiveTimeMap.put(key, System.currentTimeMillis());
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * non-blocking writing response.
     * @param key
     */
    private void write(SelectionKey key) {
        final SocketChannel sc = (SocketChannel) key.channel();
        final ConcurrentUtils.RequestWithId requestWithId = ConcurrentUtils.readQueueMap.get(sc).poll();
        assert requestWithId != null;
        int requestCount = requestWithId.requestId;
        HttpRequest request = requestWithId.request;

        // initialize some util maps to store response ordering information
        if (!ConcurrentUtils.writeQueueMap.containsKey(sc)) {
            Map<Integer, Queue<ByteBuffer>> writeQueueArr = new HashMap<>();
            ConcurrentUtils.writeQueueMap.put(sc, writeQueueArr);
        }

        if (request != null) {
            boolean isKeepAlive = true;
            if (request.getRequestHeader().containsKey("Connection")){
                isKeepAlive = !request.getRequestHeader().get("Connection").toLowerCase().equals("close");
            }
            HttpResponse response = new HttpResponse(request.getRequestTarget(), isKeepAlive, requestCount);
            response.setChannel(sc);

            //invoke another thread to read the request, the main thread would not be blocked.
            ConcurrentUtils.pool.submit(() -> {
                try {
                    response.generate();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                    return;
                }
            });

            if (isKeepAlive && requestCount < Configuration.KEEP_ALIVE_MAX_REQUESTS) {
                key.interestOps(SelectionKey.OP_READ);
                ConcurrentUtils.lastActiveTimeMap.put(key, System.currentTimeMillis());
            } else {
                key.cancel();
            }
        }
        else {
            key.interestOps(SelectionKey.OP_READ);
            ConcurrentUtils.lastActiveTimeMap.put(key, System.currentTimeMillis());
        }
    }

    // invoke another keep-running thread to monitor idle connections and close them.
    private void cleanupTimedOutChannels() {
        new Thread(() -> {
            while (true) {
                long now = System.currentTimeMillis();
                for (SelectionKey key : selector.keys()) {
                    synchronized (this) {
                        if (ConcurrentUtils.lastActiveTimeMap.containsKey(key)
                                && ConcurrentUtils.lastActiveTimeMap.get(key) + Configuration.KEEP_ALIVE_TIMEOUT < now) {
                            try {
                                SocketChannel sc = (SocketChannel)key.channel();
                                sc.close();
                                key.cancel();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (ClassCastException ignored) {
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

}
