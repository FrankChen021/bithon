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

package org.bithon.component.commons.exception;

import org.bithon.component.commons.utils.StringUtils;

/**
 * @author Frank Chen
 * @date 24/2/23 11:09 pm
 */
public class HttpMappableException extends RuntimeException {

    private final int statusCode;
    private final String exception;


    public HttpMappableException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.exception = null;
    }

    public HttpMappableException(String exception, int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.exception = exception;
    }

    public HttpMappableException(int statusCode, String messageFormat, Object... args) {
        super(StringUtils.format(messageFormat, args));
        this.statusCode = statusCode;
        this.exception = null;
    }

    public HttpMappableException(String exception, int statusCode, String messageFormat, Object... args) {
        super(StringUtils.format(messageFormat, args));
        this.statusCode = statusCode;
        this.exception = exception;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getException() {
        return exception;
    }
}
