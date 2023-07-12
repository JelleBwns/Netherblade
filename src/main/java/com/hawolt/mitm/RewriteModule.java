package com.hawolt.mitm;

import com.hawolt.http.proxy.IRequest;
import com.hawolt.logger.Logger;
import com.hawolt.mitm.rule.AbstractRewriteRule;
import com.hawolt.mitm.rule.IRewrite;
import com.hawolt.mitm.rule.RuleType;
import com.hawolt.mitm.rule.impl.BodyRewriteRule;
import com.hawolt.mitm.rule.impl.CodeRewriteRule;
import com.hawolt.mitm.rule.impl.HeaderRewriteRule;
import com.hawolt.mitm.rule.impl.RiotMessagingServiceRule;
import com.hawolt.socket.rms.WebsocketFrame;
import com.hawolt.util.Pair;

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

    public WebsocketFrame rewriteRMS(WebsocketFrame frame) {
        List<IRewrite<?, ?>> rules = map.get(InstructionType.RMS);
        for (IRewrite<?, ?> rule : rules) {
            if (frame == null) continue;
            frame = rewriteRMS(frame, Unsafe.cast(rule));
        }
        return frame;
    }

    public T rewrite(T communication) {
        for (InstructionType type : map.keySet()) {
            if (type == InstructionType.RMS) continue;
            List<IRewrite<?, ?>> rules = map.get(type);
            for (IRewrite<?, ?> rule : rules) {
                if (!rule.isTargeted(communication)) continue;
                Logger.debug("Matching url for {} rule [{}] {} ", type.name(), communication.method(), communication.url());
                switch (type) {
                    case CODE:
                        communication = rewriteCode(communication, Unsafe.cast(rule));
                        break;
                    case URL:
                        communication = rewriteURL(communication, Unsafe.cast(rule));
                        break;
                    case QUERY:
                        communication = rewriteQuery(communication, Unsafe.cast(rule));
                        break;
                    case HEADER:
                        communication = rewriteHeaders(communication, Unsafe.cast(rule));
                        break;
                    case BODY:
                        communication = rewriteBody(communication, Unsafe.cast(rule));
                        break;
                }
            }
        }
        return communication;
    }

    private WebsocketFrame rewriteRMS(WebsocketFrame communication, RiotMessagingServiceRule rule) {
        return rule.rewrite(communication);
    }

    private T rewriteBody(T communication, BodyRewriteRule rule) {
        String body = communication.getBody();
        body = rule.rewrite(body);
        communication.setBody(body);
        return communication;
    }

    private T rewriteHeaders(T communication, HeaderRewriteRule rule) {
        Pair<String, String> result = rule.rewrite(communication.getHeaders());
        if (result == null && rule.getType() == RuleType.REMOVE) communication.removeHeader(rule.getKey());
        else if (result != null) communication.addHeader(result.getKey(), result.getValue());
        return communication;
    }

    protected abstract T rewriteQuery(T communication, AbstractRewriteRule<?, ?> rule);

    protected abstract T rewriteURL(T communication, AbstractRewriteRule<?, ?> rule);

    protected abstract T rewriteCode(T communication, CodeRewriteRule rule);

}
