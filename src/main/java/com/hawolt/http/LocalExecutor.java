package com.hawolt.http;

import com.hawolt.Main;
import com.hawolt.http.proxy.BasicProxyServer;
import com.hawolt.http.proxy.ProxyRequest;
import com.hawolt.http.proxy.ProxyResponse;
import com.hawolt.logger.Logger;
import com.hawolt.mitm.CommunicationType;
import com.hawolt.mitm.InstructionType;
import com.hawolt.mitm.Unsafe;
import com.hawolt.mitm.rule.RuleInterpreter;
import com.hawolt.ui.Netherblade;
import com.hawolt.ui.SocketServer;
import com.hawolt.util.Browser;
import com.hawolt.util.LocaleInstallation;
import com.hawolt.util.StaticConstants;
import com.hawolt.yaml.SystemYaml;
import io.javalin.http.Handler;
import org.json.JSONArray;
import org.json.JSONObject;
import sun.nio.ch.Net;

import javax.swing.*;
import javax.xml.bind.DatatypeConverter;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;


/**
 * Created: 31/07/2022 00:57
 * Author: Twitter @hawolt
 **/

public class LocalExecutor {

    private static final Handler AVAILABLE = context -> {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        for (String region : SystemYaml.config.keySet()) {
            array.put(region);
        }
        object.put("regions", array);
        context.result(object.toString());
    };

    private final static String[] SUPPORTED = new String[]{"GET", "HEAD", "POST", "PUT", "OPTIONS", "DELETE", "PATCH"};

    private static final Handler LAUNCH = context -> {
        String client = LocaleInstallation.RIOT_CLIENT_SERVICES.toString();
        try {
            Runtime.getRuntime().exec(String.join(" ", client, "--client-config-url=\"http://127.0.0.1:" + StaticConstants.PORT_MAPPING.get("config") + "\"", "--launch-product=league_of_legends", "--launch-patchline=live", "--allow-multiple-clients"));
        } catch (IOException e) {
            Logger.error(e);
        }
        Map<String, BasicProxyServer> map = new HashMap<>();
        JSONObject system = SystemYaml.config.getJSONObject(context.pathParam("region"));
        map.put("config", new BasicProxyServer(StaticConstants.PORT_MAPPING.get("config"), system.getString("config")));
        for (String type : system.keySet()) {
            if (type.equalsIgnoreCase("config")) continue;
            map.put(type, new BasicProxyServer(StaticConstants.PORT_MAPPING.get(type), system.getString(type)));
            Logger.debug("Proxy {}:{} on {}:{}", type.toUpperCase(), system.getString(type), "http://127.0.0.1", StaticConstants.PORT_MAPPING.get(type));
        }

        map.get("config").register(new IRequestModifier() {
            @Override
            public ProxyRequest onBeforeRequest(ProxyRequest o) {
                return Unsafe.cast(RuleInterpreter.map.get(CommunicationType.OUTGOING).rewrite(Unsafe.cast(o)));
            }

            @Override
            public ProxyResponse onResponse(ProxyResponse o) {
                ProxyResponse response = Unsafe.cast(RuleInterpreter.map.get(CommunicationType.INGOING).rewrite(Unsafe.cast(o)));
                String plain = new String(response.getByteBody());
                for (String type : system.keySet()) {
                    String target = system.getString(type);
                    String replacement = String.format("http://127.0.0.1:%d", StaticConstants.PORT_MAPPING.get(type));
                    plain = plain.replaceAll(target, replacement);
                    Logger.debug("Rewriting {} to {} in request {}", target, replacement, o.getOriginal().getUrl());
                }
                //Logger.debug(plain);
                o.setBody(plain.getBytes(StandardCharsets.UTF_8));
                return o;
            }

            @Override
            public void onException(Exception e) {
                Logger.error(e);
            }
        }, SUPPORTED);


        for (String key : map.keySet()) {
            BasicProxyServer server = map.get(key);
            server.register(new IRequestModifier() {
                @Override
                public ProxyRequest onBeforeRequest(ProxyRequest o) {
                    return Unsafe.cast(RuleInterpreter.map.get(CommunicationType.OUTGOING).rewrite(Unsafe.cast(o)));
                }

                @Override
                public ProxyResponse onResponse(ProxyResponse o) {
                    ProxyResponse response = Unsafe.cast(RuleInterpreter.map.get(CommunicationType.INGOING).rewrite(Unsafe.cast(o)));
                    ProxyRequest request = response.getOriginal();
                    JSONObject object = new JSONObject();
                    JSONObject sent = new JSONObject();
                    sent.put("method", request.getMethod());
                    String[] data = request.getUrl().split("\\?");
                    JSONArray query = new JSONArray();
                    if (data.length > 1) {
                        String[] params = data[1].split("&");
                        for (String pair : params) {
                            String[] values = pair.split("=");
                            JSONObject parameter = new JSONObject();
                            parameter.put("k", values[0]);
                            parameter.put("v", values.length > 1 ? values[1] : JSONObject.NULL);
                            query.put(parameter);
                        }
                    }
                    sent.put("uri", data[0]);
                    sent.put("query", query);
                    JSONArray headers1 = new JSONArray();
                    for (Map.Entry<String, String> entry : request.getOriginalHeaders().entrySet()) {
                        JSONObject header = new JSONObject();
                        header.put("k", entry.getKey());
                        header.put("v", entry.getValue());
                        headers1.put(header);
                    }

                    sent.put("headers", headers1);
                    sent.put("body", response.getOriginal().getBody());
                    object.put("request", sent);
                    JSONObject received = new JSONObject();
                    received.put("code", response.getCode());

                    JSONArray headers2 = new JSONArray();
                    for (Map.Entry<String, List<String>> entry : response.getHeaders().entrySet()) {
                        List<String> list = entry.getValue();
                        for (String value : list) {
                            JSONObject header = new JSONObject();
                            if (entry.getKey().startsWith("access-control")) continue;
                            header.put("k", entry.getKey());
                            header.put("v", value);
                            headers2.put(header);
                        }
                    }
                    received.put("headers", headers2);
                    received.put("body", response.getByteBody() != null ? new String(response.getByteBody()) : JSONObject.NULL);
                    object.put("received", received);

                    object.put("type", "http");
                    SocketServer.forward(object.toString());
                    return response;
                }

                @Override
                public void onException(Exception e) {

                }
            }, SUPPORTED);
        }
    };


    public static void configure() {
        path("/v1", () -> {
            path("/client", () -> {
                get("/available", LocalExecutor.AVAILABLE);
                get("/launch/{region}", LocalExecutor.LAUNCH);
            });
            path("/config", () -> {
                get("/load", RuleInterpreter.RELOAD);
                get("/close", context -> System.exit(0));
                get("/minimize", Netherblade.MINIMIZE);
                get("/maximize", Netherblade.MAXIMIZE);
                get("/wiki", context -> Browser.navigate("https://github.com/Riotphobia/Netherblade/wiki"));
            });
        });
    }
}