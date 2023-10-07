package com.hawolt.mitm.impl;

import com.hawolt.http.proxy.ProxyRequest;
import com.hawolt.mitm.RewriteModule;
import com.hawolt.mitm.rule.AbstractRewriteRule;
import com.hawolt.mitm.rule.impl.CodeRewriteRule;

/**
 * Created: 22/11/2022 04:01
 * Author: Twitter @hawolt
 **/

public class RequestModule extends RewriteModule<ProxyRequest> {

    @Override
    protected ProxyRequest rewriteQuery(String id, ProxyRequest communication, AbstractRewriteRule<?, ?> rule) {
        return communication;
    }

    @Override
    protected ProxyRequest rewriteURL(String id, ProxyRequest communication, AbstractRewriteRule<?, ?> rule) {
        return communication;
    }

    @Override
    protected ProxyRequest rewriteCode(String id, ProxyRequest communication, CodeRewriteRule rule) {
        return communication;
    }
}
