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

package org.bithon.server.storage.jdbc.clickhouse;

import org.jooq.Configuration;
import org.jooq.ContextTransactionalCallable;
import org.jooq.ContextTransactionalRunnable;
import org.jooq.TransactionalCallable;
import org.jooq.TransactionalRunnable;
import org.jooq.impl.DefaultDSLContext;

/**
 * Transaction is not supported in ClickHouse. So, override the default implementation to avoid exception.
 *
 * @author Frank Chen
 * @date 24/4/22 6:06 PM
 */
public class ClickHouseDSLContext extends DefaultDSLContext {
    public ClickHouseDSLContext(Configuration configuration) {
        super(configuration);
    }

    @Override
    public <T> T transactionResult(ContextTransactionalCallable<T> transactional) {
        try {
            return transactional.run();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T transactionResult(TransactionalCallable<T> transactional) {
        try {
            return transactional.run(this.configuration());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void transaction(ContextTransactionalRunnable transactional) {
        try {
            transactional.run();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void transaction(TransactionalRunnable transactional) {
        try {
            transactional.run(this.configuration());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
