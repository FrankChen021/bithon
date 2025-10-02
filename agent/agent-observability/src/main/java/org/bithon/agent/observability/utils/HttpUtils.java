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

package org.bithon.agent.observability.utils;

/**
 * Utility class for HTTP-related operations
 *
 * @author frank.chen021@outlook.com
 */
public class HttpUtils {

    /**
     * Convert HTTP status code to string with optimization for common status codes.
     * For common status codes, returns cached string instances to avoid object creation.
     * For uncommon status codes, create interned strings to enable reuse.
     *
     * @param statusCode the HTTP status code
     * @return string representation of the status code
     */
    public static String statusCodeToString(int statusCode) {
        switch (statusCode) {
            // 2xx Success
            case 200:
                return "200";
            case 201:
                return "201";
            case 202:
                return "202";
            case 204:
                return "204";

            // 3xx Redirection
            case 301:
                return "301";
            case 302:
                return "302";
            case 304:
                return "304";

            // 4xx Client Error
            case 400:
                return "400";
            case 401:
                return "401";
            case 403:
                return "403";
            case 404:
                return "404";
            case 405:
                return "405";
            case 409:
                return "409";
            case 429:
                return "429";

            // 5xx Server Error
            case 500:
                return "500";
            case 502:
                return "502";
            case 503:
                return "503";
            case 504:
                return "504";

            // For uncommon status codes, use interned strings
            default:
                if (statusCode > 0 && statusCode < 1000) {
                    // A protection in case of memory DOS attack by using very large status codes
                    return String.valueOf(statusCode).intern();
                } else {
                    return String.valueOf(statusCode);
                }

        }
    }

    /**
     * Extract path from URI by removing query parameters
     * <p>
     * For example: "/api/users?id=123&name=john" -> "/api/users"
     *
     * @param uri the URI string that may contain query parameters
     * @return the path portion without query parameters
     */
    public static String dropQueryParameters(String uri) {
        int queryIndex = uri.indexOf('?');
        return queryIndex == -1 ? uri : uri.substring(0, queryIndex);
    }

    /**
     * Parse HTTP status code from status line
     * <p>
     * The input statusLine has a format of the following examples:
     * <ul>
     *   <li>HTTP/1.0 200 OK</li>
     *   <li>HTTP/1.0 401 Unauthorized</li>
     * </ul>
     * It will return 200 and 401 respectively. Returns -1 if no code can be discerned.
     *
     * @param statusLine the HTTP status line
     * @return the parsed status code, or -1 if parsing fails
     */
    public static String parseStatusLine(String statusLine) {
        if (statusLine == null) {
            return "-1";
        }
        int firstSpace = statusLine.indexOf(' ');
        if (firstSpace == -1) {
            return "-1";
        }
        
        // We need at least firstSpace + 5 characters total because:
        // - substring(firstSpace + 1, firstSpace + 4) extracts 3 characters (the status code)
        // - charAt(firstSpace + 4) checks the character right after those 3 digits (must be a space)
        // Example: "HTTP/1.1 200 OK" where firstSpace=8, we need chars at indices 9,10,11 for "200" 
        // and index 12 for the space, so minimum length is 13 (firstSpace=8, 8+5=13)
        if (firstSpace + 5 > statusLine.length()) {
            return "-1";
        }
        if (statusLine.charAt(firstSpace + 4) != ' ') {
            return "-1";
        }
        if (!Character.isDigit(statusLine.charAt(firstSpace + 1))) {
            return "-1";
        }
        if (!Character.isDigit(statusLine.charAt(firstSpace + 2))) {
            return "-1";
        }
        if (!Character.isDigit(statusLine.charAt(firstSpace + 3))) {
            return "-1";
        }

        // Since we have checked the characters are digits only, there's only up to 1000 possible values, 
        // we can safely use intern() to avoid object creation
        return statusLine.substring(firstSpace + 1, firstSpace + 4).intern();
    }
}
