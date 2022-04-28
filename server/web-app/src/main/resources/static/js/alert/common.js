class DataSourceSchemaManager {
    constructor() {
        this.m_schemas = null;
    }

    load(callback, async) {
        $.ajax({
            type: "POST",
            url: apiHost + "/api/datasource/schemas",
            async: async,
            dataType: "json",
            contentType: "application/json",
            success: (schemas) => {
                for (const dataSourceName in schemas) {
                    const dataSource = schemas[dataSourceName];

                    // remove appName and instance for alert editing
                    dataSource.dimensionsSpec = dataSource.dimensionsSpec.slice(2, dataSource.dimensionsSpec.length);

                    //
                    // convert to map
                    //
                    dataSource.dimensionsMap = {};
                    $.each(dataSource.dimensionsSpec, function (index, dimension) {
                        dataSource.dimensionsMap[dimension.name] = dimension;
                    });

                    //
                    // convert to map
                    //
                    dataSource.metricsMap = {};
                    $.each(dataSource.metricsSpec, function (index, metric) {
                        dataSource.metricsMap[metric.name] = metric;
                    });

                }

                console.log(schemas);

                this.m_schemas = schemas;
                if (callback != null)
                    callback(this.m_schemas);
            },
            error: function (data) {
                console.log(data);
                showMessage("错误" + data, "error");
            }
        });
    }

    getDataSourceSchema(dataSourceName) {
        return this.m_schemas[dataSourceName];
    }

    getDataSourceSchemas() {
        if (this.m_schemas == null) {
            this.load(null, false);
        }
        return this.m_schemas;
    }

    isLoaded() {
        return this.m_schemas != null;
    }

    forEach(callback) {
        for (const dataSourceName in this.m_schemas) {
            callback(dataSourceName, this.m_schemas[dataSourceName]);
        }
    }
}

function loadAlert(alertId, callback) {
    $.ajax({
        type: "POST",
        url: "/alerting/api/alert/getAlertById",
        async: true,
        data: JSON.stringify({
            alertId: alertId
        }),
        dataType: "json",
        contentType: "application/json",
        success: function (data) {
            console.log(data);
            if (data.code > 200) {
                showMessage(data.message, "Error");
                return;
            }
            callback(data.data);
        },
        error: function (data) {
            console.log(data);
            showMessage(data.responseJSON.message, "Error");
        }
    });
}

function addCondition(schema, condition) {
    const id = condition.id;
    const dataSource = schema[condition.dataSource];
    if (dataSource === undefined) {
        showMessage(`Data source ${condition.dataSource} does not exist`);
        return;
    }
    const container = $("#conditions");

    // create a new row
    const conditionRow = $(container).find(".condition-row:first").clone();
    $(conditionRow).attr("id", "condition-" + id);
    $(conditionRow).find(".condition-sn").val(id);
    $(conditionRow).find(".condition-datasource").val(condition.dataSource);

    let afterCond = $(conditionRow).find(".condition-datasource").parent().parent();
    $.each(condition.dimensions, function (index, dimensionCond) {
        const dimensionCell = $(
            '<div class="multiSelectBox">             ' +
            '    <div class="input-group">                                            ' +
            '        <input class="form-control" style="width: 150px; value=""/> ' +
            '    </div>                                                               ' +
            '</div>                                                                   '
        );
        $(dimensionCell).find("input").val(dimensionCond.matcher.pattern);
        afterCond.after(dimensionCell);
        afterCond = dimensionCell;
    });
    $.each(condition.metrics, function (index, metricCond) {
        const metricSpec = dataSource.metricsMap[metricCond.metric];

        //text += metricSpec.displayText;
        //text += metricCond.type;
        //text += metricCond.expected;
        $(conditionRow).find(".condition-metric").val(metricSpec.displayText);
        $(conditionRow).find(".condition-type").val(metricCond.type);
        $(conditionRow).find(".condition-threshold").val(metricCond.expected);
        $(conditionRow).find(".condition-unit").val(metricSpec.unit);
    });

    $(conditionRow).find(".show-metric").click(function () {
        const metricComponent = new MetricComponent();
        metricComponent.showDialog({
            dataSourceName: condition.dataSource,
            dimensions: condition.dimensions,
            metricCondition: condition.metrics[0],
            end: Date.now(),
            hours: 3,
            metricSpec: dataSource.metricsMap[condition.metrics[0].metric],
            threshold: condition.metrics[0].expected
        });
    });
    $(conditionRow).css('display', '');
    $(container).append(conditionRow);
}

function checkThreshold(object) {
    if (checkNumberInvaild(object.value)) {
        showMessage("Invalid threshold");
        return false;
    }
}

function isDingUrlValid(url) {
    if (url != null && url !== "") {
        return url.indexOf("https://oapi.dingtalk.com/robot/send") === 0;
    }
    return false;
}

