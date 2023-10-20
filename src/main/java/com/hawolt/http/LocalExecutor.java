package com.hawolt.http;

import com.hawolt.http.proxy.BasicProxyServer;
import com.hawolt.http.proxy.CookieHandler;
import com.hawolt.http.proxy.ProxyRequest;
import com.hawolt.http.proxy.ProxyResponse;
import com.hawolt.lcu.LCUWebSocket;
import com.hawolt.logger.Logger;
import com.hawolt.mitm.CommunicationType;
import com.hawolt.mitm.Unsafe;
import com.hawolt.mitm.rule.RuleInterpreter;
import com.hawolt.ui.Netherblade;
import com.hawolt.ui.SocketServer;
import com.hawolt.util.Browser;
import com.hawolt.util.LocaleInstallation;
import com.hawolt.util.StaticConstants;
import com.hawolt.yaml.LocalSystemYaml;
import io.javalin.http.Handler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;


/**
 * Created: 31/07/2022 00:57
 * Author: Twitter @hawolt
 **/

public class LocalExecutor {
    public static final Map<String, CookieHandler> clientCookieHandlerMap = new HashMap<>();
    private static final Map<String, BasicProxyServer> map = new HashMap<>();

    private static final Handler AVAILABLE = context -> {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        Logger.debug("system.yaml: {}", LocaleInstallation.SYSTEM_YAML);
        Logger.debug("RiotClientServices: {}", LocaleInstallation.RIOT_CLIENT_SERVICES);
        Logger.debug("file-content: system.yaml: {}", LocalSystemYaml.config.toString());
        for (String region : LocalSystemYaml.config.keySet()) {
            array.put(region);
        }
        array.put("PBE");
        object.put("regions", array);
        context.result(object.toString());
    };

    private final static String[] SUPPORTED = new String[]{"GET", "HEAD", "POST", "PUT", "OPTIONS", "DELETE", "PATCH"};

    private static final Handler LAUNCH = context -> {
        String region = context.pathParam("region");
        String client = LocaleInstallation.RIOT_CLIENT_SERVICES.toString();
        try {
            String patchline = region.equals("PBE") ? "pbe" : "live";
            Runtime.getRuntime().exec(String.join(" ", client, "--client-config-url=\"http://127.0.0.1:" + StaticConstants.PORT_MAPPING.get("config") + "\"", "--launch-product=league_of_legends", "--launch-patchline=" + patchline, "--allow-multiple-clients"));
        } catch (IOException e) {
            Logger.error(e);
        }
        String id = UUID.randomUUID().toString();
        CookieHandler handler = new CookieHandler();
        LocalExecutor.clientCookieHandlerMap.put(id, handler);
        register("entitlement", "https://entitlements.auth.riotgames.com/api/token/v1", id);
        register("playerpreference", "https://playerpreferences.riotgames.com", id);
        register("email", "https://email-verification.riotgames.com/api", id);
        register("config", "https://clientconfig.rpg.riotgames.com", id);
        register("geo", "https://riot-geo.pas.si.riotgames.com", id);
        register("auth", "https://auth.riotgames.com", id);
        register("authenticator", "https://authenticate.riotgames.com", id);
        register("entitlements", "https://entitlements.auth.riotgames.com", id);
    };

    private static final Handler STARTWS = context -> {
        String shouldStart = context.pathParam("shouldStart");
        if (shouldStart.equals("true"))
            LCUWebSocket.launch();
        else
            LCUWebSocket.disconnect();
    };

    public static void register(String type, String target, String id) {
        String proxy = String.join(":", "http://127.0.0.1", String.valueOf(StaticConstants.PORT_MAPPING.get(type)));
        Logger.debug("[EXECUTOR] SETUP {} PROXY FOR {} ON {}", type, target, proxy);
        if (map.containsKey(type)) {
            map.get(type).proxy(target);
        } else {
            BasicProxyServer server = new BasicProxyServer(StaticConstants.PORT_MAPPING.get(type), target, id);
            map.put(type, server);
            server.register(new IRequestModifier() {
                @Override
                public ProxyRequest onBeforeRequest(ProxyRequest o) {
                    return Unsafe.cast(RuleInterpreter.map.get(CommunicationType.OUTGOING).rewrite(id, Unsafe.cast(o)));
                }

                @Override
                public ProxyResponse onResponse(ProxyResponse o) {
                    ProxyResponse response = Unsafe.cast(RuleInterpreter.map.get(CommunicationType.INGOING).rewrite(id, Unsafe.cast(o)));
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
                    received.put("body", response.getByteBody() != null ? new String(response.getByteBody(), StandardCharsets.UTF_8) : JSONObject.NULL);
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
    }


    public static void configure() {
        path("/v1", () -> {
            path("/client", () -> {
                get("/available", LocalExecutor.AVAILABLE);
                get("/launch/{region}", LocalExecutor.LAUNCH);
            });
            path("/lcu", () -> {
                get("/startws/{shouldStart}", LocalExecutor.STARTWS);
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