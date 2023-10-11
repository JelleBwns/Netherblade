package com.hawolt.lcu;

import com.hawolt.io.Core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created: 19/07/2022 18:45
 * Author: Twitter @hawolt
 **/

public class WMIC {
    private static final Pattern pattern = Pattern.compile("\"--riotclient-auth-token=(.*?)\"(.*)\"--riotclient-app-port=(.*?)\"(.*)\"--remoting-auth-token=(.*?)\"(.*)\"--app-port=(.*?)\"");

    private static String wmic() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("WMIC", "path", "win32_process", "get", "Caption,Processid,Commandline");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try (InputStream stream = process.getInputStream()) {
            return Core.read(stream).toString();
        }
    }

    public static List<LeagueClient> retrieve() throws IOException {
        List<LeagueClient> list = new ArrayList<>();
        for (String line : wmic().split(System.lineSeparator())) {
            if (!line.startsWith("LeagueClientUx.exe")) continue;
            Matcher matcher = pattern.matcher(line);
            if (matcher.find())
                list.add(new LeagueClient(matcher.group(1), matcher.group(3), matcher.group(5), matcher.group(7)));
        }
        return list;
    }

    public static LeagueClient retrieve(String port) throws IOException {
        for (String line : wmic().split(System.lineSeparator())) {
            if (!line.startsWith("LeagueClientUx.exe")) continue;
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                LeagueClient client = new LeagueClient(matcher.group(1), matcher.group(3), matcher.group(5), matcher.group(7));
                if (client.getLeaguePort().equals(port)) return client;
            }
        }
        return null;
    }
}
