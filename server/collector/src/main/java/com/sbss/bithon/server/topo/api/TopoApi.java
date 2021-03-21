package com.sbss.bithon.server.topo.api;

import com.sbss.bithon.component.db.dao.EndPointType;
import com.sbss.bithon.server.common.matcher.EqualMatcher;
import com.sbss.bithon.server.common.utils.datetime.TimeSpan;
import com.sbss.bithon.server.metric.DataSourceSchema;
import com.sbss.bithon.server.metric.DataSourceSchemaManager;
import com.sbss.bithon.server.metric.input.InputRow;
import com.sbss.bithon.server.metric.storage.DimensionCondition;
import com.sbss.bithon.server.metric.storage.IMetricReader;
import com.sbss.bithon.server.metric.storage.IMetricStorage;
import com.sbss.bithon.server.topo.service.EndpointBo;
import com.sbss.bithon.server.topo.service.Link;
import com.sbss.bithon.server.topo.service.Topo;
import com.sbss.bithon.server.topo.service.TopoService;
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

    private final DataSourceSchemaManager schemaManager;
    private final TopoService topoService;
    private final IMetricReader metricReader;
    private final DataSourceSchema topoSchema;

    public TopoApi(DataSourceSchemaManager schemaManager,
                   TopoService topoService,
                   IMetricStorage metricStorage) {
        this.schemaManager = schemaManager;
        this.topoService = topoService;
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
        EndpointBo caller = new EndpointBo(EndPointType.APPLICATION, request.getApplication(), x, y);
        Topo topo = new Topo();
        topo.addEndpoint(caller);
        callees.forEach(callee -> {
            InputRow inputRow = new InputRow(callee);
            String dst = inputRow.getColAsString("dstEndpoint");
            EndPointType dstType = EndPointType.valueOf(EndPointType.class,
                                                        inputRow.getColAsString("dstEndpointType"));
            EndpointBo dstEndpoint = new EndpointBo(dstType, dst, x + 100, y + 50);
            topo.addEndpoint(dstEndpoint);
            topo.addLink(Link.builder()
                             .srcEndpoint(caller.getName())
                             .dstEndpoint(dstEndpoint.getName())
                             .avgResponseTime(inputRow.getColAsDouble("avgResponseTime", 0))
                             .maxResponseTime(inputRow.getColAsLong("maxResponseTime", 0))
                             .maxResponseTime(inputRow.getColAsLong("minResponseTime", 0))
                             .callCount(inputRow.getColAsLong("callCount", 0))
                             .errorCount(inputRow.getColAsLong("errorCount", 0))
                             .build());
        });
        return topo;
    }
}
