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

package com.sbss.bithon.agent.sdk.expt;

/**
 * @author Frank Chen
 * @date 3/10/21 16:33
 */
public class SdkException extends RuntimeException {
    public SdkException(String message) {
        super(message);
    }

    public SdkException(String format, Object... args) {
        super(String.format(format, args));
    }
}
