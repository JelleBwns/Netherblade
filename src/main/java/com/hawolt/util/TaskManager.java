package com.hawolt.util;

import com.hawolt.logger.Logger;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Created: 19/07/2022 18:45
 * Author: Twitter @hawolt
 **/

public class TaskManager {

    public static List<String> retrieve(String process) throws IOException {
        return retrieve(process, null);
    }

    public static List<String> retrieve(String process, String additional) throws IOException {
        String self = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        List<String> list = new ArrayList<>();
        for (String line : WMIC.wmic().split(System.lineSeparator())) {
            if (!line.startsWith(process) || (additional != null && !line.contains(additional))) continue;
            String pid = line.substring(line.lastIndexOf("\"") + 1).trim();
            if (pid.equals(self)) continue;
            list.add(pid);
        }
        return list;
    }

    public static void kill(String pid) {
        try {
            Runtime.getRuntime().exec("TASKKILL /F /IM " + pid);
        } catch (IOException e) {
            Logger.error(e);
        }
    }
}
