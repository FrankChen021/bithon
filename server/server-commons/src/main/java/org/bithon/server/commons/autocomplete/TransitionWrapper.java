package org.bithon.server.commons.autocomplete;

import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.Transition;

class TransitionWrapper {
    private final ATNState source;
    private final Transition transition;

    public TransitionWrapper(ATNState source, Transition transition) {
        this.source = source;
        this.transition = transition;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((transition == null) ? 0 : transition.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TransitionWrapper other = (TransitionWrapper) obj;
        if (source == null) {
            if (other.source != null) {
                return false;
            }
        } else if (!source.equals(other.source)) {
            return false;
        }
        if (transition == null) {
            return other.transition == null;
        }
        else {
            return transition.equals(other.transition);
        }
    }

    @Override
    public String toString() {
        return transition.getClass().getSimpleName() + " from " + source + " to " + transition.target;
    }
}
