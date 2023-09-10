package com.hawolt.http.proxy;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created: 30/07/2022 16:33
 * Author: Twitter @hawolt
 **/

public class ProxyResponse implements IRequest {
    private final Map<String, List<String>> headers;
    private final GenerifiedResponse response;
    private final ProxyRequest original;
    private byte[] body;
    private int code;

    public ProxyResponse(ProxyRequest request, GenerifiedResponse response) {
        this.headers = response.getHeaders();
        this.code = response.getCode();
        this.body = response.getBody();
        this.response = response;
        this.original = request;
    }

    public GenerifiedResponse getGenerifiedResponse() {
        return response;
    }

    @Override
    public void removeHeader(String header) {
        headers.remove(header);
    }

    @Override
    public void addHeader(String k, String v) {
        headers.put(k, Collections.singletonList(v));
    }

    public ProxyRequest getOriginal() {
        return original;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public byte[] getByteBody() {
        return body;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    @Override
    public String getBody() {
        return new String(body, StandardCharsets.UTF_8);
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public void setBody(String in) {
        this.body = in.getBytes();
    }

    @Override
    public String url() {
        return original.url();
    }

    @Override
    public String method() {
        return original.method();
    }

}
