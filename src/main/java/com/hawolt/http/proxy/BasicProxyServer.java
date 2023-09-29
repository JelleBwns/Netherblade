package com.hawolt.http.proxy;

import com.hawolt.http.IRequestModifier;
import com.hawolt.logger.Logger;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.io.IOException;
import java.util.*;

/**
 * Created: 30/07/2022 15:48
 * Author: Twitter @hawolt
 **/

public class BasicProxyServer {
    private final Map<String, List<IRequestModifier>> map = new HashMap<>();

    private String target;

    public BasicProxyServer(int port, String target) {
        this.target = target;
        Javalin javalin = Javalin.create().start("127.0.0.1", port);
        javalin.options("*", this::forward);
        javalin.delete("*", this::forward);
        javalin.patch("*", this::forward);
        javalin.post("*", this::forward);
        javalin.head("*", this::forward);
        javalin.get("*", this::forward);
        javalin.put("*", this::forward);
        javalin.before("*", context -> {
            context.header("access-control-allow-origin", "*");
            context.header("access-control-expose-headers", "*");
            context.header("access-control-allow-methods", "*");
            context.header("access-control-allow-headers", "*");
        });
    }

    private void forward(Context context) {
        String method = context.method().toUpperCase();
        String query = context.queryString();
        String destination = String.format("%s%s%s", target, context.path(), query != null ? "?" + query : "");
        String body = context.body();
        ProxyRequest initial = new ProxyRequest(destination, method);
        for (String header : context.headerMap().keySet()) {
            if (header.equalsIgnoreCase("Host")) continue;
            initial.addHeader(header, context.headerMap().get(header));
        }
        initial.setBody(body);
        ProxyResponse response = null;
        if (map.containsKey(method)) {
            List<IRequestModifier> list = map.get(method);
            for (IRequestModifier modifier : list) {
                try {
                    initial = modifier.onBeforeRequest(initial);
                    response = modifier.onResponse(response == null ? new ProxyResponse(initial, initial.execute()) : response);
                } catch (Exception e) {
                    modifier.onException(e);
                    Logger.error(e);
                }
            }
        }
        if (response == null) {
            try {
                response = new ProxyResponse(initial, initial.execute());
            } catch (IOException e) {
                Logger.error(e);
            }
        }
        ProxyResponse complete = Objects.requireNonNull(response);
        context.status(complete.getCode());
        String type = context.header("Content-Type");
        if (type != null) context.header("Content-Type", type);
        //dont touch i dont know why it works this way
        //idk now it works without the special else-if so i removed the inner else case
        if (context.url().contains("storefront")) {
            String encoding = context.header("Content-Encoding");
            if (encoding != null) context.header("Content-Encoding", encoding);
            byte[] content = complete.getGenerifiedResponse().getBody();
            context.header("Content-Length", String.valueOf(content.length));
            context.result(content);
        } else {
            String encodingIn = response.getHeaders().getOrDefault("content-encoding", Collections.singletonList(null)).get(0);
            byte[] result = "gzip".equals(encodingIn) ? complete.getByteBody() : complete.getGenerifiedResponse().getBody();
            context.result(result);
        }
    }

    public void register(IRequestModifier modifier, String... methods) {
        for (String method : methods) {
            if (method.equalsIgnoreCase("TRACE") || method.equalsIgnoreCase("CONNECT")) {
                throw new UnsupportedRequestMethodException(method);
            } else {
                if (!map.containsKey(method)) map.put(method, new LinkedList<>());
                map.get(method).add(modifier);
            }
        }
    }

    public void proxy(String target) {
        this.target = target;
    }
}
