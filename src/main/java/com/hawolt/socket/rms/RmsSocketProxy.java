package com.hawolt.socket.rms;

import com.hawolt.io.Core;
import com.hawolt.logger.Logger;
import com.hawolt.mitm.rtmp.ByteMagic;
import com.hawolt.rtmp.utility.Base64GZIP;
import com.hawolt.socket.DataSocketProxy;
import com.hawolt.socket.SocketInterceptor;
import com.hawolt.ui.SocketServer;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

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

    private void handle(boolean in, WebsocketFrame frame) throws IOException {
        String message;
        if (Base64GZIP.isGzip(frame.getPayload())) {
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(frame.getPayload()))) {
                message = Core.read(gis).toString();
            }
        } else {
            message = new String(frame.getPayload());
        }
        JSONObject object = new JSONObject().put("type", "rms");
        SocketServer.forward(object.put(in ? "in" : "out", new JSONObject(message)).toString());
        Logger.error("[rms] {} {}", in ? "<" : ">", message);
    }

    private void handle(boolean in, byte[] b) {
        Logger.error("IN: {}, DATA: {}", in, ByteMagic.toHex(b));
        String hash = hash(b);
        String raw = new String(b);
        if (cache.contains(hash) || raw.contains("HTTP") || transformer == null) return;
        else cache.add(hash);
        WebsocketFrame frame = transformer.apply(b);
        try {
            handle(in, frame);
            while (frame.isMultiFrame()) {
                frame = new WebsocketFrame(frame.getOverhead());
                handle(in, frame);
            }
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    @Override
    public byte[] onServerData(byte[] b) {
        handle(true, b);
        return b;
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
