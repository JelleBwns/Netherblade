package com.hawolt.mitm.interpreter.impl;

import com.hawolt.mitm.interpreter.AbstractInstruction;

public class StringLengthInstruction extends AbstractInstruction {

    @Override
    protected String modify(String id, String[] args) {
        return String.valueOf(args[1].length());
    }

    @Override
    protected int getArguments() {
        return 1;
    }

    @Override
    public String getName() {
        return "strlen";
    }

}
