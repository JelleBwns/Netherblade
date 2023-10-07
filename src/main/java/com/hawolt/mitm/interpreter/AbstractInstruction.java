package com.hawolt.mitm.interpreter;

public abstract class AbstractInstruction implements Instruction {

    @Override
    public String manipulate(String id, String[] args) throws Exception {
        if (args.length < getArguments()) return "BAD_ARG_AMOUNT";
        return modify(id, args);
    }

    protected abstract String modify(String id, String[] args) throws Exception;

    protected abstract int getArguments();
}
