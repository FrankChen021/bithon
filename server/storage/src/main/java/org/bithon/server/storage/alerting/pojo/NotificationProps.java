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

package org.bithon.server.storage.alerting.pojo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bithon.component.commons.utils.HumanReadableDuration;

import java.util.List;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 9/12/24 5:27 pm
 */
@Getter
@Setter
@Builder
public class NotificationProps {

    /**
     * silence period in minute
     */
    private HumanReadableDuration silence;

    @NotEmpty
    private List<String> channels;

    /**
     * Can be empty. If empty, the template defined on the channel will be used
     */
    private String message;

    /**
     * Which expression will be rendered as image.
     * Some ALERT rule might use composite alert expressions, some of which might be seen as 'pre-conditions',
     * there's no need to render them as images.
     * <p>
     * Can be empty.
     */
    private Set<String> renderExpressions;
}
