package com.hawolt.mitm.rule.impl;

import com.hawolt.logger.Logger;
import com.hawolt.mitm.rule.AbstractHttpRewriteRule;
import com.hawolt.mitm.rule.AbstractRewriteRule;
import org.json.JSONObject;

/**
 * Created: 06/03/2023 15:19
 * Author: Twitter @hawolt
 **/

public class CodeRewriteRule extends AbstractHttpRewriteRule<Integer, Integer> {
    private final int code;

    public CodeRewriteRule(JSONObject o) {
        super(o);
        this.code = o.getInt("code");
    }

    @Override
    public Integer rewrite(String id, Integer in) {
        Logger.debug("Rewriting code from {} to {} for {}", in, code, url);
        return code;
    }
}
