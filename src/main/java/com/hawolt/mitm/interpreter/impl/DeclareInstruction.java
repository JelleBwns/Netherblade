package com.hawolt.mitm.interpreter.impl;

import com.hawolt.mitm.interpreter.AbstractInstruction;

import java.util.Arrays;
import java.util.stream.Collectors;

public class DeclareInstruction extends AbstractInstruction {

    @Override
    protected String modify(String id, String[] args) {
        return String.join(" ", args[1], Arrays.stream(args).skip(2).collect(Collectors.joining(" ")));
    }

    @Override
    protected int getArguments() {
        return 2;
    }

    @Override
    public String getName() {
        return "declare";
    }
}
