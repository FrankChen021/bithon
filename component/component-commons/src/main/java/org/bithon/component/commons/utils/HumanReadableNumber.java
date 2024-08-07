/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.component.commons.utils;

public class HumanReadableNumber extends Number {
    private final long value;
    private final String text;

    private HumanReadableNumber(String value) {
        this.value = HumanReadableNumber.parse(value);
        this.text = value.trim();
    }

    public static HumanReadableNumber of(String value) {
        return new HumanReadableNumber(value);
    }

    /**
     * parse the CASE-SENSITIVE string number, which is either:
     * <p>
     * a number string
     * <p>
     * or
     * <p>
     * a number string with a suffix which indicates the unit the of number
     * the unit must be one of following
     * K - kilobyte = 1000
     * M - megabyte = 1,000,000
     * G - gigabyte = 1,000,000,000
     * T - terabyte = 1,000,000,000,000
     * P - petabyte = 1,000,000,000,000,000
     * KiB - kilo binary byte = 1024
     * MiB - mega binary byte = 1024*1204
     * GiB - giga binary byte = 1024*1024*1024
     * TiB - tera binary byte = 1024*1024*1024*1024
     * PiB - peta binary byte = 1024*1024*1024*1024*1024
     * <p>
     *
     * @return nullValue if input is null or empty
     * value of number
     * @throws IAE if the input is invalid
     */
    public static long parse(String number) {
        if (number == null) {
            throw new IAE("Invalid format of number: number is null");
        }

        number = number.trim();
        if (number.isEmpty()) {
            throw new IAE("Invalid format of number: number is blank");
        }

        return parseInner(number);
    }

