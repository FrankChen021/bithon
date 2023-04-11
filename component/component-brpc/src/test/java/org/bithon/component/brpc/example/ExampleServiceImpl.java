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

package org.bithon.component.brpc.example;

import org.bithon.component.brpc.example.protobuf.WebRequestMetrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExampleServiceImpl implements IExampleService {

    @Override
    public int div(int a, int b) {
        return a / b;
    }

    @Override
    public void block(int timeout) {
        try {
            Thread.sleep(timeout * 1000L);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void sendOneway(String msg) {
        System.out.println("Got message:" + msg);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public int[] append(int[] arrays, int value) {
        int[] newArray = Arrays.copyOf(arrays, arrays.length + 1);
        newArray[arrays.length] = value;
        return newArray;
    }

    @Override
    public String[] append(String[] arrays, String value) {
        String[] newArray = Arrays.copyOf(arrays, arrays.length + 1);
        newArray[arrays.length] = value;
        return newArray;
    }

    @Override
    public List<String> delete(List<String> list, int index) {
        list.remove(index);
        return list;
    }

    @Override
    public Map<String, String> mergeMap(Map<String, String> a, Map<String, String> b) {
        if (b != null) {
            if (a != null) {
                b.forEach(a::put);
            } else {
                return b;
            }
        }
        return a;
    }

    @Override
    public String sendWebMetrics(WebRequestMetrics metrics) {
        System.out.printf(Locale.ENGLISH, "Receiving metrics: %s\n", metrics);
        return metrics.getUri();
    }

    @Override
    public String sendWebMetrics1(WebRequestMetrics metrics1, WebRequestMetrics metrics2) {
        return metrics1.getUri() + "-" + metrics2.getUri();
    }

    @Override
    public String sendWebMetrics2(String uri, WebRequestMetrics metrics) {
        return uri + "-" + metrics.getUri();
    }

    @Override
    public String sendWebMetrics3(WebRequestMetrics metrics, String uri) {
        return metrics.getUri() + "-" + uri;
    }

    @Override
    public Map<String, String> mergeWithJson(Map<String, String> a, Map<String, String> b) {
        return mergeMap(a, b);
    }

    @Override
    public String ping() {
        return "pong";
    }

    @Override
    public List<String> createList(int size) {
        List<String> ret = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ret.add(String.valueOf(i));
        }
        return ret;
    }
}
