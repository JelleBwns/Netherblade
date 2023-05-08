package com.hawolt.util;

import com.hawolt.logger.Logger;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Browser {

    public static void navigate(String uri) {
        try {
            Desktop.getDesktop().browse(new URI(uri));
        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
        }
    }
}
