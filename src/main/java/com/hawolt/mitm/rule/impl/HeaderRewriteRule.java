package com.hawolt.mitm.rule.impl;

import com.hawolt.logger.Logger;
import com.hawolt.mitm.interpreter.CommandInterpreter;
import com.hawolt.mitm.rule.AbstractHttpRewriteRule;
import com.hawolt.mitm.rule.AbstractRewriteRule;
import com.hawolt.util.Pair;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Created: 06/03/2023 15:19
 * Author: Twitter @hawolt
 **/

public class HeaderRewriteRule extends AbstractHttpRewriteRule<Map<String, List<String>>, Pair<String, String>> {
    private final String key, value;

    public HeaderRewriteRule(JSONObject o) {
        super(o);
        this.key = o.getString("key");
        this.value = o.getString("value");
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public Pair<String, String> rewrite(String id, Map<String, List<String>> in) {
        switch (type) {
            case MODIFY:
            case ADD:
                List<String> list = in.get("Origin");
                String value = list == null || list.isEmpty() ? "" : list.get(0);
                String[] values = value.split(":");
                String port = values.length >= 3 ? values[2] : null;
                try {
                    return Pair.from(key, CommandInterpreter.parse(this.value, port));
                } catch (Exception e) {
                    Logger.error("Failed to add header for " + url);
                    //  Logger.error(e);
                    return null;
                }
            default:
                return null;
        }
    }
}
