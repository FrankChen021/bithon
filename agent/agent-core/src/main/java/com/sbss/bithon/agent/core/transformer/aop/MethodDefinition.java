package com.sbss.bithon.agent.core.transformer.aop;

import shaded.net.bytebuddy.description.type.TypeDescription;

public class MethodDefinition {

    private boolean isStatic;

    private TypeDescription.Generic[] paramsDescription;

    public MethodDefinition(boolean isStatic, TypeDescription.Generic[] paramsDescription) {
        this.isStatic = isStatic;
        this.paramsDescription = paramsDescription;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public TypeDescription.Generic[] getParamsDescription() {
        return paramsDescription;
    }

    public void setParamsDescription(TypeDescription.Generic[] paramsDescription) {
        this.paramsDescription = paramsDescription;
    }
}
