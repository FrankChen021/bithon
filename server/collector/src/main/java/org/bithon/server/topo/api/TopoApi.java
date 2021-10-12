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

package org.bithon.server.topo.api;

import org.bithon.component.db.dao.EndPointType;
import org.bithon.server.common.matcher.EqualMatcher;
import org.bithon.server.common.utils.datetime.TimeSpan;
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.DataSourceSchemaManager;
import org.bithon.server.metric.input.InputRow;
import org.bithon.server.metric.storage.DimensionCondition;
import org.bithon.server.metric.storage.IMetricReader;
import org.bithon.server.metric.storage.IMetricStorage;
import org.bithon.server.topo.service.EndpointBo;
import org.bithon.server.topo.service.Link;
import org.bithon.server.topo.service.Topo;
import org.bithon.server.topo.service.TopoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/20 21:39
 */
@CrossOrigin
@RestController
public class TopoApi {

    private final IMetricReader metricReader;
    private final DataSourceSchema topoSchema;

    public TopoApi(DataSourceSchemaManager schemaManager,
                   TopoService topoService,
                   IMetricStorage metricStorage) {
        this.topoSchema = schemaManager.getDataSourceSchema("topo-metrics");
        this.metricReader = metricStorage.createMetricReader(topoSchema);
    }

    @PostMapping("/api/topo/getApplicationTopo")
    public Topo getTopo(@Valid @RequestBody GetTopoRequest request) {
        TimeSpan start = TimeSpan.fromISO8601(request.getStartTimeISO8601());
        TimeSpan end = TimeSpan.fromISO8601(request.getEndTimeISO8601());
        List<Map<String, Object>> callees = metricReader.groupBy(start,
                                                                 end,
                                                                 topoSchema,
                                                                 Arrays.asList(new DimensionCondition("srcEndpoint",
                                                                                                      new EqualMatcher(
                                                                                                          request.getApplication())),
                                                                               new DimensionCondition("srcEndpointType",
                                                                                                      new EqualMatcher(
                                                                                                          EndPointType.APPLICATION
                                                                                                              .name()))),
                                                                 Arrays.asList("callCount",
                                                                               "avgResponseTime",
                                                                               "maxResponseTime",
                                                                               "minResponseTime"),
                                                                 Arrays.asList("dstEndpoint", "dstEndpointType"));

        int x = 300;
        int y = 300;
        int nodeHeight = 50;
        Topo topo = new Topo();
        EndpointBo thisApplication = new EndpointBo(EndPointType.APPLICATION,
                                                    request.getApplication(),
                                                    x,
                                           y + callees.size() / 2 * nodeHeight);
        topo.addEndpoint(thisApplication);

        for (Map<String, Object> callee : callees) {
            InputRow inputRow = new InputRow(callee);
            String dst = inputRow.getColAsString("dstEndpoint");
            EndPointType dstType = EndPointType.valueOf(EndPointType.class,
                                                        inputRow.getColAsString("dstEndpointType"));
            EndpointBo dstEndpoint = new EndpointBo(dstType, dst, x + 100, y);
            topo.addEndpoint(dstEndpoint);
            topo.addLink(Link.builder()
                             .srcEndpoint(thisApplication.getName())
                             .dstEndpoint(dstEndpoint.getName())
                             .avgResponseTime(inputRow.getColAsDouble("avgResponseTime", 0))
                             .maxResponseTime(inputRow.getColAsLong("maxResponseTime", 0))
                             .minResponseTime(inputRow.getColAsLong("minResponseTime", 0))
                             .callCount(inputRow.getColAsLong("callCount", 0))
                             .errorCount(inputRow.getColAsLong("errorCount", 0))
                             .build());
            y += nodeHeight;
        }

        List<Map<String, Object>> callers = metricReader.groupBy(start,
                                                                 end,
                                                                 topoSchema,
                                                                 Arrays.asList(new DimensionCondition("dstEndpoint",
                                                                                                      new EqualMatcher(
                                                                                                          request.getApplication())),
                                                                               new DimensionCondition("dstEndpointType",
                                                                                                      new EqualMatcher(
                                                                                                          EndPointType.APPLICATION
                                                                                                              .name()))),
                                                                 Arrays.asList("callCount",
                                                                               "avgResponseTime",
                                                                               "maxResponseTime",
                                                                               "minResponseTime"),
                                                                 Arrays.asList("srcEndpoint", "srcEndpointType"));

        y = 300;
        for (Map<String, Object> caller : callers) {
            InputRow inputRow = new InputRow(caller);
            String src = inputRow.getColAsString("srcEndpoint");
            EndPointType srcType = EndPointType.valueOf(EndPointType.class,
                                                        inputRow.getColAsString("srcEndpointType"));
            EndpointBo srcEndpoint = new EndpointBo(srcType, src, x - 100, y);
            topo.addEndpoint(srcEndpoint);
            topo.addLink(Link.builder()
                             .srcEndpoint(srcEndpoint.getName())
                             .dstEndpoint(thisApplication.getName())
                             .avgResponseTime(inputRow.getColAsDouble("avgResponseTime", 0))
                             .maxResponseTime(inputRow.getColAsLong("maxResponseTime", 0))
                             .minResponseTime(inputRow.getColAsLong("minResponseTime", 0))
                             .callCount(inputRow.getColAsLong("callCount", 0))
                             .errorCount(inputRow.getColAsLong("errorCount", 0))
                             .build());
            y += nodeHeight;
        }
        return topo;
    }
}
