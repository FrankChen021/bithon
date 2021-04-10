/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.server.event.storage.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbss.bithon.component.db.jooq.Indexes;
import com.sbss.bithon.component.db.jooq.Tables;
import com.sbss.bithon.server.event.handler.EventMessage;
import com.sbss.bithon.server.event.storage.IEventReader;
import com.sbss.bithon.server.event.storage.IEventStorage;
import com.sbss.bithon.server.event.storage.IEventWriter;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.ThreadLocalTransactionProvider;

import java.io.IOException;
import java.sql.Timestamp;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 4:19 下午
 */
public class EventJdbcStorage implements IEventStorage {

    private final DSLContext dslContext;
    private final ObjectMapper objectMapper;

    public EventJdbcStorage(DSLContext dslContext, ObjectMapper objectMapper) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;

        this.dslContext.createTableIfNotExists(Tables.BITHON_EVENT)
                       .columns(Tables.BITHON_EVENT.ID,
                                Tables.BITHON_EVENT.APP_NAME,
                                Tables.BITHON_EVENT.INSTANCE_NAME,
                                Tables.BITHON_EVENT.TYPE,
                                Tables.BITHON_EVENT.ARGUMENTS,
                                Tables.BITHON_EVENT.TIMESTAMP)
                       .indexes(Indexes.BITHON_EVENT_IDX_APPNAME,
                                Indexes.BITHON_EVENT_IDX_INSTANCENAME,
                                Indexes.BITHON_EVENT_IDX_TYPE,
                                Indexes.BITHON_EVENT_IDX_TIMESTAMP)
                       .execute();
    }

    @Override
    public IEventWriter createWriter() {
        return new EventWriter(dslContext, objectMapper);
    }

    @Override
    public IEventReader createReader() {
        return new EventReader(dslContext);
    }

    private static class EventWriter implements IEventWriter {
        private final DSLContext dslContext;
        private final ObjectMapper om;

        private EventWriter(DSLContext dslContext, ObjectMapper om) {
            this.dslContext = DSL.using(dslContext
                                            .configuration()
                                            .derive(new ThreadLocalTransactionProvider(dslContext.configuration()
                                                                                                 .connectionProvider())));

            this.om = om;
        }

        @Override
        public void close() {
            dslContext.close();
        }

        @Override
        public void write(EventMessage eventMessage) throws IOException {
            dslContext.insertInto(Tables.BITHON_EVENT)
                      .set(Tables.BITHON_EVENT.APP_NAME, eventMessage.getAppName())
                      .set(Tables.BITHON_EVENT.INSTANCE_NAME, eventMessage.getInstanceName())
                      .set(Tables.BITHON_EVENT.TYPE, eventMessage.getType())
                      .set(Tables.BITHON_EVENT.ARGUMENTS, om.writeValueAsString(eventMessage.getArgs()))
                      .set(Tables.BITHON_EVENT.TIMESTAMP, new Timestamp(eventMessage.getTimestamp()))
                      .execute();
        }
    }

    private static class EventReader implements IEventReader {
        private final DSLContext dslContext;

        private EventReader(DSLContext dslContext) {
            this.dslContext = dslContext;
        }

        @Override
        public void close() {
            dslContext.close();
        }
    }
}
