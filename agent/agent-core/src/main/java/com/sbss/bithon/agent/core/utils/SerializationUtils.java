package com.sbss.bithon.agent.core.utils;

import shaded.com.esotericsoftware.kryo.Kryo;
import shaded.com.esotericsoftware.kryo.io.Input;
import shaded.com.esotericsoftware.kryo.io.Output;
import shaded.com.esotericsoftware.kryo.serializers.JavaSerializer;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SerializationUtils {

    private static final Logger log = LoggerFactory.getLogger(SerializationUtils.class);

    public static byte[] serializeObject(Object obj) {
        Kryo kryo = new Kryo();
        kryo.setReferences(false);
        kryo.register(obj.getClass(), new JavaSerializer());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Output output = new Output(os);
        kryo.writeClassAndObject(output, obj);
        output.flush();
        output.close();
        byte[] result = os.toByteArray();
        try {
            os.flush();
            os.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    public static Object deserializeObject(byte[] bin) {
        Object result = null;
        if (null != bin) {
            Kryo kryo = new Kryo();
            kryo.setReferences(false);
            kryo.register(bin.getClass(), new JavaSerializer());
            InputStream is = new ByteArrayInputStream(bin);
            Input input = new Input(is);
            result = kryo.readClassAndObject(input);
            input.close();
            try {
                is.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return result;
    }
}
