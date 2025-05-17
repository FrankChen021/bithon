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

package org.bithon.server.datasource.query.setting;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author frank.chen021@outlook.com
 * @date 17/5/25 11:38 am
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties("bithon.datasource.query.settings")
public class QuerySettings {
    public static final QuerySettings DEFAULT = new QuerySettings();

    private boolean enableRegularExpressionOptimization = true;
    
    /**
     * When true, allows optimization of regex patterns like "^prefix.*" to startsWith expressions.
     * When false, these patterns will remain as regular expression matches.
     */
    private boolean enableRegularExpressionToStartsWith = false;
    
    /**
     * When true, allows optimization of regex patterns like ".*suffix$" to endsWith expressions.
     * When false, these patterns will remain as regular expression matches.
     */
    private boolean enableRegularExpressionToEndsWith = false;
}
