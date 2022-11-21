package com.hawolt.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created: 31/07/2022 00:21
 * Author: Twitter @hawolt
 **/

public class StaticConstants {

    public static final Map<String, Integer> PORT_MAPPING = new HashMap<String, Integer>() {{
        put("entitlement", 35202);
        put("platform", 35205);
        put("config", 35200);
        put("email", 35201);
        put("queue", 35203);
        put("ledge", 35204);
    }};

    public static final String RIOT_GAMES = "Riot Games";
    public static final String LEAGUE_OF_LEGENDS = "League of Legends";

    public static final String RIOT_INSTALLS_JSON = "RiotClientInstalls.json";
    public static final String SYSTEM_YAML = "system.yaml";
}
