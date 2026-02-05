package com.thebrandolorian.counterweight.utils;

import com.thebrandolorian.counterweight.CounterweightPlugin;

public class DebugUtils {
    private DebugUtils() { }

    public static void logInfo(String message, Object... args) { CounterweightPlugin.get().getLogger().atInfo().log(message, args); }
    public static void logWarn(String message, Object... args) { CounterweightPlugin.get().getLogger().atWarning().log(message, args); }
    public static void logError(String message, Object... args) { CounterweightPlugin.get().getLogger().atSevere().log(message, args); }

}
