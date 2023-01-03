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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author Frank Chen
 * @date 3/1/23 11:41 am
 */
class KafkaMessage {
    private byte[] buf;
    private int size;

    KafkaMessage(int limit) {
        this.buf = new byte[limit];
        this.size = 0;
    }

    int capacity() {
        return buf.length;
    }

    boolean isEmpty() {
        return size == 0;
    }

    void writeBytes(byte[] buf) {
        ensureCapacity(buf.length);
        System.arraycopy(buf, 0, this.buf, this.size, buf.length);
        this.size += buf.length;
    }

    void writeChar(char chr) {
        ensureCapacity(1);
        this.buf[this.size++] = (byte) chr;
    }

    void writeString(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ensureCapacity(bytes.length);
        writeBytes(bytes);
    }

    public void deleteFromEnd(int length) {
        if (size >= length) {
            size -= length;
        }
    }

    byte[] toBytes() {
        if (size == this.buf.length) {
            return this.buf;
        } else {
            byte[] d = new byte[this.size];
            System.arraycopy(this.buf, 0, d, 0, this.size);
            return d;
        }
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
        int newSize = buf.length;
        while (this.size + extraSize > newSize) {
            newSize *= 2;
        }
        if (newSize != buf.length) {
            byte[] newBuff = new byte[newSize];
            System.arraycopy(this.buf, 0, newBuff, 0, this.size);
            this.buf = newBuff;
        }
    }
}
