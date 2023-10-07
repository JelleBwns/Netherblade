package com.hawolt;

import com.hawolt.http.LocalExecutor;
import com.hawolt.io.Core;
import com.hawolt.io.JsonSource;
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
import com.hawolt.util.RunLevel;
import com.hawolt.util.TaskManager;
import com.hawolt.yaml.LocalSystemYaml;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;

/**
 * Created: 30/07/2022 15:37
 * Author: Twitter @hawolt
 **/

public class Main {
    public static Map<String, DataSocketProxy<?>> rtmp = new HashMap<>();
    public static DataSocketProxy<?> rms, xmpp;

    private static final String[] REQUIRED_VM_OPTIONS = new String[]{
            "--add-exports=java.desktop/sun.java2d=ALL-UNNAMED",
            "--add-exports=java.desktop/sun.awt=ALL-UNNAMED",
            "--add-exports=java.base/java.lang=ALL-UNNAMED"
    };

    private static boolean validateExportOptions() {
        String version = System.getProperty("java.version");
        int major = Integer.parseInt(version.split("\\.")[0]);
        if (major <= 15) return true;
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
        for (String option : REQUIRED_VM_OPTIONS) {
            if (!arguments.contains(option)) return false;
        }
        return true;
    }

    private static ProcessBuilder getApplicationRestartCommand(List<String> arguments) throws Exception {
        String bin = String.join(File.separator, System.getProperty("java.home"), "bin", "java");
        File self = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (!self.getName().endsWith(".jar")) {
            System.err.println("Please manually add the required VM options:");
            for (String requiredVmOption : REQUIRED_VM_OPTIONS) {
                System.err.println(requiredVmOption);
            }
            System.err.println("these options are required to properly run Chromium in OSR mode on Java 9 or higher");
            throw new Exception("Please add the required VM Options or downgrade your Java version");
        }
        ArrayList<String> command = new ArrayList<>();
        command.add(bin);
        command.addAll(Arrays.asList(REQUIRED_VM_OPTIONS));
        command.add("-jar");
        command.add(self.getPath());
        command.addAll(arguments);
        for (int i = 0; i < command.size(); i++) {
            System.out.println(i + " " + command.get(i));
        }
        return new ProcessBuilder(command);
    }

    public static void main(String[] args) {
        try {
            JsonSource source = JsonSource.of(Core.read(RunLevel.get("project.json")).toString());
            Logger.info("Writing log for Netherblade-{}", source.getOrDefault("version", "UNKNOWN-VERSION"));
            for (String pid : TaskManager.retrieve("RiotClientServices")) {
                Logger.debug("Found an existing RiotClientService instance, killing {}", pid);
                TaskManager.kill(pid);
            }
        } catch (IOException e) {
            Logger.error(e);
            System.err.println("Unable to launch Netherblade, exiting (1).");
            System.exit(1);
        }
        List<String> arguments = Arrays.asList(args);
        boolean useOSR = arguments.contains("--osr");
        if (useOSR && !validateExportOptions()) {
            try {
                ProcessBuilder builder = getApplicationRestartCommand(arguments);
                Logger.info("Restarting with required VM Options");
                Process process = builder.start();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                }
            } catch (Exception e) {
                Logger.error(e);
            }
        } else {
            AMFDecoder.debug = false;
            try {
                Javalin.create(config -> config.addStaticFiles("/html", Location.CLASSPATH))
                        .before("/v1/*", context -> {
                            context.header("Access-Control-Allow-Origin", "*");
                        })
                        .routes(LocalExecutor::configure)
                        .start(35199);
                SocketServer.launch();
                Netherblade.create(useOSR);
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
}
