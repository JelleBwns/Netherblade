package com.hawolt.mitm.rule;

import org.json.JSONObject;

/**
 * Created: 06/03/2023 15:26
 * Author: Twitter @hawolt
 **/

public abstract class AbstractRewriteRule<T, S> implements IRewrite<T, S> {

    protected final RuleType type;

    public AbstractRewriteRule(JSONObject o) {
        this.type = RuleType.find(o.getString("type"));
    }


    @Override
    public RuleType getType() {
        return type;
    }
}
