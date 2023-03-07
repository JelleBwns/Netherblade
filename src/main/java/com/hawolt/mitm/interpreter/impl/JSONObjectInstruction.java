package com.hawolt.mitm.interpreter.impl;

import com.hawolt.mitm.interpreter.AbstractInstruction;
import org.json.JSONObject;

import java.util.Arrays;

public class JSONObjectInstruction extends AbstractInstruction {
    @Override
    protected String modify(String[] args) throws Exception {
        String full = String.join("", Arrays.copyOfRange(args, 2, args.length));
        JSONObject object = new JSONObject(full);
        return object.get(args[1]).toString();
    }

    @Override
    protected int getArguments() {
        return 1;
    }

    @Override
    public String getName() {
        return "jsonobject";
    }
}
