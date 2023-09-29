package com.hawolt.util;

import com.hawolt.io.Core;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created: 19/07/2022 18:45
 * Author: Twitter @hawolt
 **/

public class WMIC {
    public static String wmic() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("WMIC", "path", "win32_process", "get", "Caption,Processid,Commandline");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try (InputStream stream = process.getInputStream()) {
            return Core.read(stream).toString();
        }
    }
}
