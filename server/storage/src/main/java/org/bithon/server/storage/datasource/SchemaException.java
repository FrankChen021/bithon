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

package org.bithon.server.storage.datasource;

import org.bithon.component.commons.exception.HttpResponseMapping;
import org.bithon.component.commons.utils.StringUtils;

/**
 * @author frank.chen021@outlook.com
 * @date 7/10/21 11:49 am
 */
public class SchemaException extends RuntimeException {

    protected SchemaException(String msg) {
        super(msg);
    }

    @HttpResponseMapping(statusCode = HttpResponseMapping.StatusCode.BAD_REQ)
    public static class NotFound extends SchemaException {
        public NotFound(String name) {
            super("Can't find schema: " + name);
        }
    }

    @HttpResponseMapping(statusCode = HttpResponseMapping.StatusCode.BAD_REQ)
    public static class AlreadyExists extends SchemaException {
        public AlreadyExists(String name) {
            super(StringUtils.format("Schema [%s] already exists.", name));
        }
    }
}
