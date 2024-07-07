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

package org.bithon.server.starter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * SpringBoot3 explicitly disallows trailing slash in the URL, which is not compatible with SpringBoot2.
 * To make the code compatible with previous release, we need to add a filter to handle the trailing slash.
 * The filter {@link UrlHandlerFilter} used below is copied from Spring Framework 6.2 source code.
 *
 * @author frank.chen021@outlook.com
 * @date 2024/7/7 12:07
 */
@Configuration
public class TrailingSlashCompatibilityConfigurer {
    @Bean
    public OncePerRequestFilter trailingSlashHandler() {
        return UrlHandlerFilter.trailingSlashHandler("/**")
                               .wrapRequest()
                               .build();
    }
}
