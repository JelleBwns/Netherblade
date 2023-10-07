package com.hawolt.http.proxy;

import com.hawolt.logger.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created: 25/08/2023 10:43
 * Author: Twitter @hawolt
 **/

public class Cookie {

    private final static SimpleDateFormat RFC_1123_DATE_TIME = new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss zzz", Locale.US);

    static {
        RFC_1123_DATE_TIME.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private final Map<String, String> map = new HashMap<>();
    private final List<String> switches = new ArrayList<>();
    private final String name, value, origin;

    public Cookie(String hostname, String source) {
        this.origin = hostname.split("/", 3)[2].split("/")[0];
        String[] data = source.split(";", 2);
        String[] cookie = data[0].split("=", 2);
        this.name = cookie[0];
        this.value = cookie[1];
        String[] args = data[1].split(";");
        for (String meta : args) {
            if (!meta.contains("=")) switches.add(meta.trim());
            else {
                String[] metadata = meta.trim().split("=");
                map.put(metadata[0], metadata[1]);
            }
        }
        if (map.get("domain") != null) return;
        String[] children = origin.split("\\.");
        map.put("domain", Arrays.stream(origin.split("\\.")).skip(children.length <= 2 ? 0 : 1).collect(Collectors.joining(".")));
    }

    public String get() {
        return String.join("=", name, value);
    }

    public boolean isNotExpired() {
        if (!map.containsKey("expires")) return true;
        try {
            Date date = RFC_1123_DATE_TIME.parse(map.get("expires"));
            return System.currentTimeMillis() < date.toInstant().toEpochMilli();
        } catch (ParseException e) {
            return false;
        } catch (NumberFormatException e) {
            Logger.warn("Failed to parse Cookie expires value: '{}', defaulting to true", map.get("expires"));
            return true;
        }
    }

    public boolean hasValue() {
        return value != null && !value.isEmpty();
    }

    public boolean isValidFor(String hostname) {
        return map.containsKey("domain") && hostname.endsWith(map.get("domain"));
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cookie cookie = (Cookie) o;
        return Objects.equals(map, cookie.map) && Objects.equals(switches, cookie.switches) && Objects.equals(name, cookie.name) && Objects.equals(value, cookie.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map, switches, name, value);
    }

    @Override
    public String toString() {
        return "Cookie{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", domain='" + map.get("domain") + '\'' +
                ", origin='" + origin + '\'' +
                '}';
    }
}
