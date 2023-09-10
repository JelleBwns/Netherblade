package com.hawolt.socket.rms;

import com.hawolt.logger.Logger;
import com.hawolt.mitm.CommunicationType;
import com.hawolt.mitm.Unsafe;
import com.hawolt.mitm.rtmp.ByteMagic;
import com.hawolt.mitm.rule.FrameAction;
import com.hawolt.mitm.rule.RuleInterpreter;
import com.hawolt.socket.DataSocketProxy;
import com.hawolt.socket.SocketInterceptor;
import com.hawolt.ui.SocketServer;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

public class RmsSocketProxy extends DataSocketProxy<WebsocketFrame> {
    private final List<String> cache = new ArrayList<>();
    private final byte[] localhost = new byte[]{49, 50, 55, 46, 48, 46, 48, 46, 49, 58, 49, 49, 52, 52, 51};

    public RmsSocketProxy(String hostname, int remote, int local, Function<byte[], WebsocketFrame> transformer) {
        super(hostname, remote, local, transformer);
        setInterceptor(new SocketInterceptor() {
            @Override
            public void sniffOriginalClient(byte[] b) throws Exception {

            }

            @Override
            public void sniffOriginalServer(byte[] b) throws Exception {

            }

            @Override
            public void sniffSpoofedClient(byte[] b) throws Exception {

            }

            @Override
            public void sniffSpoofedServer(byte[] b) throws Exception {

            }
        });
    }

    private FrameAction handle(boolean in, WebsocketFrame frame) throws IOException {
        if (frame.getOpCode() != 4) return FrameAction.DROP;
        CommunicationType type = in ? CommunicationType.INGOING : CommunicationType.OUTGOING;
        WebsocketFrame modified = Unsafe.cast(RuleInterpreter.map.get(type).rewriteRMS(frame));
        if (modified == null) {
            Logger.error("[rms] drop: {}", frame);
            return FrameAction.DROP;
        }
        String message = WebsocketFrame.getMessage(frame);
        JSONObject object = new JSONObject().put("type", "rms");
        SocketServer.forward(object.put(in ? "in" : "out", new JSONObject(message)).toString());
        Logger.debug("[rms] {} {}", in ? "<" : ">", message);
        return FrameAction.FORWARD;
    }

    private byte[] handle(boolean in, byte[] b) {
        String hash = hash(b);
        String raw = new String(b, StandardCharsets.UTF_8);
        if (cache.contains(hash) || raw.contains("HTTP") || transformer == null) return b;
        else cache.add(hash);
        WebsocketFrame frame = transformer.apply(b);
        List<FrameAction> list = new ArrayList<>();
        try {
            list.add(handle(in, frame));
            while (frame.isMultiFrame()) {
                frame = new WebsocketFrame(frame.getOverhead());
                list.add(handle(in, frame));
            }
        } catch (Exception e) {
            Logger.error(e);
        }
        return list.contains(FrameAction.DROP) ? null : b;
    }

    @Override
    public byte[] onApplicationData(byte[] b) {
        int index = ByteMagic.indexOf(b, localhost);
        byte[] data;
        if (index != -1) {
            byte[] hostname = super.hostname.getBytes();
            byte[] altered = new byte[b.length - localhost.length + hostname.length];
            System.arraycopy(b, 0, altered, 0, index);
            System.arraycopy(hostname, 0, altered, index, hostname.length);
            System.arraycopy(b, index + localhost.length, altered, index + hostname.length, b.length - (index + localhost.length));
            data = altered;
        } else {
            data = b;
        }
        handle(false, data);
        return data;
    }

    @Override
    public byte[] onServerData(byte[] b) {
        return handle(true, b);
    }


    private String hash(byte[] b) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            return Base64.getEncoder().encodeToString(b);
        }
        return ByteMagic.toHex(digest.digest(b));
    }
}
