package com.hawolt.mitm.rule.impl;

import com.hawolt.http.proxy.IRequest;
import com.hawolt.mitm.rule.AbstractRewriteRule;
import org.json.JSONObject;

/**
 * Created: 22/11/2022 04:02
 * Author: Twitter @hawolt
 **/

public class RealTimeMessagingProtocolRule extends AbstractRewriteRule<JSONObject, byte[]> {
    private final String plain;

    public RealTimeMessagingProtocolRule(JSONObject object) {
        super(object);
        this.plain = object.getString("find");
    }

    @Override
    public byte[] rewrite(String id, JSONObject o) {
        return o.toString().contains(plain) ? null : new byte[0];
    }

    @Override
    public boolean isTargeted(IRequest request) {
        return true;
    }

}
