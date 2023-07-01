package com.hawolt.mitm.rule;

import com.hawolt.http.proxy.IRequest;
import org.json.JSONObject;

import java.util.regex.Pattern;

/**
 * Created: 06/03/2023 15:26
 * Author: Twitter @hawolt
 **/

public abstract class AbstractHttpRewriteRule<T, S> extends AbstractRewriteRule<T, S> {

    protected final Pattern target;
    protected final String method, url;

    public AbstractHttpRewriteRule(JSONObject o) {
        super(o);
        this.url = o.getString("url");
        this.method = o.getString("method");
        this.target = Pattern.compile(this.url);
    }

    public Pattern getTarget() {
        return target;
    }

    public String getMethod() {
        return method;
    }

    public boolean isTargeted(IRequest request) {
        if (!getTarget().matcher(request.url()).matches()) return false;
        return getMethod().equals(request.method()) || getMethod().equals("*");
    }
}
