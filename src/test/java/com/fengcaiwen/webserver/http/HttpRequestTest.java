package com.fengcaiwen.webserver.http;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpRequestTest {

    private HttpRequest httpRequest;

    @Test
    void parseHttpRequest() {
        try {
            httpRequest = new HttpRequest(generateValidRequest());
        } catch (HttpRequestException e) {
            fail(e);
        }

        assertNotNull(httpRequest);
        assertEquals(HttpMethod.GET, httpRequest.getMethod());
        assertEquals("/", httpRequest.getRequestTarget());
        assertEquals(HttpVersion.HTTP_1_1, httpRequest.getBestCompatibleHttpVersion());
        assertEquals("HTTP/1.1", httpRequest.getOriginalHttpVersion());
    }

    @Test
    void parseHttpBadRequestMethod() {
        try {
            httpRequest = new HttpRequest(generateBadRequestMethod());
            fail();
        } catch (HttpRequestException e) {
            assertEquals(HttpStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED, e.getErrorCode());
        }
    }

    @Test
    void parseHttpLongRequestMethod() {
        try {
            httpRequest = new HttpRequest(generateLongRequestMethod());;
            fail();
        } catch (HttpRequestException e) {
            assertEquals(HttpStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED, e.getErrorCode());
        }


    }

    @Test
    void parseHttpBadRequestLineInvNumItems() {
        try {
            httpRequest = new HttpRequest(generateBadRequestLineInvNumItems());;
            fail();
        } catch (HttpRequestException e) {
            assertEquals(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST, e.getErrorCode());
        }


    }

    @Test
    void parseHttpEmptyRequestLineOnlyCRLF() {
        try {
            httpRequest = new HttpRequest(generateEmptyRequestLineOnlyCRLF());;
            fail();
        } catch (HttpRequestException e) {
            assertEquals(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST, e.getErrorCode());
        }
    }

    @Test
    void parseHttpBadRequestLineOnlyCRnoLF() {
        try {
            httpRequest = new HttpRequest(generateBadRequestLineOnlyCRnoLF());
            fail();
        } catch (HttpRequestException e) {
            assertEquals(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST, e.getErrorCode());
        }
    }

    @Test
    void parseBadHttpVersion() {
        try {
            httpRequest = new HttpRequest(generateBadHttpVersion());
            fail();
        } catch (HttpRequestException e) {
            assertEquals(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST, e.getErrorCode());
        }
    }

    @Test
    void parseUnsupportedHttpVersion() {
        try {
            httpRequest = new HttpRequest(generateUnsupportedHttpVersion());
            fail();
        } catch (HttpRequestException e) {
            assertEquals(HttpStatusCode.SERVER_ERROR_505_HTTP_VERSION_NOT_SUPPORTED, e.getErrorCode());
        }
    }

    @Test
    void parseSupportedHttpVersion() {
        try {
            httpRequest = new HttpRequest(generateSupportedHttpVersion());
            assertNotNull(httpRequest);
            assertEquals(HttpVersion.HTTP_1_1, httpRequest.getBestCompatibleHttpVersion());
            assertEquals("HTTP/1.2", httpRequest.getOriginalHttpVersion());
        } catch (HttpRequestException e) {
            fail();
        }
    }

    private InputStream generateValidRequest() {
        String rawData = "GET / HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Connection: keep-alive\r\n" +
                "Cache-Control: max-age=0\r\n" +
                "sec-ch-ua: \"Opera\";v=\"83\", \"Chromium\";v=\"97\", \";Not A Brand\";v=\"99\"\r\n" +
                "sec-ch-ua-mobile: ?0\r\n" +
                "sec-ch-ua-platform: \"macOS\"\r\n" +
                "Upgrade-Insecure-Requests: 1\r\n" +
                "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.99 Safari/537.36 OPR/83.0.4254.27\r\n" +
                "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9\r\n" +
                "Sec-Fetch-Site: none\r\n" +
                "Sec-Fetch-Mode: navigate\r\n" +
                "Sec-Fetch-User: ?1\r\n" +
                "Sec-Fetch-Dest: document\r\n" +
                "Accept-Encoding: gzip, deflate, br\r\n" +
                "Accept-Language: en-US,en;q=0.9\r\n" +
                "\r\n";
        InputStream inputStream = new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
        return inputStream;
    }

    private InputStream generateBadRequestMethod() {
        String rawData = "POST / HTTP/1.1\r\n" + "\r\n";
        InputStream inputStream = new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
        return inputStream;
    }

    private InputStream generateLongRequestMethod() {
        String rawData = "MEANINGLESSMETHOD / HTTP/1.1\r\n" + "\r\n";
        InputStream inputStream = new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
        return inputStream;
    }

    private InputStream generateBadRequestLineInvNumItems() {
        String rawData = "GET / HTTP/1.1 INVALID\r\n" + "\r\n";
        InputStream inputStream = new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
        return inputStream;
    }

    private InputStream generateEmptyRequestLineOnlyCRLF() {
        String rawData = "\r\n" + "\r\n";
        InputStream inputStream = new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
        return inputStream;
    }

    private InputStream generateBadRequestLineOnlyCRnoLF() {
        String rawData = "GET / HTTP/1.1\r" +
                "Host: localhost:8080\r\n" +
                "Connection: keep-alive\r\n";
        InputStream inputStream = new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
        return inputStream;
    }

    private InputStream generateBadHttpVersion() {
        String rawData = "GET / HTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Connection: keep-alive\r\n";
        InputStream inputStream = new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
        return inputStream;
    }

    private InputStream generateUnsupportedHttpVersion() {
        String rawData = "GET / HTTP/2.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Connection: keep-alive\r\n";
        InputStream inputStream = new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
        return inputStream;
    }

    private InputStream generateSupportedHttpVersion() {
        String rawData = "GET / HTTP/1.2\r\n" +
                "Host: localhost:8080\r\n" +
                "Connection: keep-alive\r\n";
        InputStream inputStream = new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
        return inputStream;
    }
}