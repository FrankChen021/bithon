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

package org.bithon.server.storage.datasource.input.transformer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.InputRowAccessorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Frank Chen
 * @date 12/3/24 6:35 pm
 */
public class ReplaceTransformer implements ITransformer {

    @JsonIgnore
    private final Function<IInputRow, Object> inputRowGetter;

    @JsonIgnore
    private final BiConsumer<IInputRow, String> inputRowSetter;

    @JsonIgnore
    private final List<String> namedGroups;

    private final String source;

    /**
     *
     */
    private final String match;
    private final String replacement;
    private final boolean replaceAll;

    @JsonIgnore
    private Pattern pattern;

    public ReplaceTransformer(String source,
                              String match,
                              String replacement) {
        this(source, match, replacement, null, null);
    }

    @JsonCreator
    public ReplaceTransformer(@JsonProperty("source") String source,
                              @JsonProperty("match") String match,
                              @JsonProperty("replacement") String replacement,
                              @JsonProperty("replaceAll") Boolean replaceAll,
                              @JsonProperty("when") String when) {
        super(when);

        this.source = Preconditions.checkArgumentNotNull("source", source);
        this.match = Preconditions.checkArgumentNotNull("match", match);
        this.replacement = Preconditions.checkArgumentNotNull("replacement", replacement);
        this.replaceAll = replaceAll == null || replaceAll;
        this.inputRowGetter = InputRowAccessorFactory.createGetter(source);
        this.inputRowSetter = InputRowAccessorFactory.createSetter(source);

        this.pattern = Pattern.compile(match);

        // Extract all named groups
        // the named group has the following format:
        // unnamed group capture: (\w+)
        // named group capture: (?<name>\w+)
        this.namedGroups = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\(\\?<([a-zA-Z_][a-zA-Z0-9]*)>").matcher(match);
        while (matcher.find()) {
            String groupName = matcher.group(1);
            namedGroups.add(groupName);
        }
    }

    @Override
    public boolean transform(IInputRow data) {
        Object value = inputRowGetter.get(data);
        if (value == null) {
            return TransformResult.NEXT;
        }

        String input = value.toString();
        Matcher matcher = this.pattern.matcher(input);

        String replaced = null;
        if (this.replaceAll) {
            // The code below is adopted from the replaceAll methods of the Matcher
            boolean found = matcher.find();
            if (found) {
                StringBuilder sb = new StringBuilder();
                do {

                    if (!namedGroups.isEmpty()) {
                        for (String namedGroup : namedGroups) {
                            String groupVal = matcher.group(namedGroup);
                            if (groupVal != null) {
                                data.put(namedGroup, groupVal);
                            }
                        }
                    }

                    matcher.appendReplacement(sb, replacement);
                    found = matcher.find();
                } while (found);
                matcher.appendTail(sb);
                replaced = sb.toString().trim();
            }
        } else {
            if (matcher.find()) {
                StringBuilder sb = new StringBuilder();
                matcher.appendReplacement(sb, replacement);
                matcher.appendTail(sb);
                replaced = sb.toString().trim();

                if (!namedGroups.isEmpty()) {
                    for (String namedGroup : namedGroups) {
                        String groupVal = matcher.group(namedGroup);
                        if (groupVal != null) {
                            data.put(namedGroup, groupVal);
                        }
                    }
                }
            }
        }

        if (replaced != null) {
            inputRowSetter.set(data, replaced);
        }

        return TransformResult.NEXT;
    }
}
