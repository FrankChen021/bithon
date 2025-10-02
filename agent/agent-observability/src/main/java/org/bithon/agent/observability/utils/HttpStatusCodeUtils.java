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
 * Utility class for HTTP status code string conversion with optimization for common status codes
 *
 * @author frank.chen021@outlook.com
 */
public class HttpStatusCodeUtils {

    /**
     * Convert HTTP status code to string with optimization for common status codes.
     * For common status codes, returns cached string instances to avoid object creation.
     * For uncommon status codes, creates interned strings to enable reuse.
     *
     * @param statusCode the HTTP status code
     * @return string representation of the status code
     */
    public static String statusCodeToString(int statusCode) {
        switch (statusCode) {
            // 2xx Success
            case 200: return "200";
            case 201: return "201";
            case 202: return "202";
            case 204: return "204";
            
            // 3xx Redirection
            case 301: return "301";
            case 302: return "302";
            case 304: return "304";
            
            // 4xx Client Error
            case 400: return "400";
            case 401: return "401";
            case 403: return "403";
            case 404: return "404";
            case 405: return "405";
            case 409: return "409";
            case 429: return "429";
            
            // 5xx Server Error
            case 500: return "500";
            case 502: return "502";
            case 503: return "503";
            case 504: return "504";
            
            // For uncommon status codes, use interned strings
            default: return String.valueOf(statusCode).intern();
        }
    }
}
