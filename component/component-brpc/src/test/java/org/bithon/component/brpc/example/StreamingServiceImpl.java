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


import org.bithon.component.brpc.StreamResponse;

/**
 * @author frank.chen021@outlook.com
 * @date 23/5/25 8:41 pm
 */
public class StreamingServiceImpl implements IStreamingService {

    @Override
    public void streamNumbers(int count, StreamResponse<String> response) {
        new Thread(() -> {
            try {
                for (int i = 0; i < count; i++) {
                    if (response.isCancelled()) {
                        break;
                    }
                    response.onNext(String.valueOf(i));
                    Thread.sleep(10);
                }
                if (!response.isCancelled()) {
                    response.onComplete();
                }
            } catch (Exception e) {
                response.onException(e);
            }
        }).start();
    }

    @Override
    public void streamWithPrefix(String prefix, int count, StreamResponse<String> response) {
        new Thread(() -> {
            try {
                for (int i = 0; i < count; i++) {
                    if (response.isCancelled()) {
                        break;
                    }
                    response.onNext(prefix + i);
                    Thread.sleep(10);
                }
                if (!response.isCancelled()) {
                    response.onComplete();
                }
            } catch (Exception e) {
                response.onException(e);
            }
        }).start();
    }

    @Override
    public void streamWithError(int count, StreamResponse<String> response) {
        new Thread(() -> {
            try {
                for (int i = 0; i < count; i++) {
                    if (response.isCancelled()) {
                        break;
                    }
                    response.onNext(String.valueOf(i));
                    Thread.sleep(10);
                }
                Thread.sleep(10); // Reduced from 100ms to 10ms
                // Simulate error
                throw new RuntimeException("Simulated error during streaming");
            } catch (Exception e) {
                response.onException(e);
            }
        }).start();
    }

    @Override
    public void streamPersons(int count, StreamResponse<Person> response) {
        new Thread(() -> {
            try {
                for (int i = 0; i < count; i++) {
                    if (response.isCancelled()) {
                        break;
                    }
                    Person person = new Person("Person" + i, 20 + i);
                    response.onNext(person);
                    Thread.sleep(10);
                }
                if (!response.isCancelled()) {
                    response.onComplete();
                }
            } catch (Exception e) {
                response.onException(e);
            }
        }).start();
    }
}
