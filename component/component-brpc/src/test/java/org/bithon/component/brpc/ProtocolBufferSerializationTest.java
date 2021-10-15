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

package org.bithon.component.brpc;

import org.bithon.component.brpc.example.protobuf.WebRequestMetrics;
import org.bithon.component.brpc.message.serializer.ProtocolBufferSerializer;
import org.junit.Assert;
import org.junit.Test;
import shaded.com.google.protobuf.CodedInputStream;
import shaded.com.google.protobuf.CodedOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProtocolBufferSerializationTest {

    @Test
    public void testSerialization() throws IOException {
        ProtocolBufferSerializer serializer = new ProtocolBufferSerializer();

        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            CodedOutputStream os = CodedOutputStream.newInstance(bos);
            serializer.serialize(true, os);
            serializer.serialize(false, os);
            serializer.serialize('a', os);
            serializer.serialize((char) 0xFFFF, os);
            serializer.serialize((byte) 0xb, os);
            serializer.serialize((short) 0xbb, os);
            serializer.serialize(5, os);
            serializer.serialize(50L, os);
            serializer.serialize(5.9f, os);
            serializer.serialize(6.9d, os);
            serializer.serialize(new int[]{1, 3, 5}, os);
            serializer.serialize(new byte[]{9, 11, 13}, os);
            serializer.serialize(new short[]{119, 111, 113}, os);
            serializer.serialize(new long[]{1119, 1111, 1113}, os);
            serializer.serialize(new float[]{2119, 2111, 2113}, os);
            serializer.serialize(new double[]{3119, 3111, 3113}, os);
            os.flush();
            bytes = bos.toByteArray();
        }

        CodedInputStream is = CodedInputStream.newInstance(bytes);
        Assert.assertEquals(true, serializer.deserialize(is, boolean.class));
        Assert.assertEquals(false, serializer.deserialize(is, boolean.class));
        Assert.assertEquals((long) 'a', (long) serializer.deserialize(is, char.class));
        Assert.assertEquals((long) 0xFFFF, (long) serializer.deserialize(is, char.class));
        Assert.assertEquals(0xb, (long) serializer.deserialize(is, byte.class));
        Assert.assertEquals(0xbb, (long) serializer.deserialize(is, short.class));
        Assert.assertEquals(5, (long) serializer.deserialize(is, int.class));
        Assert.assertEquals((Object) 50L, serializer.deserialize(is, Long.class));
        Assert.assertEquals((Object) 5.9f, serializer.deserialize(is, float.class));
        Assert.assertEquals((Object) 6.9d, serializer.deserialize(is, double.class));
        Assert.assertArrayEquals(new int[]{1, 3, 5}, serializer.deserialize(is, int[].class));
        Assert.assertArrayEquals(new byte[]{9, 11, 13}, serializer.deserialize(is, byte[].class));
        Assert.assertArrayEquals(new short[]{119, 111, 113}, serializer.deserialize(is, short[].class));
        Assert.assertArrayEquals(new long[]{1119, 1111, 1113}, serializer.deserialize(is, long[].class));
        Assert.assertArrayEquals(new float[]{2119, 2111, 2113}, serializer.deserialize(is, float[].class), 0.000001f);
        Assert.assertArrayEquals(new double[]{3119, 3111, 3113}, serializer.deserialize(is, double[].class), 0.00001d);
        Assert.assertTrue(is.isAtEnd());
    }

    @Test
    public void testPrimitiveSerialization() throws IOException {
        ProtocolBufferSerializer serializer = new ProtocolBufferSerializer();

        Integer i = 11;
        Float f = 11.1f;
        Double d = 12.2d;
        Long l = 13L;
        Short s = 14;
        Byte b = 15;

        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            CodedOutputStream os = CodedOutputStream.newInstance(bos);
            serializer.serialize(i, os);
            serializer.serialize(f, os);
            serializer.serialize(d, os);
            serializer.serialize(l, os);
            serializer.serialize(s, os);
            serializer.serialize(b, os);
            os.flush();
            bytes = bos.toByteArray();
        }

        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            Assert.assertEquals(i, serializer.deserialize(is, int.class));
            Assert.assertEquals(f, serializer.deserialize(is, float.class));
            Assert.assertEquals(d, serializer.deserialize(is, Double.class));
            Assert.assertEquals(l, serializer.deserialize(is, Long.class));
            Assert.assertEquals(s, serializer.deserialize(is, Short.class));
            Assert.assertEquals(b, serializer.deserialize(is, Byte.class));
            Assert.assertTrue(is.isAtEnd());
        }
    }

    @Test
    public void testSerializationProtoBuffer() throws IOException {
        ProtocolBufferSerializer serializer = new ProtocolBufferSerializer();

        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            CodedOutputStream os = CodedOutputStream.newInstance(bos);
            serializer.serialize(WebRequestMetrics.newBuilder()
                                                  .setCount4Xx(4)
                                                  .setCount5Xx(5)
                                                  .setRequests(9)
                                                  .setUri("/info")
                                                  .build(), os);
            os.flush();
            bytes = bos.toByteArray();
        }

        CodedInputStream is = CodedInputStream.newInstance(bytes);
        WebRequestMetrics metrics = serializer.deserialize(is, WebRequestMetrics.class);
        Assert.assertEquals(4, metrics.getCount4Xx());
        Assert.assertEquals(5, metrics.getCount5Xx());
        Assert.assertEquals(9, metrics.getRequests());
        Assert.assertEquals("/info", metrics.getUri());
        Assert.assertTrue(is.isAtEnd());
    }

    @Test
    public void testMapSerialization() throws IOException {
        ProtocolBufferSerializer serializer = new ProtocolBufferSerializer();

        RequestMetrics mapObject = new RequestMetrics();
        mapObject.put("/info", WebRequestMetrics.newBuilder()
                                                .setCount4Xx(4)
                                                .setCount5Xx(5)
                                                .setRequests(9)
                                                .setUri("/info")
                                                .build());
        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            CodedOutputStream os = CodedOutputStream.newInstance(bos);
            serializer.serialize(mapObject, os);
            os.flush();
            bytes = bos.toByteArray();
        }

        // customer class
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            RequestMetrics metrics = serializer.deserialize(is, RequestMetrics.class);
            Assert.assertEquals(mapObject, metrics);
            Assert.assertTrue(is.isAtEnd());
        }

        // Map
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            Map<String, WebRequestMetrics> metrics = serializer.deserialize(is,
                                                                            new ProtocolBufferSerializer.TypeReference<Map<String, WebRequestMetrics>>() {
                                                                            });
            Assert.assertEquals(mapObject, metrics);
            Assert.assertTrue(is.isAtEnd());
        }

        // Hash Map
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            Map<String, WebRequestMetrics> metrics = serializer.deserialize(is,
                                                                            new ProtocolBufferSerializer.TypeReference<HashMap<String, WebRequestMetrics>>() {
                                                                            });
            Assert.assertEquals(mapObject, metrics);
            Assert.assertTrue(is.isAtEnd());
        }

        // ConcurrentHashMap
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            Map<String, WebRequestMetrics> metrics = serializer.deserialize(is,
                                                                            new ProtocolBufferSerializer.TypeReference<ConcurrentHashMap<String, WebRequestMetrics>>() {
                                                                            });
            Assert.assertEquals(mapObject, metrics);
            Assert.assertTrue(is.isAtEnd());
        }

        // Hashtable
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            Map<String, WebRequestMetrics> metrics = serializer.deserialize(is,
                                                                            new ProtocolBufferSerializer.TypeReference<Hashtable<String, WebRequestMetrics>>() {
                                                                            });
            Assert.assertEquals(mapObject, metrics);
            Assert.assertTrue(is.isAtEnd());
        }
    }

    @Test
    public void testCollectionSerialization() throws IOException {
        ProtocolBufferSerializer serializer = new ProtocolBufferSerializer();

        List<WebRequestMetrics> metrics1 = new ArrayList<>();
        metrics1.add(WebRequestMetrics.newBuilder()
                                      .setCount4Xx(4)
                                      .setCount5Xx(5)
                                      .setRequests(9)
                                      .setUri("/info")
                                      .build());
        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            CodedOutputStream os = CodedOutputStream.newInstance(bos);
            serializer.serialize(metrics1, os);
            os.flush();
            bytes = bos.toByteArray();
        }

        // Customer list
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            CustomerList metrics2 = serializer.deserialize(is,
                                                           CustomerList.class);
            Assert.assertEquals(metrics1.get(0), metrics2.get(0));
            Assert.assertTrue(is.isAtEnd());
        }

        // List
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            List<WebRequestMetrics> metrics2 = serializer.deserialize(is,
                                                                      new ProtocolBufferSerializer.TypeReference<List<WebRequestMetrics>>() {
                                                                      });
            Assert.assertEquals(metrics1, metrics2);
            Assert.assertTrue(is.isAtEnd());
        }

        // ArrayList
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            List<WebRequestMetrics> metrics2 = serializer.deserialize(is,
                                                                      new ProtocolBufferSerializer.TypeReference<ArrayList<WebRequestMetrics>>() {
                                                                      });
            Assert.assertEquals(metrics1, metrics2);
            Assert.assertTrue(is.isAtEnd());
        }

        // Set
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            Set<WebRequestMetrics> metrics2 = serializer.deserialize(is,
                                                                     new ProtocolBufferSerializer.TypeReference<Set<WebRequestMetrics>>() {
                                                                     });
            Assert.assertEquals(new HashSet<>(metrics1), metrics2);
            Assert.assertTrue(is.isAtEnd());
        }

        // LinkedList
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            LinkedList<WebRequestMetrics> metrics2 = serializer.deserialize(is,
                                                                            new ProtocolBufferSerializer.TypeReference<LinkedList<WebRequestMetrics>>() {
                                                                            });
            Assert.assertEquals(metrics1, metrics2);
            Assert.assertTrue(is.isAtEnd());
        }

        // Queue
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            Queue<WebRequestMetrics> metrics2 = serializer.deserialize(is,
                                                                       new ProtocolBufferSerializer.TypeReference<Queue<WebRequestMetrics>>() {
                                                                       });
            Assert.assertEquals(new LinkedList<>(metrics1), metrics2);
            Assert.assertTrue(is.isAtEnd());
        }

        // Array
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            WebRequestMetrics[] metrics2 = serializer.deserialize(is,
                                                                  WebRequestMetrics[].class);
            Assert.assertEquals(metrics1, Arrays.asList(metrics2));
            Assert.assertTrue(is.isAtEnd());
        }
    }

    @Test
    public void testObjectArraySerialization() throws IOException {
        ProtocolBufferSerializer serializer = new ProtocolBufferSerializer();

        WebRequestMetrics[] metrics1 = new WebRequestMetrics[]{
            WebRequestMetrics.newBuilder()
                             .setCount4Xx(4)
                             .setCount5Xx(5)
                             .setRequests(9)
                             .setUri("/info")
                .build()
        };
        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            CodedOutputStream os = CodedOutputStream.newInstance(bos);
            serializer.serialize(metrics1, os);
            os.flush();
            bytes = bos.toByteArray();
        }

        // Array
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            WebRequestMetrics[] metrics2 = serializer.deserialize(is,
                                                                  WebRequestMetrics[].class);
            Assert.assertArrayEquals(metrics1, metrics2);
            Assert.assertTrue(is.isAtEnd());
        }

        // Queue
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            Queue<WebRequestMetrics> metrics2 = serializer.deserialize(is,
                                                                       new ProtocolBufferSerializer.TypeReference<Queue<WebRequestMetrics>>() {
                                                                       });
            Assert.assertEquals(Arrays.asList(metrics1), metrics2);
            Assert.assertTrue(is.isAtEnd());
        }
    }

    @Test
    public void testPrimitiveArraySerialization() throws IOException {
        ProtocolBufferSerializer serializer = new ProtocolBufferSerializer();

        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            CodedOutputStream os = CodedOutputStream.newInstance(bos);
            serializer.serialize(new char[]{0xFFFF, 0xABCD}, os);
            serializer.serialize(new int[]{1, 3, 5}, os);
            serializer.serialize(new Integer[]{11, 13, 15}, os);
            os.flush();
            bytes = bos.toByteArray();
        }

        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            Assert.assertArrayEquals(new char[]{0xFFFF, 0xABCD}, serializer.deserialize(is, char[].class));
            Assert.assertArrayEquals(new int[]{1, 3, 5}, serializer.deserialize(is, int[].class));
            Assert.assertArrayEquals(new int[]{11, 13, 15}, serializer.deserialize(is, int[].class));
            Assert.assertTrue(is.isAtEnd());
        }
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            Assert.assertArrayEquals(new Character[]{0xFFFF, 0xABCD}, serializer.deserialize(is, Character[].class));
            Assert.assertArrayEquals(new Integer[]{1, 3, 5}, serializer.deserialize(is, Integer[].class));
            Assert.assertArrayEquals(new Integer[]{11, 13, 15}, serializer.deserialize(is, Integer[].class));
            Assert.assertTrue(is.isAtEnd());
        }
    }

    @Test
    public void testEmptyArraySerialization() throws IOException {
        ProtocolBufferSerializer serializer = new ProtocolBufferSerializer();

        WebRequestMetrics[] metrics1 = new WebRequestMetrics[]{
        };
        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            CodedOutputStream os = CodedOutputStream.newInstance(bos);
            serializer.serialize(metrics1, os);
            os.flush();
            bytes = bos.toByteArray();
        }

        // Array
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            WebRequestMetrics[] metrics2 = serializer.deserialize(is,
                                                                  WebRequestMetrics[].class);
            Assert.assertArrayEquals(metrics1, metrics2);
            Assert.assertTrue(is.isAtEnd());
        }

        // Queue
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            Queue<WebRequestMetrics> metrics2 = serializer.deserialize(is,
                                                                       new ProtocolBufferSerializer.TypeReference<Queue<WebRequestMetrics>>() {
                                                                       });
            Assert.assertEquals(Arrays.asList(metrics1), metrics2);
            Assert.assertTrue(is.isAtEnd());
        }
    }

    @Test
    public void testStringSerialization() throws IOException {
        ProtocolBufferSerializer serializer = new ProtocolBufferSerializer();

        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            CodedOutputStream os = CodedOutputStream.newInstance(bos);
            serializer.serialize("123456", os);
            serializer.serialize(new String[]{"123456", "54321"}, os);
            os.flush();
            bytes = bos.toByteArray();
        }

        // Array
        {
            CodedInputStream is = CodedInputStream.newInstance(bytes);
            Assert.assertEquals("123456", serializer.deserialize(is,
                                                                 String.class));
            Assert.assertArrayEquals(new String[]{"123456", "54321"}, serializer.deserialize(is, String[].class));
            Assert.assertTrue(is.isAtEnd());
        }
    }

    public static class RequestMetrics extends HashMap<String, WebRequestMetrics> {
    }

    public static class CustomerList extends ArrayList<WebRequestMetrics> {
    }
}
