package org.bithon.server.storage.datasource;

/**
 * @author Frank Chen
 * @date 29/10/22 10:47 pm
 */
public interface IColumnSpec {

    /**
     * the name in the storage.
     * can NOT be null
     */
    String getName();
}
