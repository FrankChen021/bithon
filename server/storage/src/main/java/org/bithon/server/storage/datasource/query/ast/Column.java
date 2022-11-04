package org.bithon.server.storage.datasource.query.ast;

/**
 * @author Frank Chen
 * @date 4/11/22 9:03 pm
 */
public class Column extends Name {

    public Column(String name) {
        super(name);
    }

    @Override
    public void accept(IASTVisitor visitor) {
        visitor.visit(this);
    }
}
