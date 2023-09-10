package com.hawolt.socket.rms;

import com.hawolt.io.Core;
import com.hawolt.mitm.rtmp.ByteMagic;
import com.hawolt.rtmp.utility.Base64GZIP;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

public class WebsocketFrame {
    private final byte[] b;
    private final int length, startIndex;

    public WebsocketFrame(byte[] b) {
        this.b = b;
        int l1 = getLengthByte();
        int l2 = getLengthShort();
        int l3 = getLengthInt();
        this.startIndex = (l1 != 126 ? 2 : l2 != 127 ? 4 : 8) + (isMasked() ? 4 : 0);
        this.length = l1 == 126 ? l2 == 127 ? l3 : l2 : l1;
    }

    public int getLength() {
        return length;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public boolean isFinalSegment() {
        return (ByteMagic.reverse(b[0] & 0xFF) & 0b1) == 0b1;
    }

    public boolean getRSV1() {
        return (ByteMagic.reverse(b[0] & 0xFF) & 0b10) == 0b10;
    }

    public boolean getRSV2() {
        return (ByteMagic.reverse(b[0] & 0xFF) & 0b100) == 0b100;
    }

    public boolean getRSV3() {
        return (ByteMagic.reverse(b[0] & 0xFF) & 0b1000) == 0b1000;
    }

    public boolean isValid() {
        return !getRSV1() && !getRSV2() && !getRSV3();
    }

    public int getOpCode() {
        return (ByteMagic.reverse((b[0]) & 0xFF) & 0xFF) >> 4;
    }

    public boolean isMasked() {
        return ((b[1] >> 8) & 0b1) == 0b1;
    }

    public byte getLengthByte() {
        return (byte) (b[1] & 0b01111111);
    }

    public int getLengthShort() {
        return (b[2] & 0xFF) << 8 | b[3] & 0xFF;
    }

    public int getLengthInt() {
        if (b.length < 8) return 0;
        return (b[4] & 0xFF) << 24 |
                (b[5] & 0xFF) << 16 |
                (b[6] & 0xFF) << 8 |
                b[7] & 0xFF;
    }

    public byte[] getMask() {
        int startIndex = (getLengthByte() != 126 ? 2 : getLengthShort() != 127 ? 4 : 8);
        return Arrays.copyOfRange(b, startIndex, startIndex + 4);
    }

    public byte[] getPayload() {
        boolean masked = isMasked();
        byte[] payload = Arrays.copyOfRange(b, startIndex, startIndex + length);
        if (!masked) return payload;
        byte[] mask = getMask();
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (payload[i] ^ mask[i % 4]);
        }
        return payload;
    }

    public String dump() {
        return ByteMagic.toHex(b);
    }

    public boolean isMultiFrame() {
        return b.length > startIndex + length;
    }

    public byte[] getOverhead() {
        return Arrays.copyOfRange(b, startIndex + length, b.length);
    }

    @Override
    public String toString() {
        return "WebsocketFrame{" +
                "length=" + length +
                ", startIndex=" + startIndex +
                ", payload=" + new String(getPayload(), StandardCharsets.UTF_8) +
                '}';
    }

    public static String getMessage(WebsocketFrame frame) throws IOException {
        String message;
        if (frame.getPayload().length >= 2 && Base64GZIP.isGzip(frame.getPayload())) {
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(frame.getPayload()))) {
                message = Core.read(gis).toString();
            }
        } else {
            message = new String(frame.getPayload(), StandardCharsets.UTF_8);
        }
        return message;
    }
}
