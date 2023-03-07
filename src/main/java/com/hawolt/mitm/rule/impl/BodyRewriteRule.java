package com.hawolt.mitm.rule;

import com.hawolt.logger.Logger;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created: 22/11/2022 04:02
 * Author: Twitter @hawolt
 **/

public class RewriteRule implements IRewrite {
    private final String plain;
    private final Pattern target;
    private final String method;
    private final RuleType type;
    private List<ReplacementGroup> groups;
    private String replacement;
    private Pattern pattern;

    public RewriteRule(JSONObject object) {
        this.target = Pattern.compile(object.getString("url"));
        this.type = RuleType.find(object.getString("type"));
        this.plain = object.getString("find");
        this.method = object.getString("method");
        if (type != RuleType.REGEX) {
            this.replacement = object.getString("replace");
        } else {
            this.pattern = Pattern.compile(plain);
            this.groups = object.getJSONArray("groups")
                    .toList()
                    .stream()
                    .map(o -> (HashMap<?, ?>) o)
                    .map(JSONObject::new)
                    .map(ReplacementGroup::new)
                    .collect(Collectors.toList());
        }
    }

    public String getReplacement() {
        return replacement == null ? "SEE_GROUPS" : replacement;
    }

    public Pattern getTarget() {
        return target;
    }

    public String getMethod() {
        return method;
    }

    public String getPlain() {
        return plain;
    }

    public List<ReplacementGroup> getGroups() {
        return groups;
    }

    @Override
    public String rewrite(String in) {
        String modified = null;
        switch (type) {
            case PLAIN:
                if (!in.contains(plain)) break;
                modified = in.replaceAll(plain, replacement);
                Logger.debug("[{}-RULE] {} -> {}", type.name(), plain, getReplacement());
                break;
            case REGEX:
                Matcher matcher = pattern.matcher(in);
                StringBuilder builder = new StringBuilder(in);
                List<Replacement> list = new ArrayList<>();
                while (matcher.find()) {
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        for (ReplacementGroup group : getGroups()) {
                            if (!group.getGroups().contains(i)) continue;
                            String target = matcher.group(i);
                            if (target == null) continue;
                            int start = matcher.start(i);
                            list.add(new Replacement(start, start + target.length(), group.getReplacement()));
                        }
                    }
                }
                for (int i = list.size() - 1; i >= 0; i--) {
                    Replacement rule = list.get(i);
                    String target = builder.substring(rule.getStart(), rule.getEnd());
                    builder.replace(rule.getStart(), rule.getEnd(), rule.getReplacement());
                    Logger.error("[REWRITE] {} -> {}", target, rule.getReplacement());
                }
                in = builder.toString();
                Logger.debug("[{}-RULE] {}:{} -> {}", type.name(), plain, target, getReplacement());
                break;
            case UNKNOWN:
                break;
        }
        return modified == null ? in : modified;
    }
}