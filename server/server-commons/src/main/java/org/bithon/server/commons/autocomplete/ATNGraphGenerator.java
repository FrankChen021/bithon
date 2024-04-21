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
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.EpsilonTransition;
import org.antlr.v4.runtime.atn.RuleStartState;
import org.antlr.v4.runtime.atn.RuleStopState;
import org.antlr.v4.runtime.atn.RuleTransition;
import org.antlr.v4.runtime.atn.Transition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/4/20 17:49
 */
public class ATNGraphGenerator {

    private String[] ruleNames;
    private Vocabulary vocabulary;

    static class IATNNode {
        final long id; //: number; // A unique number (positive for state numbers, negative for rule nodes)
        final String name;

        // We use the INVALID_TYPE in this field to denote a rule node.
        final int type;
        List<IATNLink> links = new ArrayList<>();

        IATNNode(long id, String name, int type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }

        boolean isRule() {
            return type == ATNState.INVALID_TYPE;
        }

        @Override
        public String toString() {
            return "IATNNode{" +
                   "id=" + id +
                   ", name='" + name + '\'' +
                   ", type=" + type +
                   '}';
        }
    }

    static class IATNLink {
        IATNNode source;
        IATNNode target;
        ATNState state;
        int ruleIndex;
        Transition transition;
        String label;

        IATNLink(IATNNode source,
                 IATNNode target,
                 ATNState state,
                 int ruleIndex,
                 Transition transition,
                 String label) {
            this.source = source;
            this.target = target;
            this.state = state;
            this.ruleIndex = ruleIndex;
            this.transition = transition;
            this.label = label;

            source.links.add(this);
        }
    }

    // Maps an ATN state to its index in the rules list.
    Map<Long, IATNNode> stateToNode = new HashMap<>();
    List<IATNNode> nodes = new ArrayList<>();
    List<IATNLink> links = new ArrayList<>();
    int currentRuleIndex = -1;

    static class Graph {
        Map<Integer, IATNNode> node;
    }

    public void generate(LexerAndParserFactory factory) {
        Parser parser = factory.createParser(null);
        this.vocabulary = parser.getVocabulary();
        this.ruleNames = parser.getRuleNames();
        parser.dumpDFA();
        ATN parserATN = parser.getATN();

        for (int i = 0; i < parserATN.ruleToStartState.length; i++) {
            generate(parserATN, i);
        }
    }

    public void generate(ATN atn, int ruleIndex) {
        RuleStartState startState = atn.ruleToStartState[ruleIndex];
        RuleStopState stopState = atn.ruleToStopState[ruleIndex];

        Set<ATNState> seenStates = new HashSet<>(Collections.singletonList(startState));
        Stack<ATNState> pipeline = new Stack<>();
        pipeline.push(startState);

        while (!pipeline.isEmpty()) {
            ATNState state = pipeline.pop();

            IATNNode srcNode = ensureATNNode(state.stateNumber, state);
            Transition[] transitions = state.getTransitions();

            for (Transition transition : transitions) {
                // Rule stop states usually point to the follow state in the calling rule, but can also
                // point to a state in itself if the rule is left recursive. In any case we don't need to follow
                // transitions going out from a stop state.
                if (state == stopState) {
                    continue;
                }

                boolean transitsToRule = (transition.target).getStateType() == ATNState.RULE_START;
                //int marker = transition.target.stateNumber * (transitsToRule ? state.stateNumber : 1);
                long marker = transitsToRule ? ((long) state.stateNumber << 32) | ((long) transition.target.stateNumber) : transition.target.stateNumber;
                IATNNode dstNode = ensureATNNode(marker, transition.target);

                IATNLink link = new IATNLink(
                    srcNode,
                    dstNode,
                    state,
                    state.ruleIndex,
                    transition,
                    getLinkTag(transition)
                );

                links.add(link);

                ATNState nextState;
                if (transitsToRule) {
                    // Target is a state in a different rule (or this rule if left recursive).
                    // Add a back link from that sub rule into ours.
                    nextState = ((RuleTransition) transition).followState;
                    IATNNode returnIndex = ensureATNNode(nextState.stateNumber, nextState);

                    IATNLink nodeLink = new IATNLink(dstNode, returnIndex, nextState, nextState.ruleIndex, transition, "ε");
                    links.add(nodeLink);
                } else {
                    nextState = transition.target;
                }

                if (seenStates.contains(nextState)) {
                    continue;
                }

                seenStates.add(nextState);
                pipeline.push(nextState);
            }
        }
    }

