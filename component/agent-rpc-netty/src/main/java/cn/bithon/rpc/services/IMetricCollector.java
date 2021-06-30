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

package cn.bithon.rpc.services;

import cn.bithon.rpc.IService;
import cn.bithon.rpc.Oneway;
import cn.bithon.rpc.services.metrics.ExceptionMetricMessage;
import cn.bithon.rpc.services.metrics.HttpClientMetricMessage;
import cn.bithon.rpc.services.metrics.JdbcPoolMetricMessage;
import cn.bithon.rpc.services.metrics.JvmGcMetricMessage;
import cn.bithon.rpc.services.metrics.JvmMetricMessage;
import cn.bithon.rpc.services.metrics.MongoDbMetricMessage;
import cn.bithon.rpc.services.metrics.RedisMetricMessage;
import cn.bithon.rpc.services.metrics.SqlMetricMessage;
import cn.bithon.rpc.services.metrics.ThreadPoolMetricMessage;
import cn.bithon.rpc.services.metrics.WebRequestMetricMessage;
import cn.bithon.rpc.services.metrics.WebServerMetricMessage;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/27 19:57
 */
public interface IMetricCollector extends IService {

    @Oneway
    void sendWebRequest(MessageHeader header, List<WebRequestMetricMessage> messages);

    @Oneway
    void sendJvm(MessageHeader header, List<JvmMetricMessage> messages);

    @Oneway
    void sendJvmGc(MessageHeader header, List<JvmGcMetricMessage> messages);

    @Oneway
    void sendWebServer(MessageHeader header, List<WebServerMetricMessage> messages);

    @Oneway
    void sendException(MessageHeader header, List<ExceptionMetricMessage> messages);

    @Oneway
    void sendHttpClient(MessageHeader header, List<HttpClientMetricMessage> messages);

    @Oneway
    void sendThreadPool(MessageHeader header, List<ThreadPoolMetricMessage> messages);

    @Oneway
    void sendJdbc(MessageHeader header, List<JdbcPoolMetricMessage> messages);

    @Oneway
    void sendRedis(MessageHeader header, List<RedisMetricMessage> messages);

    @Oneway
    void sendSql(MessageHeader header, List<SqlMetricMessage> messages);

    @Oneway
    void sendMongoDb(MessageHeader header, List<MongoDbMetricMessage> messages);
}
