package com.hawolt.socket.rtmp;

import com.hawolt.logger.Logger;
import com.hawolt.mitm.CommunicationType;
import com.hawolt.mitm.Unsafe;
import com.hawolt.mitm.rule.RuleInterpreter;
import com.hawolt.rtmp.amf.TypedObject;
import com.hawolt.rtmp.amf.decoder.AMFDecoder;
import com.hawolt.socket.DataSocketProxy;
import com.hawolt.ui.SocketServer;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.function.Function;

public class RtmpSocketProxy extends DataSocketProxy<TypedObject> {
    private final AMFDecoder incoming = new AMFDecoder() {
        @Override
        public byte read() {
            byte b = data[position++];
            if ((((int) b) & 0xFF) == 0xC3) return read();
            return b;
        }
    };
    private int count;

    public RtmpSocketProxy(String hostname, int remote, int local, Function<byte[], TypedObject> transformer) {
        super(hostname, remote, local, transformer);
    }

    @Override
    public byte[] onServerData(byte[] b) {
        try {
            if (++count < 6) return b;
            TypedObject object = incoming.decode(Arrays.copyOfRange(b, 12, b.length), new TypedObject());
            Logger.debug("[rtmp-plaintext] < {}", object);
            JSONObject json = TypedObject.tidy(object);
            JSONObject o = new JSONObject().put("type", "rtmp");
            SocketServer.forward(o.put("in", json).toString());
            byte[] bytes = Unsafe.cast(RuleInterpreter.map.get(CommunicationType.INGOING).rewriteRTMP(b, json));
            if (bytes == null) return null;
        } catch (Exception e) {
            //TODO ignore these for now
        }
        return b;
    }

    @Override
    public byte[] onApplicationData(byte[] b) {
        try {
            if (++count < 6) return b;
            TypedObject object = incoming.decode(Arrays.copyOfRange(b, 12, b.length), new TypedObject());
            Logger.debug("[rtmp-plaintext] > {}", object);
            JSONObject json = TypedObject.tidy(object);
            JSONObject o = new JSONObject().put("type", "rtmp");
            SocketServer.forward(o.put("out", json).toString());
            byte[] bytes = Unsafe.cast(RuleInterpreter.map.get(CommunicationType.OUTGOING).rewriteRTMP(b, json));
            if (bytes == null) return null;
        } catch (Exception e) {
            Logger.error(e);
        }
        return b;
    }
}
