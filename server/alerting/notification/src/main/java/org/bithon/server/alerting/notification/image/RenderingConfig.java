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

package org.bithon.server.alerting.notification.image;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author
 * @date
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bithon.alerting.notification.rendering")
public class RenderingConfig {
    private boolean enabled = false;

    /**
     * The render API
     */
    private String serviceEndpoint = "http://localhost:3000/api/public/chart/render";

    /**
     * Hour of data range before the alerting time.
     */
    @Min(1)
    @Max(24)
    private int dataRange = 1;

    /**
     * Size of image
     */
    @Min(600)
    @Max(2000)
    private int width = 800;

    @Min(200)
    @Max(600)
    private int height = 300;
}
