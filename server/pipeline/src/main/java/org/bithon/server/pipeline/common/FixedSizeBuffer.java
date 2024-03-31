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

package org.bithon.server.pipeline.common;

import org.bithon.component.commons.utils.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author Frank Chen
 * @date 3/1/23 11:41 am
 */
public class FixedSizeBuffer extends OutputStream {


    public static class OverflowException extends IOException {
        public OverflowException(String message) {
            super(message);
        }
    }

    private final byte[] buf;
    private int position;
    private int mark = -1;

    public FixedSizeBuffer(int limit) {
        this.buf = new byte[limit];
        this.position = 0;
    }

    public int limit() {
        return buf.length;
    }

    public int size() {
        return position;
    }

    public void writeBytes(byte[] value) {
        ensureCapacity(value.length);
        System.arraycopy(value, 0, this.buf, this.position, value.length);
        this.position += value.length;
    }

    public void writeBytes(byte[] value, int offset, int len) {
        ensureCapacity(len);
        System.arraycopy(value, offset, this.buf, this.position, len);
        this.position += len;
    }

    public void writeAsciiChar(char value) {
        ensureCapacity(1);
        this.buf[this.position++] = (byte) value;
    }

    public void writeByte(byte value) {
        ensureCapacity(1);
        this.buf[this.position++] = value;
    }

    public void writeString(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        ensureCapacity(bytes.length);
        writeBytes(bytes);
    }

    public void deleteFromEnd(int length) {
        if (position >= length) {
            position -= length;
        }
    }

    public byte[] toBytes() {
        byte[] d = new byte[this.position];
        System.arraycopy(this.buf, 0, d, 0, this.position);
        return d;
    }

    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(this.buf, 0, this.position);
    }

    public void clear() {
        this.position = 0;
        this.mark = -1;
    }

    public void reset(int position) {
        this.position = position;
    }

    public int getPosition() {
        return this.position;
    }

    private void ensureCapacity(int extraSize) throws OverflowException {
        if (this.position + extraSize > this.buf.length) {
            throw new OverflowException(StringUtils.format("Buffer is limited to size of %d, but requires %d.", buf.length, this.position + extraSize));
        }
    }

    @Override
    public void write(int b) {
        this.writeByte((byte) b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        this.writeBytes(b, off, len);
    }

    public void mark() {
        this.mark = this.position;
    }

    public void reset() {
        if (this.mark != -1) {
            this.position = this.mark;
        }
    }
}
