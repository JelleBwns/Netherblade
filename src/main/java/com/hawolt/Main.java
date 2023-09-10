package com.hawolt;

import com.hawolt.http.LocalExecutor;
import com.hawolt.logger.Logger;
import com.hawolt.mitm.cache.InternalStorage;
import com.hawolt.mitm.rule.RuleInterpreter;
import com.hawolt.rtmp.amf.decoder.AMFDecoder;
import com.hawolt.socket.DataSocketProxy;
import com.hawolt.socket.rms.RmsSocketProxy;
import com.hawolt.socket.rms.WebsocketFrame;
import com.hawolt.socket.rtmp.RtmpSocketProxy;
import com.hawolt.socket.xmpp.XmppSocketProxy;
import com.hawolt.ui.Netherblade;
import com.hawolt.ui.SocketServer;
import com.hawolt.yaml.LocalSystemYaml;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Created: 30/07/2022 15:37
 * Author: Twitter @hawolt
 **/

public class Main {
    public static Map<String, DataSocketProxy<?>> rtmp = new HashMap<>();
    public static DataSocketProxy<?> rms, xmpp;

    public static void main(String[] args) {
        AMFDecoder.debug = false;
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
                LocalSystemYaml.rewrite();
            } catch (Exception e) {
                Logger.error(e);
            }
            //TODO experimental
            for (Map.Entry<Integer, String> entry : LocalSystemYaml.map.entrySet()) {
                Logger.debug("[rtmp] setting up proxy on port {} for {}", entry.getKey(), entry.getValue());
                RtmpSocketProxy rtmp = new RtmpSocketProxy(entry.getValue(), 2099, entry.getKey(), null);
                Main.rtmp.put(entry.getValue(), rtmp);
                rtmp.start();
            }
            //TODO experimental
            InternalStorage.registerStorageListener(true, "rmstoken", rmstoken -> {
                JSONObject object = new JSONObject(new String(Base64.getDecoder().decode(rmstoken.split("\\.")[1])));
                String affinity = object.getString("affinity");
                InternalStorage.registerStorageListener(false, String.join("-", "rms", affinity), host -> {
                    Logger.debug("[rms] setting up proxy on port {} for {}", 11443, host);
                    rms = new RmsSocketProxy(host, 443, 11443, WebsocketFrame::new);
                    rms.start();
                });
            });
            //TODO experimental
            InternalStorage.registerStorageListener(true, "xmpptoken", xmpptoken -> {
                JSONObject object = new JSONObject(new String(Base64.getDecoder().decode(xmpptoken.split("\\.")[1])));
                String affinity = object.getString("affinity");
                InternalStorage.registerStorageListener(false, String.join("-", "xmpp", affinity), host -> {
                    Logger.debug("[xmpp] setting up proxy on port {} for {}", 5223, host);
                    xmpp = new XmppSocketProxy(host, 5223, 5223, String::new);
                    xmpp.start();
                });
            });
        } catch (IOException e) {
            Logger.error(e);
            System.err.println("Unable to launch Netherblade, exiting (1).");
            System.exit(1);
        }
    }
}
