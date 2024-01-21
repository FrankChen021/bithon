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
