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

package org.bithon.agent.plugin.apache.zookeeper;

import org.apache.zookeeper.ZooDefs;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 15/1/25 9:05 pm
 */
public class OpNameLookup {
    private final Map<Integer, String> lookupTable = new HashMap<>();

    public OpNameLookup() {
        for (Field field : ZooDefs.OpCode.class.getDeclaredFields()) {
            try {
                Integer opCodeValue = (Integer) field.get(null);
                String opName = field.getName();

                if (Character.isLowerCase(opName.charAt(0))) {
                    // Turn the first char to UPPER case
                    opName = Character.toUpperCase(opName.charAt(0)) + opName.substring(1);
                }

                lookupTable.put(opCodeValue, opName);
            } catch (Throwable ignored) {
            }
        }
    }

    public String lookup(int opCode) {
        String name = lookupTable.get(opCode);
        return name == null ? "UNKNOWN(" + opCode + ")" : name;
    }
}
