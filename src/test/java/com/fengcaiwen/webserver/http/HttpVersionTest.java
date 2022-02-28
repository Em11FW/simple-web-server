package com.fengcaiwen.webserver.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpVersionTest {

    @Test
    void getBestCompatibleVersionExactMatch() {
        try {
            HttpVersion version = HttpVersion.getBestCompatibleVersion("HTTP/1.1");
            assertNotNull(version);
            assertEquals(HttpVersion.HTTP_1_1, version);
        } catch (BadHttpVersionException e) {
            fail();
        }
    }

    @Test
    void getBestCompatibleVersionBadFormat() {
        try {
            HttpVersion version = HttpVersion.getBestCompatibleVersion("http/1.1");
            fail();
        } catch (BadHttpVersionException e) {}
    }

    @Test
    void getBestCompatibleVersionHigherVersion() {
        try {
            HttpVersion version = HttpVersion.getBestCompatibleVersion("HTTP/1.2");
            assertNotNull(version);
            assertEquals(HttpVersion.HTTP_1_1, version);
        } catch (BadHttpVersionException e) {
            fail();
        }
    }
}