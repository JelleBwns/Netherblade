package com.hawolt.mitm.rule;

import com.hawolt.io.Core;
import com.hawolt.mitm.CommunicationType;
import com.hawolt.mitm.InstructionType;
import com.hawolt.mitm.RewriteModule;
import com.hawolt.mitm.RuleInjector;
import com.hawolt.mitm.impl.RequestModule;
import com.hawolt.mitm.impl.ResponseModule;
import com.hawolt.mitm.rule.impl.BodyRewriteRule;
import com.hawolt.mitm.rule.impl.CodeRewriteRule;
import com.hawolt.mitm.rule.impl.HeaderRewriteRule;
import com.hawolt.ui.SocketServer;
import com.hawolt.util.RunLevel;
import io.javalin.http.Handler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Created: 22/11/2022 04:33
 * Author: Twitter @hawolt
 **/

public class RuleInterpreter {
    public final static Map<CommunicationType, RewriteModule<?>> map = new HashMap<CommunicationType, RewriteModule<?>>() {{
        put(CommunicationType.INGOING, new ResponseModule());
        put(CommunicationType.OUTGOING, new RequestModule());
    }};
    private final static Map<InstructionType, Function<JSONObject, IRewrite<?, ?>>> converter = new HashMap<InstructionType, Function<JSONObject, IRewrite<?, ?>>>() {{
        put(InstructionType.HEADER, HeaderRewriteRule::new);
        put(InstructionType.BODY, BodyRewriteRule::new);
        put(InstructionType.CODE, CodeRewriteRule::new);
    }};
    public static final Handler RELOAD = context -> {
        reload(context.queryParam("file"));
    };

    public static void reload(String file) throws IOException {
        try {
            String filename = file == null ? "instructions.json" : file;
            JSONObject object = new JSONObject(Core.read(Core.getFileAsStream(Paths.get(filename))).toString());
            SocketServer.forward(new JSONObject().put("type", "rule").put("data", object).toString());
            for (String first : object.keySet()) {
                CommunicationType communicationType = CommunicationType.find(first);
                if (communicationType == CommunicationType.UNKNOWN) continue;
                RuleInterpreter.map.get(communicationType).supply(interpret(first, object));
            }
        } catch (FileNotFoundException e) {
            System.err.println("instructions.json not present.");
        }
        RuleInjector.load(RunLevel.get("inject.json"));
    }

    public static Map<InstructionType, List<IRewrite<?, ?>>> interpret(String first, JSONObject object) {
        Map<InstructionType, List<IRewrite<?, ?>>> map = new HashMap<>();
        JSONObject communicationTypeJson = object.getJSONObject(first);
        for (String second : communicationTypeJson.keySet()) {
            InstructionType instructionType = InstructionType.find(second);
            if (instructionType == InstructionType.UNKNOWN || !converter.containsKey(instructionType)) continue;
            if (!map.containsKey(instructionType)) map.put(instructionType, new ArrayList<>());
            JSONArray rules = communicationTypeJson.getJSONArray(second);
            for (int i = 0; i < rules.length(); i++) {
                map.get(instructionType).add(converter.get(instructionType).apply(rules.getJSONObject(i)));
            }
        }
        return map;
    }
}
