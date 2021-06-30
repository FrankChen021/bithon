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

package com.sbss.bithon.agent.rpc.brpc.metrics;

import cn.bithon.rpc.IService;
import cn.bithon.rpc.Oneway;
import com.sbss.bithon.agent.rpc.brpc.BrpcMessageHeader;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/27 19:57
 */
public interface IMetricCollector extends IService {

    @Oneway
    void sendWebRequest(BrpcMessageHeader header, List<BrpcWebRequestMetricMessage> messages);

    void sendJvm(BrpcMessageHeader header, List<BrpcJvmMetricMessage> messages);

    @Oneway
    void sendJvmGc(BrpcMessageHeader header, List<BrpcJvmGcMetricMessage> messages);

    @Oneway
    void sendWebServer(BrpcMessageHeader header, List<BrpcWebServerMetricMessage> messages);

    @Oneway
    void sendException(BrpcMessageHeader header, List<BrpcExceptionMetricMessage> messages);

    @Oneway
    void sendHttpClient(BrpcMessageHeader header, List<BrpcHttpClientMetricMessage> messages);

    @Oneway
    void sendThreadPool(BrpcMessageHeader header, List<BrpcThreadPoolMetricMessage> messages);

    @Oneway
    void sendJdbc(BrpcMessageHeader header, List<BrpcJdbcPoolMetricMessage> messages);

    @Oneway
    void sendRedis(BrpcMessageHeader header, List<BrpcRedisMetricMessage> messages);

    @Oneway
    void sendSql(BrpcMessageHeader header, List<BrpcSqlMetricMessage> messages);

    @Oneway
    void sendMongoDb(BrpcMessageHeader header, List<BrpcMongoDbMetricMessage> messages);
}
