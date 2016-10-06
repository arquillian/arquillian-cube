package org.arquillian.cube.docker.impl.client.utils;


import org.arquillian.cube.docker.impl.util.NumberType;

public class NumberConversion {

    public static String humanReadableByteCount(Long bytes, boolean decimal) {
        int unit = decimal ? 1000 : 1024;
        if (bytes == null) bytes = 0L;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (decimal ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (decimal ? "" : "i");

        return String.format("%.2f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static long convertToLong(Object number) {
        long longNumber = 0;
        if (number != null) {
            NumberType type = NumberType.valueOf(number.getClass().getSimpleName().toUpperCase());

            switch (type) {
                case BYTE:
                    longNumber = ((Byte) number).longValue();
                    break;
                case SHORT:
                    longNumber = ((Short) number).longValue();
                    break;
                case INTEGER:
                    longNumber = ((Integer) number).longValue();
                    break;
                case LONG:
                    longNumber = (long) number;
                    break;
            }
        }
        return longNumber;
    }
}
