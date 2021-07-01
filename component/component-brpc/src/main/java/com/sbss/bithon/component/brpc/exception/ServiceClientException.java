/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.component.brpc.exception;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/1 2:01 下午
 */
public class ServiceClientException extends ServiceInvocationException {
    public ServiceClientException(CharSequence message) {
        super(message);
    }

    public ServiceClientException(String messageFormat, Object... args) {
        super(String.format(messageFormat, args));
    }
}
