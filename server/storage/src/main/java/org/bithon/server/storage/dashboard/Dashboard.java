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

package org.bithon.server.storage.dashboard;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * @author Frank Chen
 * @date 19/8/22 5:42 pm
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dashboard {
    private String name;
    private String payload;
    private String signature;
    private Timestamp timestamp;
    private boolean deleted;

    @Data
    public static class Metadata {
        private String title;
        private String folder;
    }

    @JsonIgnore
    private Metadata metadata;
}
