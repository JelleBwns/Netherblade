package com.hawolt.mitm.interpreter.impl;

import com.hawolt.mitm.interpreter.AbstractInstruction;

public class VariableInstruction extends AbstractInstruction {

    @Override
    protected String modify(String id, String[] args) {
        return null;
    }

    @Override
    protected int getArguments() {
        return 2;
    }

    @Override
    public String getName() {
        return "var";
    }

}
