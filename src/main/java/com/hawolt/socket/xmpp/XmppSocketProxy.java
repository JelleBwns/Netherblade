package com.hawolt.socket.xmpp;

import com.hawolt.logger.Logger;
import com.hawolt.socket.DataSocketProxy;
import com.hawolt.ui.SocketServer;
import org.json.JSONObject;

import java.util.function.Function;

public class XmppSocketProxy extends DataSocketProxy<String> {
    public XmppSocketProxy(String hostname, int remote, int local, Function<byte[], String> transformer) {
        super(hostname, remote, local, transformer);
    }

    @Override
    public byte[] onServerData(byte[] b) {
        String xml = new String(b);
        JSONObject o = new JSONObject().put("type", "xmpp");
        SocketServer.forward(o.put("in", xml).toString());
        Logger.debug("[xmpp] < {}", xml);
        return b;
    }

    @Override
    public byte[] onApplicationData(byte[] b) {
        String xml = new String(b);
        JSONObject o = new JSONObject().put("type", "xmpp");
        SocketServer.forward(o.put("out", xml).toString());
        Logger.debug("[xmpp] > {}", xml);
        return b;
    }
}
