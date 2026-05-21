package com.gemcasemod;

public final class GemFormatUtil {
    private static final java.text.DecimalFormat FORMAT = new java.text.DecimalFormat("##.#");

    private GemFormatUtil() {}

    public static String formatCount(int n) {
        int log = (int) StrictMath.log10(n);
        if (log <= 3) {
            return String.valueOf(n);
        } else if (log <= 6) {
            return FORMAT.format(n / 1000D) + "K";
        } else if (log <= 8) {
            return FORMAT.format(n / 1000000D) + "M";
        }
        return FORMAT.format(n / 1000000000D) + "B";
    }
}
