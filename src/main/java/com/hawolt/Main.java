package com.hawolt;

import com.hawolt.http.LocalExecutor;
import com.hawolt.logger.Logger;
import com.hawolt.mitm.cache.InternalStorage;
import com.hawolt.mitm.rule.RuleInterpreter;
import com.hawolt.socket.rms.RmsSocketProxy;
import com.hawolt.socket.rms.WebsocketFrame;
import com.hawolt.socket.rtmp.RtmpSocketProxy;
import com.hawolt.socket.xmpp.XmppSocketProxy;
import com.hawolt.ui.Netherblade;
import com.hawolt.ui.SocketServer;
import com.hawolt.util.ReflectHttp;
import com.hawolt.yaml.SystemYaml;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

/**
 * Created: 30/07/2022 15:37
 * Author: Twitter @hawolt
 **/

public class Main {
    public static RmsSocketProxy proxy;

    public static void main(String[] args) {
        String version = System.getProperty("java.version");
        int major = Integer.parseInt(version.split("\\.")[0]);
        if (major <= 15) {
            try {
                ReflectHttp.enable("PATCH");
            } catch (NoSuchFieldException | IllegalAccessException e) {
                Logger.error(e);
                System.err.println("Unable to modify permitted HTTP methods, exiting (1).");
                System.exit(1);
            }
        } else {
            System.err.println("Incompatible Java version, please use Java 8 upto 15.");
            System.exit(1);
        }
        try {
            Javalin.create(config -> config.addStaticFiles("/html", Location.CLASSPATH))
                    .before("/v1/*", context -> {
                        context.header("Access-Control-Allow-Origin", "*");
                    })
                    .routes(LocalExecutor::configure)
                    .start(35199);
            SocketServer.launch();
            Netherblade.create();
            RuleInterpreter.reload(null);
            try {
                SystemYaml.rewrite();
            } catch (Exception e) {
                Logger.error(e);
            }
            //TODO experimental
            for (Map.Entry<Integer, String> entry : SystemYaml.map.entrySet()) {
                Logger.debug("[rtmp] setting up proxy on port {} for {}", entry.getKey(), entry.getValue());
                RtmpSocketProxy proxy = new RtmpSocketProxy(entry.getValue(), 2099, entry.getKey(), null);
                proxy.start();
            }
            //TODO experimental
            InternalStorage.registerStorageListener(false, "rmstoken", rmstoken -> {
                JSONObject object = new JSONObject(new String(Base64.getDecoder().decode(rmstoken.split("\\.")[1])));
                String affinity = object.getString("affinity");
                InternalStorage.registerStorageListener(false, String.join("-", "rms", affinity), host -> {
                    Logger.debug("[rms] setting up proxy on port {} for {}", 11443, host);
                    proxy = new RmsSocketProxy(host, 443, 11443, WebsocketFrame::new);
                    proxy.start();
                });
            });
            //TODO experimental
            InternalStorage.registerStorageListener(true, "xmpptoken", xmpptoken -> {
                JSONObject object = new JSONObject(new String(Base64.getDecoder().decode(xmpptoken.split("\\.")[1])));
                String affinity = object.getString("affinity");
                InternalStorage.registerStorageListener(false, String.join("-", "xmpp", affinity), host -> {
                    Logger.debug("[xmpp] setting up proxy on port {} for {}", 5223, host);
                    XmppSocketProxy proxy = new XmppSocketProxy(host, 5223, 5223, String::new);
                    proxy.start();
                });
            });
        } catch (IOException e) {
            Logger.error(e);
            System.err.println("Unable to launch Netherblade, exiting (1).");
            System.exit(1);
        }
    }

}
