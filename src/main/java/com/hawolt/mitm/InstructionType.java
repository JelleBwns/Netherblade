package com.hawolt.mitm;

/**
 * Created: 22/11/2022 03:39
 * Author: Twitter @hawolt
 **/

public enum InstructionType {
    URL, QUERY, HEADER, BODY, CODE, UNKNOWN, RMS, RTMP;

    private static final InstructionType[] INSTRUCTION_TYPES = InstructionType.values();

    public static InstructionType find(String in) {
        for (InstructionType type : INSTRUCTION_TYPES) {
            if (type.name().equalsIgnoreCase(in)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
