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


import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.server.common.utils.datetime.TimeSpan;
import org.bithon.server.event.sink.EventMessage;
import org.bithon.server.event.storage.Event;
import org.bithon.server.event.storage.IEventCleaner;
import org.bithon.server.event.storage.IEventReader;
import org.bithon.server.event.storage.IEventStorage;
import org.bithon.server.event.storage.IEventWriter;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.impl.DSL;
import org.jooq.impl.ThreadLocalTransactionProvider;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 4:19 下午
 */
@JsonTypeName("jdbc")
public class EventJdbcStorage implements IEventStorage {

    protected final DSLContext dslContext;
    protected final ObjectMapper objectMapper;

    @JsonCreator
    public EventJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) DSLContext dslContext,
                            @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
    }

    @Override
    public void initialize() {
        this.dslContext.createTableIfNotExists(Tables.BITHON_EVENT).columns(Tables.BITHON_EVENT.fields()).indexes(Tables.BITHON_EVENT.getIndexes()).execute();
    }

    @Override
    public IEventWriter createWriter() {
        return new EventWriter(dslContext, objectMapper);
    }

    @Override
    public IEventReader createReader() {
        return new EventReader(dslContext);
    }

    @Override
    public IEventCleaner createCleaner() {
        return timestamp -> dslContext.delete(Tables.BITHON_EVENT).where(Tables.BITHON_EVENT.TIMESTAMP.le(new Timestamp(timestamp))).execute();
    }

    private static class EventWriter implements IEventWriter {
        private final DSLContext dslContext;
        private final ObjectMapper om;

        private EventWriter(DSLContext dslContext, ObjectMapper om) {
            this.dslContext = DSL.using(dslContext.configuration().derive(new ThreadLocalTransactionProvider(dslContext.configuration().connectionProvider())));

            this.om = om;
        }

        @Override
        public void close() {
            dslContext.close();
        }

        @Override
        public void write(Collection<EventMessage> eventMessage) {
            List<Query> queries = eventMessage.stream()
                                              .map(message -> dslContext.insertInto(Tables.BITHON_EVENT)
                                                                        .set(Tables.BITHON_EVENT.APPNAME, message.getAppName())
                                                                        .set(Tables.BITHON_EVENT.INSTANCENAME, message.getInstanceName())
                                                                        .set(Tables.BITHON_EVENT.TYPE, message.getType())
                                                                        .set(Tables.BITHON_EVENT.ARGUMENTS, message.getJsonArgs())
                                                                        .set(Tables.BITHON_EVENT.TIMESTAMP, new Timestamp(message.getTimestamp())))
                                              .collect(Collectors.toList());
            dslContext.batch(queries).execute();
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

        @Override
        public List<Event> getEventList(String application, TimeSpan start, TimeSpan end, int pageNumber, int pageSize) {
            return dslContext.selectFrom(Tables.BITHON_EVENT)
                             .where(Tables.BITHON_EVENT.TIMESTAMP.ge(start.toTimestamp()))
                             .and(Tables.BITHON_EVENT.TIMESTAMP.lt(end.toTimestamp()))
                             .and(Tables.BITHON_EVENT.APPNAME.eq(application))
                             .orderBy(Tables.BITHON_EVENT.TIMESTAMP.desc())
                             .offset(pageNumber * pageSize)
                             .limit(pageSize)
                             .fetch((r) -> {
                                 Event e = new Event();
                                 e.setApplication(r.getAppname());
                                 e.setArgs(r.getArguments());
                                 e.setInstance(r.getInstancename());
                                 e.setType(r.getType());
                                 e.setTimestamp(r.getTimestamp().getTime());
                                 return e;
                             });
        }

        @Override
        public int getEventListSize(String application, TimeSpan start, TimeSpan end) {
            return (int) dslContext.select(DSL.count())
                                   .from(Tables.BITHON_EVENT)
                                   .where(Tables.BITHON_EVENT.TIMESTAMP.ge(start.toTimestamp()))
                                   .and(Tables.BITHON_EVENT.TIMESTAMP.lt(end.toTimestamp()))
                                   .and(Tables.BITHON_EVENT.APPNAME.eq(application))
                                   .fetchOne(0);
        }
    }
}
