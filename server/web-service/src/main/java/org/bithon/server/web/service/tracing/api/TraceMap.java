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

package org.bithon.server.web.service.tracing.api;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collection;

/**
 * @author Frank Chen
 * @date 25/11/21 3:05 pm
 */
@Data
@AllArgsConstructor
public class TraceMap {

    @Data
    public static class Node {
        private String name;
        private int level;
        private int levelIndex;

        public Node(String name) {
            this.name = name;
        }
    }

    @Data
    public static class Link {
        private String source;
        private String target;

        public Link(String source, String target) {
            this.source = source;
            this.target = target;
        }

        private int count;

        public Link incrCount() {
            count++;
            return this;
        }
    }

    private Collection<Node> nodes;
    private Collection<Link> links;
}
