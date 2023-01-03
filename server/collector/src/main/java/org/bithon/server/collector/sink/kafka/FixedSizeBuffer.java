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

package org.bithon.server.collector.sink.kafka;

import org.bithon.component.commons.utils.StringUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author Frank Chen
 * @date 3/1/23 11:41 am
 */
class FixedSizeBuffer {
    private final byte[] buf;
    private int size;

    FixedSizeBuffer(int limit) {
        this.buf = new byte[limit];
        this.size = 0;
    }

    int limit() {
        return buf.length;
    }

    public int size() {
        return size;
    }

    boolean isEmpty() {
        return size == 0;
    }

    void writeBytes(byte[] value) {
        ensureCapacity(value.length);
        System.arraycopy(value, 0, this.buf, this.size, value.length);
        this.size += value.length;
    }

    void writeChar(char value) {
        ensureCapacity(1);
        this.buf[this.size++] = (byte) value;
    }

    void writeString(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        ensureCapacity(bytes.length);
        writeBytes(bytes);
    }

    public void deleteFromEnd(int length) {
        if (size >= length) {
            size -= length;
        }
    }

    byte[] toBytes() {
        byte[] d = new byte[this.size];
        System.arraycopy(this.buf, 0, d, 0, this.size);
        return d;
    }

    ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(this.buf, 0, this.size);
    }

    public void reset() {
        this.size = 0;
    }

    public void reset(int offset) {
        this.size = offset;
    }

    public int getOffset() {
        return this.size;
    }

    private void ensureCapacity(int extraSize) {
        if (this.size + extraSize > this.buf.length) {
            throw new RuntimeException(StringUtils.format("Buffer is limited to size of %d, but requires %d.", buf.length, this.size + extraSize));
        }
    }
}
