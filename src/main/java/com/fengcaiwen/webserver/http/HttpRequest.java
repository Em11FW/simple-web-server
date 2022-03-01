package com.fengcaiwen.webserver.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * byte-by-byte parsing the request line and request header.
 *
 */

public class HttpRequest {

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpRequest.class);

    private static final int SP = 0x20;
    private static final int CR = 0x0D;
    private static final int LF = 0x0A;

    private HttpMethod method;
    private String requestTarget;
    private String originalHttpVersion;
    private HttpVersion bestCompatibleHttpVersion;
    private Map<String, String> requestHeader;
    private HttpStatusCode badRequestCode;

    public HttpStatusCode getBadRequestCode() {
        return badRequestCode;
    }

    public void setBadRequestCode(HttpStatusCode badRequestCode) {
        this.badRequestCode = badRequestCode;
    }

    public HttpRequest() {
    }

    public Map<String, String> getRequestHeader() {
        return requestHeader;
    }

    void setRequestHeader(Map<String, String> requestHeader) {
        this.requestHeader = requestHeader;
    }

    public HttpMethod getMethod() {
        return method;
    }

    void setMethod(String methodName) throws HttpRequestException {
        for (HttpMethod method : HttpMethod.values()) {
            if (methodName.equals(method.name())) {
                this.method = method;
                return;
            }
        }
        throw new HttpRequestException(HttpStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED);
    }

    public String getRequestTarget() {
        return requestTarget;
    }

    void setRequestTarget(String requestTarget) throws HttpRequestException {
        if (requestTarget == null || requestTarget.length() == 0) {
            throw new HttpRequestException(HttpStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
        }
        this.requestTarget = requestTarget;
    }

    void setOriginalHttpVersion(String originalHttpVersion) throws BadHttpVersionException, HttpRequestException {
        this.originalHttpVersion = originalHttpVersion;
        this.bestCompatibleHttpVersion = HttpVersion.getBestCompatibleVersion(originalHttpVersion);
        if (this.bestCompatibleHttpVersion == null) {
            throw new HttpRequestException(HttpStatusCode.SERVER_ERROR_505_HTTP_VERSION_NOT_SUPPORTED);
        }
    }

    public String getOriginalHttpVersion() {
        return originalHttpVersion;
    }

    public HttpVersion getBestCompatibleHttpVersion() {
        return bestCompatibleHttpVersion;
    }

    public void parse(InputStream inputStream) throws HttpRequestException {
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.US_ASCII);
        try {
            parseRequestLine(reader);
            parseHeaders(reader);
            parseBody(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseBody(InputStreamReader reader) {
    }

    private void parseHeaders(InputStreamReader reader) throws IOException, HttpRequestException {
        Map<String, String> headers = new HashMap<>();
        StringBuilder fieldName = new StringBuilder();
        StringBuilder fieldValue = new StringBuilder();

        LOGGER.info("Start parsing request header ... ");

        boolean fieldNameParsed = false;
        boolean fieldValueParsed = false;

        int _byte;
        while ((_byte = reader.read()) >= 0) {
            if (_byte == CR) {
                _byte = reader.read();
                if (_byte == LF) {
                    if (fieldValueParsed){
                        setRequestHeader(headers);
                        return;
                    }
                    headers.put(fieldName.toString(), fieldValue.toString().trim());
                    //LOGGER.info("name: {} , value {}", fieldName.toString(), fieldValue.toString());
                    fieldName.delete(0, fieldName.length());
                    fieldValue.delete(0,fieldValue.length());
                    fieldNameParsed = false;
                    fieldValueParsed = true;
                } else {
                    throw new HttpRequestException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                }
            }

            else if (_byte == ':' && !fieldNameParsed) {
                fieldNameParsed = true;
                fieldValueParsed = false;
            } else {
                if (!fieldNameParsed) {
                    fieldName.append((char) _byte);
                }
                else {
                    fieldValue.append((char) _byte);
                }
            }
        }
        setRequestHeader(headers);
    }

    private void parseRequestLine(InputStreamReader reader) throws IOException, HttpRequestException {
        StringBuilder sb = new StringBuilder();

        boolean methodParsed = false;
        boolean requestTargetParsed = false;

        int _byte;
        while ((_byte = reader.read()) >= 0) {
            if (_byte == CR) {
                if (!requestTargetParsed) {
                    throw new HttpRequestException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                }
                _byte = reader.read();
                if (_byte == LF) {
                    LOGGER.debug("Request Line to Process : {}", sb.toString());
                    try {
                        setOriginalHttpVersion(sb.toString());
                    } catch (BadHttpVersionException e) {
                        throw new HttpRequestException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                    }
                    return; //exit parsing request line after the first encounter with CRLF
                } else {
                    throw new HttpRequestException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                }
            }

            if (_byte == SP) {

                if (!methodParsed) {
                    LOGGER.debug("Request Line to Process : {}", sb.toString());
                    setMethod(sb.toString());
                    methodParsed = true;
                } else if (!requestTargetParsed) {
                    LOGGER.debug("Request Line to Process : {}", sb.toString());
                    setRequestTarget(sb.toString());
                    requestTargetParsed = true;
                } else {
                    throw new HttpRequestException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                }
                sb.delete(0, sb.length());

            } else {
                sb.append((char) _byte);
                if (!methodParsed) {
                    if (sb.length() > HttpMethod.MAX_LENGTH) {
                        throw new HttpRequestException(HttpStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED);
                    }
                }
            }
        }
    }


}
