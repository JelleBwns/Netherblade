package com.hawolt.mitm.interpreter.impl;

import com.hawolt.logger.Logger;
import com.hawolt.mitm.interpreter.AbstractInstruction;

import java.util.Arrays;

public class ReplaceStringInstruction extends AbstractInstruction {
    @Override
    protected String modify(String[] args) throws Exception {
        String full = String.join("", Arrays.copyOfRange(args, 3, args.length));
        String original = args[1];
        String replacement = args[2];
        Logger.debug("[NETHERSCRIPT-PARSER] REPLACE {} WITH {}", original, replacement);
        return full.replace(original, replacement);
    }

    @Override
    protected int getArguments() {
        return 3;
    }

    @Override
    public String getName() {
        return "replace";
    }
}