    private static long parseInner(String number) {
        if (number.charAt(0) == '-') {
            throw new IAE("Invalid format of number: %s. Negative value is not allowed.", number);
        }

        int lastDigitIndex = number.length() - 1;
        boolean isBinaryByte = false;
        char unit = number.charAt(lastDigitIndex--);
        if (unit == 'B') {
            // unit ends with 'b' must be a format of KiB/MiB/GiB/TiB/PiB, so at least 3 extra characters are required
            if (lastDigitIndex < 2) {
                throw new IAE("Invalid format of number: %s", number);
            }
            if (number.charAt(lastDigitIndex--) != 'i') {
                throw new IAE("Invalid format of number: %s", number);
            }

            unit = number.charAt(lastDigitIndex--);
            isBinaryByte = true;
        } else if (unit == 'i') {
            // in the format of 5Gi
            unit = number.charAt(lastDigitIndex--);
            isBinaryByte = true;
        }

        long base = 1;
        switch (unit) {
            case 'K':
                base = isBinaryByte ? 1024 : 1_000;
                break;

            case 'M':
                base = isBinaryByte ? 1024 * 1024 : 1_000_000;
                break;

            case 'G':
                base = isBinaryByte ? 1024 * 1024 * 1024 : 1_000_000_000;
                break;

            case 'T':
                base = isBinaryByte ? 1024L * 1024 * 1024 * 1024 : 1_000_000_000_000L;
                break;

            case 'P':
                base = isBinaryByte ? 1024L * 1024 * 1024 * 1024 * 1024 : 1_000_000_000_000_000L;
                break;

            default:
                if (!Character.isDigit(unit)) {
                    throw new IAE("Unrecognizable unit [%c] in the number: %s", unit, number);
                }

                //lastDigitIndex here holds the index which is prior to the current digit
                //move backward so that it's at the right place
                lastDigitIndex++;
                break;
        }

        try {
            long value = Long.parseLong(number.substring(0, lastDigitIndex + 1)) * base;
            if (base > 1 && value < base) {
                //for base == 1, overflow has been checked in parseLong
                throw new IAE("Number overflow: %s", number);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IAE("Invalid format or out of range of long: %s", number);
        }
    }

    /**
     * Returns a human-readable string version of input value
     *
     * @param bytes      input value. Negative value is also allowed
     * @param precision  [0,3]
     * @param unitSystem which unit system is adopted to format the input value, see {@link UnitSystem}
     */
    public static String format(long bytes, long precision, UnitSystem unitSystem) {
        if (precision < 0 || precision > 3) {
            throw new IAE("precision [%d] must be in the range of [0,3]", precision);
        }

        String pattern = "%." + precision + "f %s%s";
        switch (unitSystem) {
            case BINARY_BYTE:
                return BinaryFormatter.format(bytes, pattern, "B");
            case DECIMAL_BYTE:
                return DecimalFormatter.format(bytes, pattern, "B");
            case DECIMAL:
                return DecimalFormatter.format(bytes, pattern, "").trim();
            default:
                throw new IAE("Unknown unit system[%s]", unitSystem);
        }
    }

    public long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object thatObj) {
        if (thatObj == null) {
            return false;
        }
        if (thatObj instanceof HumanReadableNumber) {
            return value == ((HumanReadableNumber) thatObj).value;
        } else {
            if (thatObj instanceof Number) {
                return ((Number) thatObj).longValue() == value;
            }
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    @Override
    public String toString() {
        return this.text;
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    public enum UnitSystem {
        /**
         * also known as IEC format
         * e.g. B, KiB, MiB, GiB ...
         */
        BINARY_BYTE,

        /**
         * number
         * also known as SI format
         * e.g. B, KB, MB ...
         */
        DECIMAL_BYTE,

        /**
         * simplified SI format without 'B' indicator
         * e.g., K, M, G ...
         */
        DECIMAL
    }

    public static class IAE extends RuntimeException {
        public IAE(String format, Object... args) {
            super(StringUtils.format(format, args));
        }
    }

    static class BinaryFormatter {
        private static final String[] UNITS = {"", "Ki", "Mi", "Gi", "Ti", "Pi", "Ei"};

        static String format(long bytes, String pattern, String suffix) {
            if (bytes > -1024 && bytes < 1024) {
                return bytes + " " + suffix;
            }

            if (bytes == Long.MIN_VALUE) {
                /*
                 * special path for Long.MIN_VALUE
                 *
                 * Long.MIN_VALUE = 2^63 = (2^60=1EiB) * 2^3
                 */
                return StringUtils.format(pattern, -8.0, UNITS[UNITS.length - 1], suffix);
            }

            /**
             * A number and its binary bits are listed as fellows
             * [0,    1KiB) = [0,    2^10)
             * [1KiB, 1MiB) = [2^10, 2^20),
             * [1MiB, 1GiB) = [2^20, 2^30),
             * [1GiB, 1PiB) = [2^30, 2^40),
             * ...
             * <p>
             * So, expression (63 - Long.numberOfLeadingZeros(absValue))) helps us to get the right number of bits of the given input
             *
             * Internal implementation of Long.numberOfLeadingZeros uses bit operations to do calculation so the cost is very low
             */
            int unitIndex = (63 - Long.numberOfLeadingZeros(Math.abs(bytes))) / 10;
            return StringUtils.format(pattern, (double) bytes / (1L << (unitIndex * 10)), UNITS[unitIndex], suffix);
        }
    }

    static class DecimalFormatter {
        private static final String[] UNITS = {"K", "M", "G", "T", "P", "E"};

        static String format(long bytes, String pattern, String suffix) {
            /*
             * handle number between (-1000, 1000) first to simply further processing
             */
            if (bytes > -1000 && bytes < 1000) {
                return bytes + " " + suffix;
            }

            /**
             * because max precision is 3, extra fractions can be ignored by use of integer division which might be a little more efficient
             */
            int unitIndex = 0;
            while (bytes <= -1000_000 || bytes >= 1000_000) {
                bytes /= 1000;
                unitIndex++;
            }
            return StringUtils.format(pattern, bytes / 1000.0, UNITS[unitIndex], suffix);
        }
    }
}
