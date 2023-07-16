package com.hawolt.yaml;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.hawolt.logger.Logger;
import com.hawolt.util.LocaleInstallation;
import com.hawolt.yaml.objects.YamlRegion;
import com.hawolt.yaml.objects.YamlServers;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created: 30/07/2022 17:52
 * Author: Twitter @hawolt
 **/

@SuppressWarnings("all")
public class LocalSystemYaml {
    public static final Map<Integer, String> map = new HashMap<>();


    public static JSONObject config;

    static {
        try {
            LocalSystemYaml.config = generate();
        } catch (IOException e) {
            Logger.error(e);
            System.err.println("Unable to generate internal json mapping, exiting (1).");
            System.exit(1);
        }
    }

    public String default_region, player_bug_report_url;
    public Map app, build, patcher, player_support_url, region_data, riotclient;
    public ArrayList<LinkedHashMap> products;

    public static JSONObject generate() throws IOException {
        YamlReader reader = new YamlReader(new String(Files.readAllBytes(LocaleInstallation.SYSTEM_YAML.toPath())));
        LocalSystemYaml yaml = reader.read(LocalSystemYaml.class);
        JSONObject object = new JSONObject();
        for (Object key : yaml.region_data.keySet()) {
            YamlRegion yamlRegion = new YamlRegion((String) key, yaml.region_data.get(key));
            YamlServers yamlServers = yamlRegion.getServers();
            JSONObject region = new JSONObject();
            region.put("config", yamlServers.getConfig());
            region.put("email", yamlServers.getEmail());
            region.put("entitlement", yamlServers.getEntitlement());
            region.put("queue", yamlServers.getQueue());
            region.put("ledge", yamlServers.getLedge());
            region.put("platform", yamlServers.getPlatform());
            region.put("geo", "https://riot-geo.pas.si.riotgames.com");
            object.put(yamlRegion.getName(), region);
        }
        return object;
    }

    public static void rewrite() throws IOException {
        Path path = LocaleInstallation.SYSTEM_YAML.toPath();
        Logger.debug("system.yaml: {}", path);
        Path original = path.getParent().resolve("system.yaml.backup");
        List<String> lines;
        if (original.toFile().exists()) {
            lines = Files.readAllLines(original);
        } else {
            Files.write(original, Files.readAllBytes(path));
            lines = Files.readAllLines(path);
        }
        int number = 21110;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.trim().equals("lcds:")) continue;
            StringBuilder host = new StringBuilder(lines.get(i + 1));
            int indexOfHost = host.indexOf(":") + 2;
            String relay = host.substring(indexOfHost, host.length());
            int firstIndex = relay.indexOf(".");
            int secondIndex = relay.indexOf(".", firstIndex + 1);
            String region = relay.substring(firstIndex + 1, secondIndex);
            relay = String.format("feapp.%s.lol.pvp.net", region);
            host.replace(indexOfHost, host.length(), "127.0.0.1");
            lines.set(i + 1, host.toString());
            StringBuilder port = new StringBuilder(lines.get(i + 2));
            int indexOfPort = port.indexOf(":") + 2;
            int mapping = ++number;
            port.replace(indexOfPort, port.length(), String.valueOf(mapping));
            lines.set(i + 2, port.toString());
            StringBuilder tls = new StringBuilder(lines.get(i + 4));
            int indexOfTLS = tls.indexOf(":") + 2;
            tls.replace(indexOfTLS, tls.length(), "false");
            lines.set(i + 4, tls.toString());
            Logger.debug("mapping {} to port {}", relay, mapping);
            map.put(mapping, relay);
            i += 4;
        }
        byte[] bytes = lines.stream().collect(Collectors.joining(System.lineSeparator())).getBytes();
        Files.write(path, bytes);
    }
}
