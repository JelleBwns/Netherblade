package com.hawolt.mitm.rule.impl;

import com.hawolt.http.proxy.IRequest;
import com.hawolt.logger.Logger;
import com.hawolt.mitm.rule.AbstractRewriteRule;
import com.hawolt.socket.rms.WebsocketFrame;
import org.json.JSONObject;

import java.io.IOException;

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
    public WebsocketFrame rewrite(String id, WebsocketFrame frame) {
        try {
            String message = WebsocketFrame.getMessage(frame);
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
