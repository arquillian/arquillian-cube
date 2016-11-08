package org.arquillian.cube.docker.impl.client.utils;


import org.arquillian.cube.docker.impl.util.NumberType;

public class NumberConversion {

    public static String humanReadableByteCount(Long bytes, boolean decimal) {
        if (bytes == null) bytes = 0L;
        String sign = bytes < 0 ? "-" : "";
        Long absBytes = Math.abs(bytes);
        int unit = decimal ? 1000 : 1024;
        if (absBytes < unit) {
            return sign + absBytes + " B";
        }
        int exp = (int) (Math.log(absBytes) / Math.log(unit));
        String pre = (decimal ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (decimal ? "" : "i");

        return  String.format("%s %.2f %sB", sign, absBytes / Math.pow(unit, exp), pre).trim();
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
