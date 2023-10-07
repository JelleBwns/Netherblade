package com.hawolt.mitm.interpreter.impl;

import com.hawolt.http.LocalExecutor;
import com.hawolt.logger.Logger;
import com.hawolt.mitm.interpreter.AbstractInstruction;
import com.hawolt.util.StaticConstants;

public class SetupProxyInstruction extends AbstractInstruction {
    @Override
    protected String modify(String id, String[] args) throws Exception {
        String type = args[1];
        String target = args[2];
        int port = StaticConstants.PORT_MAPPING.get(type);
        String proxy = String.join(":", "http://127.0.0.1", String.valueOf(port));
        LocalExecutor.register(type, target, id);
        Logger.debug("[NETHERSCRIPT-PARSER] SETUP {} PROXY FOR {} ON {}", type, target, proxy);
        return proxy;
    }

    @Override
    protected int getArguments() {
        return 2;
    }

    @Override
    public String getName() {
        return "proxy";
    }
}
