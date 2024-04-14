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

package org.bithon.server.commons.autocomplete;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.Transition;
import org.bithon.component.commons.utils.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/4/14 21:06
 */
public class ATNDump {
    public static void dump(Parser parser) {
        dump("", parser, parser.getATN().states.get(0), new HashSet<>());
    }

    private static void dump(String indent, Parser parser, ATNState state, Set<Integer> visited) {
        if (visited.contains(state.stateNumber)) {
            return;
        }
        visited.add(state.stateNumber);

        System.out.println(StringUtils.format("%sState %d: %s, %s",
                                              indent,
                                              state.stateNumber,
                                              state.getClass().getSimpleName(),
                                              parser.getRuleNames()[state.ruleIndex]
                                             ));

        for (Transition transition : state.getTransitions()) {
            if (transition.isEpsilon()) {
                dump(indent + " ", parser, transition.target, visited);
            } else if (transition instanceof AtomTransition) {
                //printAtomTransition(indent, parser, (AtomTransition) transition);
                dump(indent + " ", parser, transition.target, visited);
            } else {
                printTransition(indent, parser, transition);
            }
        }

        visited.remove(state.stateNumber);
    }


    private static void printTransition(String indent, Parser parser, Transition transition) {
        System.out.println(StringUtils.format("%sRule %s, %s, to %s",
                                              indent,
                                              transition.getClass().getSimpleName(),
                                              parser.getRuleNames()[transition.target.ruleIndex],
                                              transition.target));
    }

    private static void printAtomTransition(String indent, Parser parser, AtomTransition transition) {
        System.out.println(StringUtils.format("%sRule %s, AtomTransition, label = %d, to %s",
                                              indent,
                                              parser.getRuleNames()[transition.target.ruleIndex],
                                              transition.label,
                                              transition.target));
    }
}
