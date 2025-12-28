package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileSizeUtils {
    private static final Pattern SIZE = Pattern.compile("^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*([a-zA-Z]*)\\s*$");
    private static final Map<String, Long> MULTIPLIER = Map.ofEntries(
            Map.entry("", 1L),
            Map.entry("B", 1L),

            // Decimal
            Map.entry("KB", 1000L),
            Map.entry("MB", 1000L * 1000),
            Map.entry("GB", 1000L * 1000 * 1000),
            Map.entry("TB", 1000L * 1000 * 1000 * 1000),

            // Binary
            Map.entry("KIB", 1024L),
            Map.entry("MIB", 1024L * 1024),
            Map.entry("GIB", 1024L * 1024 * 1024),
            Map.entry("TIB", 1024L * 1024 * 1024 * 1024));

    private FileSizeUtils() {}

    public static long convertValMaxToInt(String input) {
        // First validate string
        if (input == null)
            throw new IllegalArgumentException("Value for Max File Size is null");

        Matcher m = SIZE.matcher(input);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid size: " + input);
        }
        BigDecimal value = new BigDecimal(m.group(1));
        String unitRaw = m.group(2) == null ? "" : m.group(2);
        String unit = unitRaw.toUpperCase();

        // if (unit.length() == 1) unit = unit + "B"; // for people using K instead of KB
        Long mul = MULTIPLIER.get(unit);
        if (mul == null) {
            throw new IllegalArgumentException("Unkown Unit: " + unitRaw);
        }
        BigDecimal bytes = value.multiply(BigDecimal.valueOf(mul));
        return bytes.longValueExact();
    }
}
