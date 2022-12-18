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

package org.bithon.server.storage.jdbc.event;

import org.bithon.server.storage.event.EventMessage;
import org.bithon.server.storage.event.IEventWriter;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.ThreadLocalTransactionProvider;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/29 21:06
 */
public class EventJdbcWriter implements IEventWriter {
    private final DSLContext dslContext;

    EventJdbcWriter(DSLContext dslContext) {
        this.dslContext = DSL.using(dslContext.configuration().derive(new ThreadLocalTransactionProvider(dslContext.configuration().connectionProvider())));
    }

    @Override
    public void close() {
        dslContext.close();
    }

    @Override
    public void write(List<EventMessage> eventMessages) {
        BatchBindStep step = dslContext.batch(dslContext.insertInto(Tables.BITHON_EVENT,
                                                                    Tables.BITHON_EVENT.TIMESTAMP,
                                                                    Tables.BITHON_EVENT.APPNAME,
                                                                    Tables.BITHON_EVENT.INSTANCENAME,
                                                                    Tables.BITHON_EVENT.TYPE,
                                                                    Tables.BITHON_EVENT.ARGUMENTS)
                                                        .values((Timestamp) null,
                                                                null,
                                                                null,
                                                                null,
                                                                null));

        for (EventMessage message : eventMessages) {
            step.bind(new Timestamp(message.getTimestamp()),
                      message.getAppName(),
                      message.getInstanceName(),
                      message.getType(),
                      message.getJsonArgs());
        }

        step.execute();
    }
}
