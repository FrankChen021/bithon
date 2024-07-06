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

package org.bithon.component.commons.uuid;

/**
 * @author frank.chen021@outlook.com
 * @date 14/5/24 10:18 pm
 */
public class UUID {

    /**
     * The most significant 64 bits of this UUID.
     */
    private final long mostSignificantBits;

    /**
     * The least significant 64 bits of this UUID.
     */
    private final long leastSignificantBits;

    public UUID(long m, long l) {
        this.mostSignificantBits = m;
        this.leastSignificantBits = l;
    }

    /**
     * Return the compact format of the UUID
     */
    public String toCompactFormat() {
        char[] chars = new char[32];
        toChars(this.mostSignificantBits, 16, chars, 0);
        toChars(this.leastSignificantBits, 16, chars, 16);
        return new String(chars);
    }

    /**
     * 0181be8a-e593-7693-8897-8dc4c8d2f4bd
     * |---8---|-4-|-4-|--4--|------12-----|
     */
    public String toUUIDFormat() {
        char[] chars = new char[36];

        int index = 0;
        index = toChars(this.mostSignificantBits >> 32, 8, chars, index);
        chars[index++] = '-';

        index = toChars(this.mostSignificantBits >> 16, 4, chars, index);
        chars[index++] = '-';

        index = toChars(this.mostSignificantBits & 0x0000FFFF, 4, chars, index);
        chars[index++] = '-';

        // Higher 4 digits of the leastSignificantBits
        index = toChars(this.leastSignificantBits >> 48, 4, chars, index);
        chars[index++] = '-';

        // Lower 12 digits(48 bits) of the least significant bits
        toChars(this.leastSignificantBits & 0x0000_FFFF_FFFF_FFFFL, 12, chars, index);

        return new String(chars);
    }

    /**
     * @param digits how many digits. One digit has 4 bits
     */
    private int toChars(long value, int digits, char[] chars, int index) {
        for (int i = 0, shift = (digits - 1) * 4; i < digits; i++, shift -= 4) {
            long v = (value >>> shift) & 0x0F;
            chars[index++] = v <= 9 ? (char) ('0' + v) : (char) ('a' + v - 10);
        }
        return index;
    }
}