    private String getLinkTag(Transition transition) {
        if (transition instanceof EpsilonTransition) {
            return "ε";
        }
        String name = transition.getClass().getSimpleName();
        String tag = name.endsWith("Transition") ? name.substring(0, name.length() - "Transition".length()).toLowerCase() : name;
        if (transition instanceof AtomTransition) {
            tag += "("
                   + ((AtomTransition) transition).label()
                                                  .toList()
                                                  .stream()
                                                  .map((label) -> vocabulary.getDisplayName(label))
                                                  .collect(Collectors.joining(", "))
                   + ")";
        }
        return tag;
    }

    /**
     * Checks the list of used ATN nodes for the given stateNumber and adds a new ATN node if no entry could be found.
     *
     * @param stateNumber The state identifier (usually the state number).
     * @param state       The ATN state represented by the ATN node, if a new node must be added.
     * @returns The index of the ATN node for the given state.
     */
    private IATNNode ensureATNNode(long stateNumber, ATNState state) {
        IATNNode node = stateToNode.get(stateNumber);
        if (node != null) {
            return node;
        }

        //int index = nodes.size();
        //stateToNode.put(stateNumber, index);
        //nodes.add(new IATNNode(stateNumber, String.valueOf(stateNumber), state.getStateType()));
        node = new IATNNode(stateNumber, String.valueOf(stateNumber), state.getStateType());
        stateToNode.put((long) stateNumber, node);

        // If this state transits to a new rule, create also a fake node for that rule.
        Transition[] transitions = state.getTransitions();
        if (transitions.length == 1 && transitions[0].target.getStateType() == ATNState.RULE_START) {
            long marker = ((long) state.stateNumber << 32) | transitions[0].target.stateNumber;

            // Type 0 is used to denote a rule.
            //nodes.add
            IATNNode n = new IATNNode(//currentRuleIndex--,
                                      //transitions[0].target.stateNumber,
                                      marker,
                                      ruleNames[transitions[0].target.ruleIndex],
                                      ATNState.INVALID_TYPE
            );
            stateToNode.put(marker, n);
        }

        return node;
    }

    public void compact() {
        this.stateToNode.forEach((k, v) -> {
            compact(v);
        });
    }

    /**
     * A --e--> B --a--> C
     * will be compacted to
     * A --a--> C
     *
     * @param node
     */
    public void compact(IATNNode node) {
        for (int i = 0; i < node.links.size(); i++) {
            IATNLink link = node.links.get(i);
            if (link.transition.getSerializationType() == Transition.EPSILON) {
                IATNNode targetNode = link.target;

                node.links.remove(i--);

                for (int j = 0; j < targetNode.links.size(); j++) {
                    IATNLink nextLink = targetNode.links.get(j);
                    nextLink.source = node;
                    node.links.add(nextLink);
                }
            } else {
                compact(link.target);
            }
        }
    }

    public void show() {
        IATNNode node = this.stateToNode.get(0L);
        Set<Long> visited = new HashSet<>();
        show(node, " ", visited);
    }

    public void show(IATNNode node, String indent, Set<Long> visited) {
        if (!visited.add(node.id)) {
            return;
        }
        if (node.isRule()) {
            // This is a rule transition.
            long id = node.id & 0xFFFFFFFFL;
            IATNNode n = this.stateToNode.get(id);
            show(n, indent + " ", visited);
        }

        for (IATNLink link : node.links) {
            System.out.println(String.format("%s%s --%s--> %s(%s)", indent, node.id, link.label, link.target.id, link.target.name));
            show(link.target, indent + " ", visited);
        }

        visited.remove(node.id);
    }
}
