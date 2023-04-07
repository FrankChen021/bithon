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

package org.bithon.component.brpc.message;

import java.util.HashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/12/10 18:08
 */
public class Headers extends HashMap<String, String> {

    /**
     * Version-Git Commit-Timestamp
     */
    public static final String HEADER_VERSION = "Version";

    public static final String HEADER_APP_ID = "appId";

    public static final Headers EMPTY = new Headers();
}
