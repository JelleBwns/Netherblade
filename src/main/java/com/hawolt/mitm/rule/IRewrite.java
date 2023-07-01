package com.hawolt.mitm.rule;

import com.hawolt.http.proxy.IRequest;

/**
 * Created: 22/11/2022 04:24
 * Author: Twitter @hawolt
 **/

public interface IRewrite<T, S> {
    S rewrite(T in);

    RuleType getType();

    boolean isTargeted(IRequest request);
}
