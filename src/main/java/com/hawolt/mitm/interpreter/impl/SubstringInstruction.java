package com.hawolt.mitm.interpreter.impl;

import com.hawolt.mitm.interpreter.AbstractInstruction;

public class SubstringInstruction extends AbstractInstruction {

    @Override
    protected String modify(String id, String[] args) {
        return args[1].substring(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
    }

    @Override
    protected int getArguments() {
        return 3;
    }

    @Override
    public String getName() {
        return "substring";
    }

}
