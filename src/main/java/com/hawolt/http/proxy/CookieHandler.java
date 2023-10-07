package com.hawolt.http.proxy;

import com.hawolt.http.layer.IResponse;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created: 09/01/2023 21:17
 * Author: Twitter @hawolt
 **/

public class CookieHandler {

    private final Map<String, Cookie> map = new HashMap<>();
    private final Object lock = new Object();


    public void handle(ProxyResponse response) {
        synchronized (lock) {
            List<String> base = response.getHeaders().get("set-cookie");
            if (base != null) {
                List<Cookie> list = base.stream()
                        .map(line -> new Cookie(response.url(), line))
                        .collect(Collectors.toList());
                for (Cookie cookie : list) {
                    map.put(cookie.getName(), cookie);
                }
            }
        }
    }

    public String getCookie(String hostname) {
        synchronized (lock) {
            return map.values().stream()
                    .filter(cookie -> cookie.isValidFor(hostname))
                    .filter(Cookie::isNotExpired)
                    .filter(Cookie::hasValue)
                    .map(Cookie::get)
                    .collect(Collectors.joining("; "));
        }
    }
}
