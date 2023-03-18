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

package org.bithon.agent.plugin.mongodb.interceptor;

import com.mongodb.MongoNamespace;
import com.mongodb.WriteConcern;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.observability.metric.domain.mongo.MongoCommand;
import org.bson.BsonDocument;
import org.bson.FieldNameValidator;
import org.bson.codecs.Decoder;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/28 11:40
 */
public class Protocol {
    /**
     * {@link com.mongodb.connection.CommandProtocol#CommandProtocol(String database, BsonDocument, FieldNameValidator, Decoder)}
     */
    public static class CommandProtocol extends AbstractInterceptor {
        @Override
        public void onConstruct(AopContext aopContext) {
            IBithonObject obj = aopContext.getTargetAs();
            obj.setInjectedObject(new MongoCommand(aopContext.getArgAs(0),
                                                   MongoNamespace.COMMAND_COLLECTION_NAME,
                                                   "Command"));
        }
    }

    /**
     * {@link com.mongodb.connection.DeleteCommandProtocol#DeleteCommandProtocol(MongoNamespace, boolean, WriteConcern, List)}
     */
    public static class DeleteCommandProtocol extends AbstractInterceptor {
        @Override
        public void onConstruct(AopContext aopContext) {
            MongoNamespace ns = aopContext.getArgAs(0);
            IBithonObject bithonObject = aopContext.getTargetAs();
            bithonObject.setInjectedObject(new MongoCommand(ns.getDatabaseName(),
                                                            ns.getCollectionName(),
                                                            "DeleteCommand"));
        }
    }

    /**
     * {@link com.mongodb.connection.InsertCommandProtocol#InsertCommandProtocol(MongoNamespace, boolean, WriteConcern, Boolean, List)}
     */
    public static class InsertCommandProtocol extends AbstractInterceptor {
        @Override
        public void onConstruct(AopContext aopContext) {
            MongoNamespace ns = aopContext.getArgAs(0);
            IBithonObject bithonObject = aopContext.getTargetAs();
            bithonObject.setInjectedObject(new MongoCommand(ns.getDatabaseName(),
                                                            ns.getCollectionName(),
                                                            "InsertCommand"));
        }
    }

    /**
     * {@link com.mongodb.connection.UpdateCommandProtocol#UpdateCommandProtocol(MongoNamespace, boolean, WriteConcern, Boolean, List)}
     */
    public static class UpdateCommandProtocol extends AbstractInterceptor {
        @Override
        public void onConstruct(AopContext aopContext) {
            MongoNamespace ns = aopContext.getArgAs(0);
            IBithonObject bithonObject = aopContext.getTargetAs();
            bithonObject.setInjectedObject(new MongoCommand(ns.getDatabaseName(),
                                                            ns.getCollectionName(),
                                                            "UpdateCommand"));
        }
    }

    /**
     * {@link com.mongodb.connection.InsertProtocol#InsertProtocol(MongoNamespace, boolean, WriteConcern, List)}
     */
    public static class InsertProtocol extends AbstractInterceptor {
        @Override
        public void onConstruct(AopContext aopContext) {
            MongoNamespace ns = aopContext.getArgAs(0);
            IBithonObject bithonObject = aopContext.getTargetAs();
            bithonObject.setInjectedObject(new MongoCommand(ns.getDatabaseName(),
                                                            ns.getCollectionName(),
                                                            "Insert"));
        }
    }

    /**
     * {@link com.mongodb.connection.QueryProtocol#QueryProtocol(MongoNamespace, int, int, BsonDocument, BsonDocument, Decoder)}
     * {@link com.mongodb.connection.QueryProtocol#QueryProtocol(MongoNamespace, int, int, int, BsonDocument, BsonDocument, Decoder)}
     */
    public static class QueryProtocol extends AbstractInterceptor {
        @Override
        public void onConstruct(AopContext aopContext) {
            MongoNamespace ns = aopContext.getArgAs(0);
            IBithonObject bithonObject = aopContext.getTargetAs();
            bithonObject.setInjectedObject(new MongoCommand(ns.getDatabaseName(),
                                                            ns.getCollectionName(),
                                                            "Query"));
        }
    }

    /**
     * {@link com.mongodb.connection.DeleteProtocol#DeleteProtocol(MongoNamespace, boolean, WriteConcern, List)}
     */
    public static class DeleteProtocol extends AbstractInterceptor {
        @Override
        public void onConstruct(AopContext aopContext) {
            MongoNamespace ns = aopContext.getArgAs(0);
            IBithonObject bithonObject = aopContext.getTargetAs();
            bithonObject.setInjectedObject(new MongoCommand(ns.getDatabaseName(),
                                                            ns.getCollectionName(),
                                                            "Delete"));
        }
    }

    /**
     * {@link com.mongodb.connection.UpdateProtocol#UpdateProtocol(MongoNamespace, boolean, WriteConcern, List)}
     */
    public static class UpdateProtocol extends AbstractInterceptor {
        @Override
        public void onConstruct(AopContext aopContext) {
            MongoNamespace ns = aopContext.getArgAs(0);
            IBithonObject bithonObject = aopContext.getTargetAs();
            bithonObject.setInjectedObject(new MongoCommand(ns.getDatabaseName(),
                                                            ns.getCollectionName(),
                                                            "Update"));
        }
    }

    /**
     * {@link com.mongodb.connection.GetMoreProtocol#GetMoreProtocol(MongoNamespace, long, int, Decoder)}
     */
    public static class GetMoreProtocol extends AbstractInterceptor {
        @Override
        public void onConstruct(AopContext aopContext) {
            MongoNamespace ns = aopContext.getArgAs(0);
            IBithonObject bithonObject = aopContext.getTargetAs();
            bithonObject.setInjectedObject(new MongoCommand(ns.getDatabaseName(),
                                                            ns.getCollectionName(),
                                                            "GetMore"));
        }
    }

    /**
     * {@link com.mongodb.connection.KillCursorProtocol#KillCursorProtocol(MongoNamespace, List)}
     */
    public static class KillCursorProtocol extends AbstractInterceptor {
        @Override
        public void onConstruct(AopContext aopContext) {
            MongoNamespace ns = aopContext.getArgAs(0);
            IBithonObject bithonObject = aopContext.getTargetAs();
            bithonObject.setInjectedObject(new MongoCommand(ns.getDatabaseName(),
                                                            ns.getCollectionName(),
                                                            "KillCursor"));
        }
    }
}
