package com.github.ricardojlrufino.clipsync.utils;

import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class MyFormatter extends SimpleFormatter {
    private String format = "[%1$tF %1$tT.%1$tL] [%2$-7s] %3$s %n";

    @Override
    public synchronized String format(LogRecord lr) {
        return String.format(format, new Date(lr.getMillis()), lr.getLevel().getLocalizedName(),
                lr.getMessage());
    }
}