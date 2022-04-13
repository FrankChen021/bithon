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

package org.bithon.component.commons.security;

import org.bithon.component.commons.utils.NumberUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Frank Chen
 * @date 13/4/22 10:12 PM
 */
public class HashGenerator {

    private static MessageDigest sha256Provider;

    static {
        try {
            sha256Provider = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ignored) {
        }
    }

    public static byte[] sha256(String input) {
        return sha256Provider.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256String(String input) {
        return NumberUtils.toHexString(sha256(input));
    }

}
