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

package org.bithon.server.web.service.topo.api;

import jakarta.validation.Valid;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.SchemaManager;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.InputRow;
import org.bithon.server.storage.datasource.query.IDataSourceReader;
import org.bithon.server.storage.datasource.query.Query;
import org.bithon.server.storage.meta.EndPointType;
import org.bithon.server.storage.metrics.Interval;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.topo.service.EndpointBo;
import org.bithon.server.web.service.topo.service.Link;
import org.bithon.server.web.service.topo.service.Topo;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/20 21:39
 */
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class TopoApi {

    private final SchemaManager schemaManager;

    public TopoApi(SchemaManager schemaManager) {
        this.schemaManager = schemaManager;
    }

    @PostMapping("/api/topo/getApplicationTopo")
    public Topo getTopo(@Valid @RequestBody GetTopoRequest request) throws IOException {
        ISchema topoSchema = schemaManager.getSchema("topo-metrics");

        // since the min granularity is minute, round down the timestamp to minute
        // and notice that the 'end' parameter is inclusive, so the round down has no impact on the query range
        TimeSpan start = new TimeSpan(TimeSpan.fromISO8601(request.getStartTimeISO8601()).getMilliseconds() / 60_000 * 60_000);
        TimeSpan end = new TimeSpan((TimeSpan.fromISO8601(request.getEndTimeISO8601()).getMilliseconds()) / 60_000 * 60_000);

        Query calleeQuery = Query.builder()
                                 .schema(topoSchema)
                                 .selectColumns(Stream.of("dstEndpoint",
                                                          "dstEndpointType",
                                                          "callCount",
                                                          "avgResponseTime",
                                                          "maxResponseTime",
                                                          "minResponseTime")
                                                      .map((column) -> {
                                                          IColumn spec = topoSchema.getColumnByName(column);
                                                          return spec.getResultColumn();
                                                      })
                                                      .collect(Collectors.toList()))
                                 .filter(new LogicalExpression.AND(new ComparisonExpression.EQ(new IdentifierExpression("srcEndpoint"),
                                                                                               LiteralExpression.create(request.getApplication())),
                                                                   new ComparisonExpression.EQ(new IdentifierExpression("srcEndpointType"),
                                                                                               LiteralExpression.create(EndPointType.APPLICATION.name()))))
                                 .interval(Interval.of(start, end))
                                 .groupBy(Arrays.asList("dstEndpoint", "dstEndpointType"))
                                 .build();

        try (IDataSourceReader dataSourceReader = topoSchema.getDataStoreSpec().createReader()) {
            List<Map<String, Object>> callees = (List<Map<String, Object>>) dataSourceReader.groupBy(calleeQuery);

            int x = 300;
            int y = 300;
            int nodeHeight = 50;
            Topo topo = new Topo();
            EndpointBo thisApplication = new EndpointBo("application",
                                                        request.getApplication(),
                                                        x,
                                                        y + callees.size() / 2L * nodeHeight);
            topo.addEndpoint(thisApplication);

            for (Map<String, Object> callee : callees) {
                IInputRow inputRow = new InputRow(callee);
                String dst = inputRow.getColAsString("dstEndpoint");
                String dstType = inputRow.getColAsString("dstEndpointType");
                EndpointBo dstEndpoint = new EndpointBo(dstType, dst, x + 100, y);
                topo.addEndpoint(dstEndpoint);
                topo.addLink(Link.builder()
                                 .srcEndpoint(thisApplication.getId())
                                 .dstEndpoint(dstEndpoint.getId())
                                 .avgResponseTime(inputRow.getColAsDouble("avgResponseTime", 0))
                                 .maxResponseTime(inputRow.getColAsLong("maxResponseTime", 0))
                                 .minResponseTime(inputRow.getColAsLong("minResponseTime", 0))
                                 .callCount(inputRow.getColAsLong("callCount", 0))
                                 .errorCount(inputRow.getColAsLong("errorCount", 0))
                                 .build());
                y += nodeHeight;
            }

            Query callerQuery = Query.builder()
                                     .schema(topoSchema)
                                     .selectColumns(Stream.of("srcEndpoint",
                                                              "srcEndpointType",
                                                              "callCount",
                                                              "avgResponseTime",
                                                              "maxResponseTime",
                                                              "minResponseTime")
                                                          .map((column) -> {
                                                              IColumn spec = topoSchema.getColumnByName(column);
                                                              return spec.getResultColumn();
                                                          })
                                                          .collect(Collectors.toList()))
                                     .filter(new LogicalExpression.AND(new ComparisonExpression.EQ(new IdentifierExpression("dstEndpoint"),
                                                                                                   LiteralExpression.create(request.getApplication())),
                                                                       new ComparisonExpression.EQ(new IdentifierExpression("dstEndpointType"),
                                                                                                   LiteralExpression.create(EndPointType.APPLICATION.name()))))
                                     .interval(Interval.of(start, end))
                                     .groupBy(Arrays.asList("srcEndpoint", "srcEndpointType")).build();
            List<Map<String, Object>> callers = (List<Map<String, Object>>) dataSourceReader.groupBy(callerQuery);

            y = 300;
            for (Map<String, Object> caller : callers) {
                IInputRow inputRow = new InputRow(caller);
                String src = inputRow.getColAsString("srcEndpoint");
                String srcType = inputRow.getColAsString("srcEndpointType");
                EndpointBo srcEndpoint = new EndpointBo(srcType, src, x - 100, y);
                topo.addEndpoint(srcEndpoint);
                topo.addLink(Link.builder()
                                 .srcEndpoint(srcEndpoint.getId())
                                 .dstEndpoint(thisApplication.getId())
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
}
