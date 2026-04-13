package com.peakskills;

/**
 * Colored console logger for PeakSkills.
 * Bright green — stands out from vanilla server noise.
 */
public final class PeakLog {

    private static final String I = "\u001B[1;92m[PeakSkills]\u001B[0m ";
    private static final String W = "\u001B[1;93m[PeakSkills]\u001B[0m ";
    private static final String E = "\u001B[1;91m[PeakSkills]\u001B[0m ";

    private PeakLog() {}

    public static void info(String msg, Object... args) {
        PeakSkills.LOGGER.info(I + msg, args);
    }

    public static void warn(String msg, Object... args) {
        PeakSkills.LOGGER.warn(W + msg, args);
    }

    public static void error(String msg, Object... args) {
        PeakSkills.LOGGER.error(E + msg, args);
    }

    public static void error(String msg, Throwable t) {
        PeakSkills.LOGGER.error(E + msg, t);
    }
}
