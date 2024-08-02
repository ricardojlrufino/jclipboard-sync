package com.github.ricardojlrufino.clipsync.clipboard;

public class ClipboardType {
    public static final int STRING = 1;
    public static final int PICTURE = 2;
    public static final int HTML = 3;
    public static final int FILE = 4;

    public static String describe(int type){
        return switch (type) {
            case STRING -> "STRING";
            case PICTURE -> "PICTURE";
            case HTML -> "HTML";
            case FILE -> "FILE";
            default -> throw new IllegalStateException("Invalid type type");
        };
    }

}
