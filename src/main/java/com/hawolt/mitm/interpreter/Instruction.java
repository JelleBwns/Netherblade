package com.hawolt.mitm.interpreter;

public interface Instruction {
    String getName();

    String manipulate(String id, String[] args) throws Exception;
}
