package com.hawolt.mitm;

import com.hawolt.http.proxy.IRequest;
import com.hawolt.logger.Logger;
import com.hawolt.mitm.rule.AbstractRewriteRule;
import com.hawolt.mitm.rule.IRewrite;
import com.hawolt.mitm.rule.RuleType;
import com.hawolt.mitm.rule.impl.*;
import com.hawolt.socket.rms.WebsocketFrame;
import com.hawolt.util.Pair;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created: 22/11/2022 03:46
 * Author: Twitter @hawolt
 **/

public abstract class RewriteModule<T extends IRequest> {
    protected Map<InstructionType, List<IRewrite<?, ?>>> map = new HashMap<>();

    public void supply(Map<InstructionType, List<IRewrite<?, ?>>> map) {
        this.map.clear();
        for (InstructionType type : map.keySet()) {
            Logger.debug("RewriteModule [{}]: {} rules loaded", type.name(), map.get(type).size());
        }
        this.map = map;
    }

    public void inject(InstructionType type, IRewrite<?, ?> rule) {
        Logger.debug("RewriteModule [{}]: rule injected", type.name());
        map.get(type).add(rule);
    }

    public WebsocketFrame rewriteRMS(String id, WebsocketFrame frame) {
        List<IRewrite<?, ?>> rules = map.get(InstructionType.RMS);
        if (rules == null) return frame;
        for (IRewrite<?, ?> rule : rules) {
            if (frame == null) continue;
            frame = rewriteRMS(id, frame, Unsafe.cast(rule));
        }
        return frame;
    }

    public byte[] rewriteRTMP(String id, byte[] bytes, JSONObject o) {
        List<IRewrite<?, ?>> rules = map.get(InstructionType.RTMP);
        if (rules == null) return bytes;
        for (IRewrite<?, ?> rule : rules) {
            if (o == null) continue;
            bytes = rewriteRTMP(id, o, Unsafe.cast(rule));
        }
        return bytes;
    }

    public T rewrite(String id, T communication) {
        for (InstructionType type : map.keySet()) {
            if (type == InstructionType.RMS) continue;
            List<IRewrite<?, ?>> rules = map.get(type);
            for (IRewrite<?, ?> rule : rules) {
                if (!rule.isTargeted(communication)) continue;
                Logger.debug("Matching url for {} rule [{}] {} ", type.name(), communication.method(), communication.url());
                switch (type) {
                    case CODE:
                        communication = rewriteCode(id, communication, Unsafe.cast(rule));
                        break;
                    case URL:
                        communication = rewriteURL(id, communication, Unsafe.cast(rule));
                        break;
                    case QUERY:
                        communication = rewriteQuery(id, communication, Unsafe.cast(rule));
                        break;
                    case HEADER:
                        communication = rewriteHeaders(id, communication, Unsafe.cast(rule));
                        break;
                    case BODY:
                        communication = rewriteBody(id, communication, Unsafe.cast(rule));
                        break;
                }
            }
        }
        return communication;
    }

    private byte[] rewriteRTMP(String id, JSONObject communication, RealTimeMessagingProtocolRule rule) {
        return rule.rewrite(id, communication);
    }

    private WebsocketFrame rewriteRMS(String id, WebsocketFrame communication, RiotMessagingServiceRule rule) {
        return rule.rewrite(id, communication);
    }

    private T rewriteBody(String id, T communication, BodyRewriteRule rule) {
        String body = communication.getBody();
        body = rule.rewrite(id, body);
        communication.setBody(body);
        return communication;
    }

    private T rewriteHeaders(String id, T communication, HeaderRewriteRule rule) {
        Pair<String, String> result = rule.rewrite(id, communication.getHeaders());
        if (result == null && rule.getType() == RuleType.REMOVE) communication.removeHeader(rule.getKey());
        else if (result != null) communication.addHeader(result.getKey(), result.getValue());
        return communication;
    }

    protected abstract T rewriteQuery(String id, T communication, AbstractRewriteRule<?, ?> rule);

    protected abstract T rewriteURL(String id, T communication, AbstractRewriteRule<?, ?> rule);

    protected abstract T rewriteCode(String id, T communication, CodeRewriteRule rule);

}
