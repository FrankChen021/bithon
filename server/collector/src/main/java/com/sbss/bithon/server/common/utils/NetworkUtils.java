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

package com.sbss.bithon.server.common.utils;

import org.jooq.tools.StringUtils;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/4 10:24 下午
 */
public class NetworkUtils {
    private static final String DOUBLE_SLASH = "//";
    private static final String SINGLE_SLASH = "/";

    private static Pattern IPV4_PATTERN = Pattern.compile(
        "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}");

    public static boolean isIpAddress(String hostPort) {
        return IPV4_PATTERN.matcher(hostPort).matches();
    }

    public static String formatUri(URI uri) {
        String targetUri = uri.getPath();

        String formatUri = targetUri.replaceAll(DOUBLE_SLASH, SINGLE_SLASH)
                                    .split("\\?")[0]
            .split("&")[0]
            .split(",")[0]
            .split(";")[0];

        formatUri = formatUri.endsWith(SINGLE_SLASH) ? formatUri.substring(0, formatUri.length() - 1) : formatUri;
        return StringUtils.isBlank(formatUri) ? SINGLE_SLASH : formatUri;
    }
}
