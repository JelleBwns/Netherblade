package com.hawolt.mitm.rule.impl;

import com.hawolt.http.proxy.IRequest;
import com.hawolt.io.Core;
import com.hawolt.logger.Logger;
import com.hawolt.mitm.rule.AbstractRewriteRule;
import com.hawolt.rtmp.utility.Base64GZIP;
import com.hawolt.socket.rms.WebsocketFrame;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * Created: 22/11/2022 04:02
 * Author: Twitter @hawolt
 **/

public class RiotMessagingServiceRule extends AbstractRewriteRule<WebsocketFrame, WebsocketFrame> {
    private final String plain;

    public RiotMessagingServiceRule(JSONObject object) {
        super(object);
        this.plain = object.getString("find");
    }

    @Override
    public WebsocketFrame rewrite(WebsocketFrame frame) {
        try {
            String message;
            if (frame.getPayload().length >= 2 && Base64GZIP.isGzip(frame.getPayload())) {
                try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(frame.getPayload()))) {
                    message = Core.read(gis).toString();
                }
            } else {
                message = new String(frame.getPayload());
            }
            if (message.contains(plain)) return null;
        } catch (IOException e) {
            Logger.error(e);
        }
        return frame;
    }

    @Override
    public boolean isTargeted(IRequest request) {
        return true;
    }

}
