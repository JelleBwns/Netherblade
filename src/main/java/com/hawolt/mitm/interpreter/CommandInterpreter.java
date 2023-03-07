package com.hawolt.mitm.interpreter;

import com.hawolt.mitm.interpreter.impl.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CommandInterpreter {

    private static final Map<String, Instruction> INSTRUCTION_MAP = new HashMap<>();

    static {
        INSTRUCTION_MAP.put("jsonobject", new JSONObjectInstruction());
        INSTRUCTION_MAP.put("strlen", new StringLengthInstruction());
        INSTRUCTION_MAP.put("substring", new SubstringInstruction());
        INSTRUCTION_MAP.put("mint", new IntegerMathInstruction());
        INSTRUCTION_MAP.put("declare", new DeclareInstruction());
        INSTRUCTION_MAP.put("var", new VariableInstruction());
        INSTRUCTION_MAP.put("lcu", new LCUInstruction());
    }

    public static String parse(String in) throws Exception {
        return parse(in, null);
    }

    public static String parse(String in, String port) throws Exception {
        int[] occurrences = new int[0];
        int index = -1;
        while ((index = in.indexOf('$', index + 1)) != -1) {
            occurrences = Arrays.copyOf(occurrences, occurrences.length + 1);
            occurrences[occurrences.length - 1] = index;
        }
        StringBuilder builder = new StringBuilder(in);
        Map<String, String> map = new HashMap<>();
        if (port != null) map.put("port", port);
        for (int i = occurrences.length - 1; i >= 0; i--) {
            int start = builder.indexOf("(", occurrences[i] + 1);
            if (start == -1) continue;
            int end = builder.indexOf(")", start);
            String command = builder.substring(start + 1, end);
            String[] args = command.split(" ");
            if (INSTRUCTION_MAP.containsKey(args[0])) {
                Instruction instruction = INSTRUCTION_MAP.get(args[0]);
                String result = instruction.manipulate(args);
                if (instruction instanceof VariableInstruction) {
                    boolean available = map.containsKey(args[1]);
                    if (!available) continue;
                    builder.replace(occurrences[i], end + 1, map.get(args[1]));
                } else if (instruction instanceof DeclareInstruction) {
                    String[] variable = result.split(" ", 2);
                    map.put(variable[0], variable[1]);
                    builder.replace(occurrences[i], end + 1, "");
                } else {
                    builder.replace(occurrences[i], end + 1, result);
                }
            }
        }
        return builder.toString().trim();
    }
}
