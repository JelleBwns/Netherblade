package com.hawolt.mitm.interpreter.impl;

import com.hawolt.mitm.interpreter.AbstractInstruction;

public class IntegerMathInstruction extends AbstractInstruction {

    @Override
    protected String modify(String id, String[] args) {
        int val1 = Integer.parseInt(args[1]);
        String operator = args[2];
        int val2 = Integer.parseInt(args[3]);
        switch (operator) {
            case "+":
                return String.valueOf(val1 + val2);
            case "-":
                return String.valueOf(val1 - val2);
            case "*":
                return String.valueOf(val1 * val2);
            case "/":
                return String.valueOf(val1 / val2);
        }
        return "0";
    }

    @Override
    protected int getArguments() {
        return 3;
    }

    @Override
    public String getName() {
        return "mint";
    }

}
