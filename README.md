# SimpleWebServer

## Implementation

1. ### A local hosted web server only support http get and head request, with static html file serving only.
2. ### Multi-thread implementation with blocking io and thread pool (ThreadPoolExecutor).
    * main thread listen on the configured port;
    * once a connection is accepted, pass the socket to a working thread from the thread pool;
    * for the same socket, request parsing and response generating are synchronous (processed by the same thread).
    * close the connection after sending the response (no keep-alive)
3. ### Http/1.1 Keep-alive behavior with non-blocking io and thread pool (ThreadPoolExecutor).
    * use java nio selector (register key) to have control on three tasks: accept, read, write;
    * use main thread to accept connection, use multiple threads to read and write asynchronously. 
    * keep alive behavior:
        - since http/1.1 defined keep-alive connection as default, so the server only turns off keep-alive behavior when receives a "Connection: close" header.
        - in addition, the server closes a channel after the number of request sending on this channel exceeds a threshold.
        - finally, if a tcp connection stay in a idle state longer than the keep-alive timeout, the connection will be closed.
    * concurrent processing request and response:
        - request and response are handled asynchronously.
        - There are no blocking mechanism between different requests and responses (both from different connections and in the same connection).
        - however, the order of requests and response in the same connection must be handled correctly. For example, even though the a later request might finish parsing before an earlier request, or a later response finish generating before an earlier response; the client should receive the response exactly in the same order of the sending requests.
              
## How to run and test the Server

1. Start webserver with blocking io, no keep-alive behavior.
   * BioWebServer.main()
2. Start webserver with non-blocking io, with keep-alive behavior.
   * NioWebServer.main()
3. Test from the browser
   * localhost:8080/index.html
   * localhost:8080/images.html
4. Manually testing by using binary tools (e.g, telnet) and adding sleep in reading process and writing process. (proper unit test should be done if there is more time) 

## TODO

1. Testing multi-thread performance with unit tests.
2. Testing keep-alive behavior with unit tests.
2. refactoring and cleaning up.
3. improve exception handling.

## Notice

This repository contains pieces of source code that is Copyright (c) 2020 CoderFromScratch (https://github.com/CoderFromScratch/simple-java-http-server), mainly the http request line parsing handling part. 
Beyond that, I contribute to the missing request header parsing and response generating parts. However, my main work was focusing on designing and building the bio and nio multi-threads server, as well as the HTTP\1.1 keep-alive implementation.
It is my first time working on web programing from a lower level, I will be grateful if anyone would like to have a discussion or pointing the issues in my implementation. 

