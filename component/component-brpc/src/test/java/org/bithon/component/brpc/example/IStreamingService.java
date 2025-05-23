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


import org.bithon.component.brpc.BrpcMethod;
import org.bithon.component.brpc.BrpcService;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.brpc.message.serializer.Serializer;

/**
 * @author frank.chen021@outlook.com
 * @date 23/5/25 8:39 pm
 */
@BrpcService
public interface IStreamingService {

    /**
     * Stream a sequence of numbers as strings
     */
    void streamNumbers(int count, StreamResponse<String> response);

    /**
     * Stream numbers with a prefix
     */
    void streamWithPrefix(String prefix, int count, StreamResponse<String> response);

    /**
     * Stream that throws an error after N numbers
     */
    void streamWithError(int count, StreamResponse<String> response);

    /**
     * Stream complex objects using JSON serialization
     */
    @BrpcMethod(serializer = Serializer.JSON_SMILE)
    void streamPersons(int count, StreamResponse<Person> response);
}
