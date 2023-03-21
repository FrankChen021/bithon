class Dashboard {
    constructor(containerId, appName, dashboardName, defaultInterval, schemaApi) {
        this._schemaApi = schemaApi;
        this._appName = appName;
        this._dashboardName = dashboardName;
        this._defaultInterval = defaultInterval;

        // View
        this._containerId = containerId;
        this._container = $('#' + containerId);
        this._stackLayoutRowFill = 0;
        this._stackLayoutRow = $('<div style="display: flex"></div>');
        this._container.append(this._stackLayoutRow);
        this._timeSelector = null;

        // Model
        this._chartComponents = {};
        this._chartDescriptors = {};
        this._selectedInterval = null;

        // Y Axis Formatter
        this._formatters = {};
        this._formatters['binary_byte'] = (v) => v.formatBinaryByte();
        this._formatters['compact_number'] = (v) => v.formatCompactNumber();
        this._formatters['percentage'] = (v) => v === 'NaN' ? '0%' : v.formatWithNoTrailingZeros(2) + '%';
        this._formatters['nanosecond'] = (v) => nanoFormat(v, 2);
        this._formatters['millisecond'] = (v) => milliFormat(v, 2);
        this._formatters['microsecond'] = (v) => microFormat(v, 2);
        this._formatters['byte_rate'] = (v) => v.formatBinaryByte() + "/s";
        this._formatters['rate'] = (v) => v.formatCompactNumber() + "/s";
        this._formatters['dateTime'] = (v) => new Date(v).format('yyyy-MM-dd hh:mm:ss');
        this._formatters['shortDateTime'] = (v) => new Date(v).format('MM-dd hh:mm:ss');
        this._formatters['timeDuration'] = (v) => v.formatTimeDuration();
        this._formatters['timeDiff'] = (v) => v.formatTimeDiff();

        // deprecated
        this._formatters['nanoFormatter'] = (v) => nanoFormat(v, 2);
    }

    // PUBLIC
    load(dashboard) {
        this._dashboard = dashboard;

        //
        // Create App Filter
        // The 'appSelector' element is defined in app-layout.html
        //
        this.vFilter = new AppSelector({
            parentId: 'filterBar',
            intervalProvider: () => this.getSelectedTimeInterval(),
        }).registerChangedListener((name, value) => {
            if (name === 'application') {
                g_SelectedApp = value;

                let url = `/web/metrics/${this._dashboardName}?appName=${value}`;
                if (g_MetricSelectedInterval !== undefined) {
                    url += `&interval=${g_MetricSelectedInterval}`;
                }
                window.history.pushState('', '', url);
            }

            this.refreshDashboard();
        }).createAppSelector();

        //
        // dataSource --> Charts
        //
        const dataSource2Charts = {};
        $.each(this._dashboard.charts, (index, chartDescriptor) => {
            const chartId = 'chart_' + index;
            chartDescriptor['id'] = chartId;

            // set up a data source to charts mapping
            const dataSourceName = chartDescriptor.dataSource;
            if (dataSourceName !== undefined) {
                if (dataSource2Charts[dataSourceName] == null) {
                    dataSource2Charts[dataSourceName] = [];
                }
                dataSource2Charts[dataSourceName].push(chartId);
            }
        });

        //
        // Create AutoRefresher
        // the filterBarForm is defined in the app-layout.html
        //
        const parent = $('#filterBarForm');
        new AutoRefresher({
            timerLength: 10
        }).childOf(parent).registerRefreshListener(() => {
            this._selectedInterval = this._timeSelector.getInterval();
            this.refreshDashboard();
        });

        //
        // Create TimeInterval
        //
        this._timeSelector = new TimeInterval(this._defaultInterval).childOf(parent).registerIntervalChangedListener((selectedModel) => {
            g_MetricSelectedInterval = selectedModel.id;
            this._selectedInterval = this._timeSelector.getInterval();
            this.refreshDashboard();

            let url = `/web/metrics/${this._dashboardName}?appName=${g_SelectedApp}`;
            if (g_MetricSelectedInterval !== undefined) {
                url += `&interval=${g_MetricSelectedInterval}`;
            }
            window.history.pushState('', '', url);
        });
        this._selectedInterval = this._timeSelector.getInterval();

        $.each(dashboard.charts, (index, chartDescriptor) => {

            this.layout(chartDescriptor.id, chartDescriptor.width * 3);

            // create chart
            this.createChartComponent(chartDescriptor.id, chartDescriptor)
                .setOpenHandler(() => {
                    this.openChart(chartDescriptor.id);
                });

            this._chartDescriptors[chartDescriptor.id] = chartDescriptor;
        });

        // Connect the charts to show tooltip synchronize
        const charts = [];
        for (const id in this._chartComponents) {
            try {
                const chartInstance = this._chartComponents[id].getChart();
                charts.push(chartInstance);

                // Ignore brush event for connection
                chartInstance.on('brushEnd', (params) => params.escapeConnect = true);
                chartInstance.on('brushSelected', (params) => params.escapeConnect = true);
            } catch (ignored) {
                // this chart component might be TableComponent
            }
        }
        echarts.connect(charts);

        const dataSourceFilter = this._dashboard.charts[0].dataSource;

        //
        // Loaded Dimension Filter
        // This is legacy implementation. Should be refactored to decouple the filter from the dataSource field
        //
        let hasSchema = false;
        for (const dataSourceName in dataSource2Charts) {
            hasSchema = true;
            this._schemaApi.getSchema({
                name: dataSourceName,
                successCallback: (schema) => {
                    let index;
                    if (schema.name === dataSourceFilter) {
                        // create filters for dimensions
                        this.vFilter.createFilterFromSchema(schema);
                    }

                    //
                    // This should be changed in future
                    // converts metricsSpec from Array to Map
                    //
                    const metricMap = {};
                    for (index = 0; index < schema.metricsSpec.length; index++) {
                        const metric = schema.metricsSpec[index];
                        metricMap[metric.name] = metric;
                    }
                    schema.metricsSpec = metricMap;

                    //
                    // Build Transformers
                    //
                    this.createTransformers(schema);

                    // refresh dashboard after schema has been retrieved
                    // because there may be value transformers on different units
                    const charts = dataSource2Charts[schema.name];
                    $.each(charts, (index, chartId) => {
                        this.refreshChart(this._chartDescriptors[chartId],
                            this._chartComponents[chartId],
                            this.getSelectedTimeInterval());
                    });

                    // init the detail
                    $.each(dashboard.charts, (index, chartDescriptor) => {
                        this.createChartDetail(chartDescriptor);
                    });
                },
                errorCallback: (error) => {
                }
            });
        }
        if (!hasSchema) {
            this.refreshDashboard();
        }
    }

    createChartDetail(chartDescriptor) {
        // create detail view for this chart
        if (chartDescriptor.details == null) {
            return;
        }

        const detailViewId = chartDescriptor.id + '_detailView';
        if (chartDescriptor.details.query === undefined) {
            //
            // Convert from old definition
            //
            chartDescriptor.details.query = {
                dataSource: chartDescriptor.dataSource
            };

            if (chartDescriptor.details.columns !== undefined) {
                const pair = this.#fromDetailV2(chartDescriptor);
                chartDescriptor.details.query.fields = pair[0];
                chartDescriptor.details.columns = pair[1];
            } else {
                const pair = this.#fromDetailV1(chartDescriptor);
                chartDescriptor.details.query.fields = pair[0];
                chartDescriptor.details.columns = pair[1];
            }

            chartDescriptor.details.query.order = chartDescriptor.details.orderBy;
            chartDescriptor.details.query.filters = chartDescriptor.details.filters;

            if (chartDescriptor.details.groupBy !== undefined && chartDescriptor.details.groupBy.length > 0) {
                chartDescriptor.details.query.type = "groupBy";
            } else {
                chartDescriptor.details.query.type = "list";
                chartDescriptor.details.pagination = [25, 50, 100];
            }
        }

        //
        // Use the format definition in the chart as default
        //
        for (let i = 0; i < chartDescriptor.details.columns.length; i++) {
            let column = chartDescriptor.details.columns[i];
            if (column.format !== undefined) {
                continue;
            }

            let columnName;
            if (typeof column === 'string') {
                columnName = column;
            } else {
                columnName = column.name;
            }

            const chartColumn = chartDescriptor.columnMap[columnName];
            if (chartColumn !== undefined && chartColumn.format !== undefined) {
                if (typeof column === 'string') {
                    chartDescriptor.details.columns[i] = {name: columnName};
                    column = chartDescriptor.details.columns[i];
                }
                column.format = chartColumn.format;
            } else {
                // this column might be a dimension
            }
        }
        if (chartDescriptor.details.query.dataSource === undefined) {
            chartDescriptor.details.query.dataSource = chartDescriptor.dataSource;
        }

        let buttons = undefined;
        if (chartDescriptor.details.tracing !== undefined) {
            buttons = [{
                title: "Tracing Log",
                text: "Search...",
                onClick: (index, row, start, end) => this.#openTraceSearchPage(chartDescriptor, start, end, row)
            }]
        }

        const chartComponent = this._chartComponents[chartDescriptor.id];

        // Create component
        const detailTableComponent = this.createTableComponent(detailViewId,
            chartComponent.getUIContainer(),
            chartDescriptor.details,
            true,
            buttons
        )

        // Bind event on parent component
        chartComponent.setSelectionHandler(
            (chartOption, startIndex, endIndex) => {
                // get the time range
                const start = chartOption.timestamp.start;
                const interval = chartOption.timestamp.interval;

                const startTimestamp = start + startIndex * interval;
                const endTimestamp = start + endIndex * interval;

                const startISO8601 = moment(startTimestamp).utc().toISOString();
                const endISO8601 = moment(endTimestamp).utc().toISOString();

                this.refreshTable(chartDescriptor.details.query,
                    detailTableComponent,
                    {
                        start: startISO8601,
                        end: endISO8601
                    });
            },
            () => {
                detailTableComponent.clear();
                detailTableComponent.show();
            },
            () => {
                detailTableComponent.hide();
            },
            () => {
                detailTableComponent.clear();
            });
    }

    #fromDetailV2(chartDescriptor) {

        const tableColumns = chartDescriptor.details.columns.map((column) => {
            let columnName;
            if (typeof column === 'object') {
                columnName = column.name;
            } else {
                columnName = column;
            }

            return {
                name: columnName,
                format: column.formatter,
                sortable: column.sortable | true
            };
        });

        const tableFields = [];
        chartDescriptor.details.groupBy.forEach((groupBy) => tableFields.push(groupBy));
        chartDescriptor.details.aggregators.forEach((aggregator) => tableFields.push({
            name: aggregator.name,
            field: aggregator.field,
            aggregator: aggregator.type
        }));

        return [tableFields, tableColumns];
    }

    // create columns from groupBy/metrics/
    #fromDetailV1(chartDescriptor) {
        const fields = []
        const columns = [];

        $.each(chartDescriptor.details.dimensions, (index, dimension) => {
            fields.push(dimension.name);
            columns.push({
                name: dimension.name,
                format: dimension.formatter,

                // a parameter from template formatter
                template: dimension.template,

                sortable: dimension.sortable | true
            });
        });

        $.each(chartDescriptor.details.groupBy, (index, groupBy) => {
            if (typeof groupBy === 'object') {
                fields.push(groupBy.name);
                columns.push({name: groupBy.name, format: groupBy.formatter, sortable: true});
            } else {
                fields.push(groupBy);
                columns.push({name: groupBy, sortable: true});
            }
        });

        //
        // create columns for metrics
        //
        $.each(chartDescriptor.details.metrics, (index, metric) => {
            let metricName;
            if (typeof metric === 'object') {
                fields.push(metric.name);
                columns.push({name: metric.name, format: metric.formatter, sortable: true});
            } else {
                fields.push(metric);
                columns.push({name: metric, sortable: true});
            }
        });

        return [fields, columns];
    }

    /**
     * Tracing Spec Example On Dashboard
     *
     *  "tracing": {
     *    "dimensionMaps": {
     *      "cluster": "tags.clickhouse.cluster",
     *      "user": "tags.clickhouse.user",
     *      "queryType": "tags.clickhouse.queryType",
     *      "exceptionCode": {
     *        "type" : "switch",
     *        "cases" : {
     *          "-400": ["status", "400"],
     *          "-404": ["status", "404"],
     *          "-500": ["status", "500"],
     *          "-504": ["status", "504"],
     *          "0":    ["status", "200"],
     *          "default": "tags.clickhouse.exceptionCode"
     *        }
     *      }
     *    }
     *  }
     */
    #openTraceSearchPage(chartDescriptor, startTime, endTime, row) {
        // const startTime = moment(start).local().format('yyyy-MM-DD HH:mm:ss');
        // const endTime = moment(end).local().format('yyyy-MM-DD HH:mm:ss');

        let url = `/web/trace/search?appName=${g_SelectedApp}&`;

        const instanceFilter = this.vFilter.getSelectedFilter("instanceName");
        if (instanceFilter != null) {
            url += `instanceName=${encodeURI(instanceFilter.matcher.pattern)}&`;
        }
        url += `interval=c:${encodeURI(startTime)}/${encodeURI(endTime)}&`;

        const tracingSpec = chartDescriptor.details.tracing;

        $.each(chartDescriptor.details.groupBy, (index, dimension) => {
            const mappingField = tracingSpec.dimensionMaps[dimension];
            if (mappingField == null) {
                return;
            }

            const val = row[dimension];
            if (typeof mappingField === "string") {
                url += `${mappingField}=${encodeURI(val)}&`;
            } else {
                // the mapping is an object
                const val = row[dimension];
                if (mappingField.type === 'switch') { // currently, only switch is supported
                    let f = mappingField.cases[val];
                    if (f == null) {
                        // case not found, use the default case
                        f = mappingField.cases.default;
                    }
                    const fType = $.type(f);
                    if (fType === 'array') {
                        // f is a pair, f[0] is the field name, f[1] is the value
                        if (f[1] != null && f[1] !== '') {
                            url += `${f[0]}=${encodeURI(f[1])}&`;
                        }
                    } else if (fType === 'string') {
                        url += `${f}=${encodeURI(val)}&`;
                    }
                } else {
                    console.log(`mapping type ${mappingField.type} is not supported yet.`);
                }
            }
        });

        window.open(url);
    }

    createTableComponent(chartId, parentElement, tableDescriptor, insertIndexColumn, buttons) {

        const lookup = tableDescriptor.lookup;
        const tableColumns = tableDescriptor.columns.map((column) => {

            const tableColumn = typeof column === 'object' ? Object.assign({
                    field: column.name,
                    title: column.title || column.name
                }, column)
                : {
                    title: column,
                    field: column
                };

            // handle lookup
            let lookupFn = null;
            if (lookup !== undefined) {
                const fieldLookupTable = lookup[tableColumn.field];
                if (fieldLookupTable !== undefined) {
                    lookupFn = (val) => {
                        const mapped = fieldLookupTable[val];
                        return mapped != null ? mapped + '(' + val + ')' : val;
                    }
                }
            }

            // handle format
            const formatterFn = this._formatters[column.format];
            if (lookupFn != null || formatterFn !== undefined) {
                tableColumn.formatter = (v) => {
                    if (lookupFn != null) {
                        v = lookupFn(v);
                    }
                    if (formatterFn !== undefined) {
                        v = formatterFn(v);
                    }
                    return v;
                };
            }

            // Default to sortable
            if (tableColumn.sortable === undefined) {
                tableColumn.sortable = true;
            }

            return tableColumn;
        });

        if (insertIndexColumn) {
            tableColumns.unshift({
                field: 'id',
                title: 'No.',
                align: 'center',
                width: 20,
                formatter: (cell, row, index) => {
                    return (index + 1);
                }
            });
        }
        const vTable = new TableComponent({
                tableId: chartId + "_table",
                parent: parentElement,
                columns: tableColumns,
                pagination: tableDescriptor.pagination,
                detailView: false,

                buttons: buttons,

                // default order
                order: tableDescriptor.query.orderBy?.order,
                orderBy: tableDescriptor.query.orderBy?.name,
            }
        );
        if (tableDescriptor.title !== undefined) {
            vTable.header('<b>' + tableDescriptor.title + '</b>');
        }

        this._chartComponents[chartId] = vTable;

        return vTable;
    }

    createLineComponent(chartId, chartDescriptor) {
        const chartOption = this.getDefaultChartOption();

        // runtime properties
        chartDescriptor.columnMap = {};

        if (chartDescriptor.yAxis === undefined) {
            chartDescriptor.yAxis = [];
        }
        for (let i = 0, size = chartDescriptor.columns.length; i < size; i++) {
            let column = chartDescriptor.columns[i];

            // string type of column is allowed for simple configuration
            // during rendering, it's turned into object for simple processing
            if (typeof column === 'string') {
                chartDescriptor.columns[i] = {name: column};
                column = chartDescriptor.columns[i];
            }

            // Set up a map
            chartDescriptor.columnMap[column.name] = column;

            // legend
            chartOption.legend.data.push({
                name: column.name,
                icon: 'circle'
            });

            // formatter
            const yAxisIndex = column.yAxis || 0;
            // Make sure the array has enough object for further access
            while (chartDescriptor.yAxis.length < yAxisIndex + 1) {
                chartDescriptor.yAxis.push({});
            }
            const yAxis = chartDescriptor.yAxis[yAxisIndex];
            if (yAxis.format === undefined) {
                yAxis.format = column.format === undefined ? 'compact_number' : column.format;
            }

            if (yAxis.formatter === undefined) {
                yAxis.formatter = this.getFormatter(yAxis.format);
            }
        }

        chartOption.yAxis = chartDescriptor.yAxis.map((yAxis, index) => {
            return {
                type: 'value',
                min: 0 || yAxis.min,
                minInterval: 1 || yAxis.minInterval,
                interval: yAxis.interval,
                inverse: yAxis.inverse === undefined ? false : yAxis.inverse,
                splitLine: {show: true},
                axisLine: {show: false},
                scale: false,
                axisTick: {
                    show: false,
                },
                axisLabel: {
                    formatter: yAxis.formatter
                },
            }
        });

        chartOption.tooltip.formatter = series => {
            const currentChartOption = this.getChartCurrentOption(chartId);

            const start = currentChartOption.timestamp.start;
            const interval = currentChartOption.timestamp.interval;
            const dataIndex = series[0].dataIndex;
            let tooltip = moment(start + dataIndex * interval).local().format('yyyy-MM-DD HH:mm:ss');
            series.forEach(s => {
                //Use the yAxis defined formatter to format the data
                const yAxisIndex = currentChartOption.series[s.seriesIndex].yAxisIndex;

                let formatterFn = currentChartOption.yAxis[yAxisIndex].axisLabel.formatter;

                const text = formatterFn(s.data);

                //Concat the tooltip
                //marker can be seen as the style of legend of this series
                tooltip += `<br />${s.marker}${s.seriesName}: ${text}`;
            });
            return tooltip;
        };
        if (chartOption.yAxis.length === 1) {
            chartOption.grid.right = 15;
        }
        if (chartDescriptor.details != null) {
            chartOption.brush = {
                xAxisIndex: 'all',
                brushLink: 'all',
                outOfBrush: {
                    colorAlpha: 0.1
                }
            };
            chartOption.toolbox = {
                // the toolbox is disabled because the ChartComponent takes over the functionalities
                show: false
            };
        }
        const chartComponent = new ChartComponent({
            containerId: chartId,

            // Keep query object in the chart for further query
            query: chartDescriptor.query
        }).header('<b>' + chartDescriptor.title + '</b>')
            .setChartOption(chartOption);

        this._chartComponents[chartId] = chartComponent;

        return chartComponent;
    }

    refreshTable(query, tableComponent, interval) {
        const filters = this.vFilter.getSelectedFilters();
        if (query.filters !== undefined) {
            $.each(query.filters, (index, filter) => {
                filters.push(filter);
            });
        }

        const thisQuery = Object.assign({}, query);
        thisQuery.interval = {
            startISO8601: interval.start,
            endISO8601: interval.end
        };
        thisQuery.filters = filters;

        let path;
        if (query.type === 'list') {
            path = '/api/datasource/list/v2'
        } else {
            path = '/api/datasource/groupBy/v2';
        }
        const loadOptions = {
            url: apiHost + path,
            ajaxData: thisQuery,
            responseHandler: (res) => {
                return {
                    total: res.total,
                    rows: res.data
                }
            }
        };
        tableComponent.load(loadOptions);
    }

    // PRIVATE
    createChartComponent(chartId, chartDescriptor) {
        if (chartDescriptor.type === 'table') {
            return this.createTableComponent(chartId, $('#' + chartId), chartDescriptor);
        }

        // Process legacy list type
        if (chartDescriptor.type === 'list') {
            // Convert old format to new format
            const query = {
                type: 'list',
                dataSource: chartDescriptor.dataSource,
                filters: chartDescriptor.filters
            };
            query.fields = chartDescriptor.columns.map((column) => {
                if (typeof column === 'object') {
                    return column.field;
                }
                return column;
            });
            chartDescriptor.query = query;
            chartDescriptor.pagination = [25, 50, 100];
            chartDescriptor.type = 'table';

            return this.createTableComponent(chartId, $('#' + chartId), chartDescriptor);
        }
        if (chartDescriptor.type === 'line') {
            return this.createLineComponent(chartId, chartDescriptor);
        }

        //
        // Old timeseries chart format, convert it to the latest
        //
        const query = {
            type: 'timeseries',
            dataSource: chartDescriptor.dataSource,
            fields: []
        };

        const columns = [];
        chartDescriptor.metrics.forEach((metric) => {
            query.fields.push(metric.name);

            const column = {
                name: metric.name,
                format: metric.format
            };
            const yAxisIndex = metric.yAxis || 0;
            if (chartDescriptor.yAxis !== undefined) {
                const yAxis = chartDescriptor.yAxis[yAxisIndex];
                if (yAxis.format !== undefined) {
                    // Don't override the format set above
                    column.format = yAxis.format;
                }
                column.yAxis = yAxisIndex;
            }
            if (metric.chartType !== undefined) {
                column.chartType = metric.chartType;
            }
            if (metric.fill !== undefined) {
                column.fill = metric.fill;
            }
            if (metric.displayName !== undefined) {
                column.title = metric.displayName;
            }
            if (metric.selected !== undefined) {
                column.selected = metric.selected;
            }
            columns.push(column);
        });
        delete chartDescriptor.metrics;

        // predefined filter
        if (chartDescriptor.dimensions !== undefined) {
            query.filters = [];
            $.each(chartDescriptor.dimensions, (name, value) => {
                query.filters.push(value);
            });

            delete chartDescriptor.dimensions;
        }

        if (chartDescriptor.groupBy !== undefined) {
            chartDescriptor.groupBy.forEach((groupBy) => query.fields.unshift(groupBy));
            delete chartDescriptor.groupBy;
        }

        chartDescriptor.columns = columns;
        chartDescriptor.query = query;
        chartDescriptor.type = 'line';
        console.log(JSON.stringify(chartDescriptor, null, 4));
        return this.createLineComponent(chartId, chartDescriptor);
    }

    // PRIVATE
    layout(id, width) {
        if (this._stackLayoutRowFill + width > 12) {
            // create a new row
            this._stackLayoutRowFill = 0;
            this._stackLayoutRow = $('<div style="display: flex"></div>');
            this._container.append(this._stackLayoutRow);
        }
        this._stackLayoutRowFill += width;

        return this._stackLayoutRow.append(`<div class="form-group col-md-${width}" id="${id}" style="margin-bottom: 0;padding-bottom: 10px; padding-left: 1px; padding-right: 1px"></div>`);
    }

    // PUBLIC
    refreshDashboard() {
        if (this._dashboard == null) {
            return;
        }

        // refresh each chart
        const interval = this.getSelectedTimeInterval();
        for (const id in this._chartComponents) {
            const chartComponent = this._chartComponents[id];

            const chartDescriptor = this._chartDescriptors[id];
            if (chartDescriptor !== undefined) {
                // detail view has no root descriptor, do not refresh
                // this can be fixed to allow refresh the detail view if it's shown
                this.refreshChart(chartDescriptor, chartComponent, interval);
            }
        }
    }

    refreshLine(chartDescriptor, chartComponent, interval, metricNamePrefix, mode) {
        if (metricNamePrefix === undefined) {
            metricNamePrefix = '';
        }

        let filters = this.vFilter.getSelectedFilters();
        if (chartDescriptor.query.filters !== undefined) {
            $.each(chartDescriptor.query.filters, (index, filter) => {
                filters.push(filter);
            });
        }

        const thisQuery = Object.assign({}, chartDescriptor.query);
        if (thisQuery.dataSource === undefined) {
            thisQuery.dataSource = chartDescriptor.dataSource;
        }
        thisQuery.interval = {
            startISO8601: interval.start,
            endISO8601: interval.end
        };
        thisQuery.filters = filters;

        const queryFieldsCount = chartDescriptor.query.fields.length;

        chartComponent.load({
            url: apiHost + "/api/datasource/timeseries/v3",
            ajaxData: JSON.stringify(thisQuery),
            processResult: (data) => {
                const timeLabels = [];
                for (let t = data.startTimestamp; t <= data.endTimestamp; t += data.interval) {
                    timeLabels.push(moment(t).local().format('HH:mm:ss'));
                }

                const series = [];
                $.each(data.data, (index, metric) => {
                    let metricName = metric.tags[metric.tags.length - 1];

                    let column = chartDescriptor.columnMap[metricName];
                    if (column === undefined) {
                        console.warn(`Cant find definition of ${metricName}`);
                        return;
                    }

                    let group = "";
                    for (let i = 0; i < metric.tags.length - 1; i++) {
                        group += metric.tags[i];
                        group += "-";
                    }

                    const chartType = column.chartType || 'line';
                    const isLine = chartType === 'line';
                    const isArea = isLine && (column.fill === undefined ? true : column.fill);

                    const n = metricNamePrefix + group + (column.title === undefined ? column.name : column.title);
                    let s = {
                        id: n,
                        name: n,
                        type: chartType,

                        data: metric.values.map(val => column.transformer(val)),
                        yAxisIndex: column.yAxis || 0,

                        areaStyle: isArea ? {opacity: 0.3} : null,
                        lineStyle: isLine ? {width: 1} : null,
                        itemStyle: isLine ? {opacity: 0} : null,
                        barWidth: 10,

                        // selected is not a property of series
                        // this is used to render default selected state of legend by chart-component
                        selected: column.selected === undefined ? true : column.selected
                    };
                    series.push(s);
                });

                // a groupBy query might return empty data
                if (series.length === 0) {
                    const count = (data.endTimestamp - data.startTimestamp) / data.interval;
                    series.push({
                        id: 'empty',
                        name: 'empty',
                        type: 'line',
                        data: new Array(count).fill(0),
                        yAxisIndex: 0,
                        areaStyle: {opacity: 0.3},
                        lineStyle: {width: 1},
                        itemStyle: {opacity: 0},
                        selected: true
                    });
                }

                if (mode === undefined) {
                    if (data.data.length === 0) {
                        // no data returned
                        mode = 'replace';
                    } else {
                        // If the returned count of series is less than the given fields count, that's a group-by.
                        // In this case, we always replace the series because one group may not exist in a following query.
                        mode = data.data.length < queryFieldsCount ? 'replace' : 'refresh'
                    }
                }

                return {
                    refreshMode: mode,

                    // save the timestamp for further processing
                    timestamp: {
                        start: data.startTimestamp,
                        interval: data.interval
                    },
                    xAxis: {
                        data: timeLabels
                    },
                    series: series
                }
            }
        });
    }

    refreshChart(chartDescriptor, chartComponent, interval, metricNamePrefix, mode) {
        if (chartDescriptor.type === 'table') {
            this.refreshTable(chartDescriptor.query, chartComponent, interval);
            return;
        }
        if (chartDescriptor.type === 'line') {
            this.refreshLine(chartDescriptor, chartComponent, interval, metricNamePrefix, mode);
            return;
        }

        console.log('Unknown chart type: ' + chartDescriptor.type);
    }

    // Unit conversion
    // PRIVATE
    createTransformers(schema) {
        $.each(this._dashboard.charts, (index, chartDescriptor) => {
            if (chartDescriptor.dataSource === schema.name) {
                // create transformers for those charts associated with this datasource
                $.each(chartDescriptor.columns, (columnIndex, column) => {
                    column.transformer = this.createTransformer(schema, chartDescriptor, column);
                });
            }
        });
    }

    createTransformer(schema, chartDescriptor, column) {
        if (chartDescriptor.yAxis != null) {
            // get yAxis config for this metric
            const name = column.name;
            const yIndex = column.yAxis || 0;
            if (yIndex < chartDescriptor.yAxis.length) {
                const yAxis = chartDescriptor.yAxis[yIndex];
                const metricSpec = schema.metricsSpec[name];
                if (metricSpec != null && yAxis.format === 'millisecond' && metricSpec.unit === 'nanosecond') {
                    return (val) => val == null ? 0 : (val / 1000 / 1000);
                }
            }
        }
        return (val) => val === null || val === 'NaN' ? 0 : val;
    }

    // PRIVATE
    getChartCurrentOption(id) {
        return this._chartComponents[id].getChartOption();
    }

    // PRIVATE
    getDefaultChartOption() {
        return {
            title: {
                text: '',
                subtext: '',
                show: false,
                textStyle: {
                    fontSize: 13,
                    fontWeight: 'normal',
                },
            },
            tooltip: {
                trigger: 'axis',
                axisPointer: {
                    type: 'line',
                    label: {
                        backgroundColor: '#283b56',
                    },
                },
            },
            axisPointer: {
                link: [
                    {
                        xAxisIndex: 'all'
                    }
                ],
                label: {
                    backgroundColor: '#777'
                }
            },
            legend: {
                type: 'scroll',
                top: 0,
                data: [],
            },
            dataZoom: {
                show: false,
                start: 0,
                end: 100,
            },
            grid: {
                left: 60,
                right: 60,
                bottom: 20,
                top: 40,
            },
            xAxis: {
                type: 'category',
                boundaryGap: false,
                axisLabel: {},
                data: [],
            },
            yAxis: [],
            series: [],
        };
    }

    //PRIVATE
    getFormatter(format) {
        return this._formatters[format];
    }

    resize() {
        for (const id in this._chartComponents) {
            this._chartComponents[id].resize();
        }
    }

    //PRIVATE
    openChart(chartId) {
        const chartDescriptor = this._chartDescriptors[chartId];

        const dialogContent =
            '<ul class="nav nav-tabs">' +
            '  <li class="nav-item">' +
            '    <a class="nav-link active" data-toggle="tab" href="#nav-current" role="tab" aria-controls="nav-current" aria-selected="true">Latest</a>' +
            '  </li>' +
            '  <li class="nav-item">' +
            '    <a class="nav-link" data-toggle="tab" href="#nav-compare" role="tab" aria-controls="nav-compare" aria-selected="true">Comparison</a>' +
            '  </li>' +
            '</ul>' +
            '<div class="tab-content">' +
            '   <div class="tab-pane fade show active" id="nav-current" role="tabpanel" aria-labelledby="nav-current-tab">' +
            '       <div class="btn-group btn-group-sm" role="group" aria-label="..." style="padding-top:5px">' +
            '           <button class="btn btn-popup-latest" style="border-color: #ced4da" data-value="1">1h</button>' +
            '           <button class="btn btn-popup-latest" style="border-color: #ced4da" data-value="3">3h</button>' +
            '           <button class="btn btn-popup-latest" style="border-color: #ced4da" data-value="6">6h</button>' +
            '           <button class="btn btn-popup-latest" style="border-color: #ced4da" data-value="12">12h</button>' +
            '           <button class="btn btn-popup-latest" style="border-color: #ced4da" data-value="24">24h</button>' +
            '       </div>' +
            '       <div id="latest_charts" style="padding-top:5px;height:470px;width:100%"></div>' +
            '   </div>' +
            '   <div class="tab-pane fade" id="nav-compare" role="tabpanel" aria-labelledby="nav-compare-tab">' +
            '       <div class="btn-group btn-group-sm" id="btn-remove-buttons" role="group" aria-label="..." style="padding-top:5px">' +
            '       </div>' +
            '       <div class="btn-group btn-group-sm dropright" role="group" aria-label="..." style="padding-top:5px">' +
            '           <button class="btn btn-compare-remove-add dropdown-toggle" style="border-color: #ced4da" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">Adds</button>' +
            '           <div class="dropdown-menu" style="min-width: 0">      ' +
            '               <a class="dropdown-item btn-compare-add" href="#" data-value="0">today</a>' +
            '               <a class="dropdown-item btn-compare-add" href="#" data-value="1">-1d</a>' +
            '               <a class="dropdown-item btn-compare-add" href="#" data-value="3">-3d</a>     ' +
            '               <a class="dropdown-item btn-compare-add" href="#" data-value="7">-7d</a>     ' +
            '           </div>' +
            '       </div>' +
            '       <div id="compare_charts" style="padding-top:5px;height:470px;width:100%"></div>' +
            '   </div>' +

            '</div>';

        bootbox.dialog({
            centerVertical: true,
            size: 'xl',
            onEscape: true,
            backdrop: true,
            message: dialogContent,
            onShown: () => {
                const latestCharts = this.createChartComponent('latest_charts', chartDescriptor).height('400px');
                const compareChart = this.createChartComponent('compare_charts', chartDescriptor).height('400px');

                $('a[data-toggle="tab"]').on('shown.bs.tab', () => {
                    latestCharts.resize();
                    compareChart.resize();
                })

                const latestButtons = $('.btn-popup-latest');
                latestButtons.click((e) => {
                    const target = $(e.target);
                    if (target.hasClass('btn-primary'))
                        return;

                    $('.btn-popup-latest').removeClass('btn-primary');
                    target.addClass('btn-primary');

                    const hour = parseInt(target.attr('data-value'));
                    this.refreshChart(chartDescriptor, latestCharts, this.getLatestInterval(hour, 'hour'));
                });

                // Add line
                $('.btn-compare-add').click((e) => {
                    const day = $(e.target).attr('data-value');
                    const removeButtonId = 'btn-popup-compare-' + day;
                    if ($('#btn-remove-buttons').find('#' + removeButtonId).length > 0) {
                        return;
                    }

                    // remove line
                    const text = $(e.target).text();
                    const removeButton = $(`<button id="${removeButtonId}" class="btn btn-compare-remove" style="border-color: #ced4da" data-value="${day}" data-label="today">${text}&nbsp;&nbsp;<span aria-hidden="true">×</span></button>`)
                        .click((e) => {
                            // remove button from UI
                            let button = e.target;
                            if (e.target.nodeName === 'SPAN')
                                button = button.parentElement;
                            $(button).remove();

                            // remove lines from chart
                            compareChart.clearLines(text + '-');
                        });
                    $('#btn-remove-buttons').append(removeButton);

                    const todayStart = moment().startOf('day');
                    const baseStart = todayStart.clone().subtract(day, 'day');
                    const baseEnd = baseStart.clone().add(1, 'day');

                    this.refreshChart(chartDescriptor,
                        compareChart,
                        {
                            start: baseStart.toISOString(true),
                            end: baseEnd.toISOString(true)
                        },
                        text + '-',
                        'add');
                });

                latestButtons[0].click();
            },
            onHidden: () => {
                this._chartComponents['latest_charts'].dispose();
                this._chartComponents['compare_charts'].dispose();

                delete this._chartComponents['latest_charts'];
                delete this._chartComponents['compare_charts'];
            }
        });
    }

    getSelectedTimeInterval() {
        return this._selectedInterval;
    }

    //PRIVATE
    /**
     * "year" | "years" | "y" |
     * "month" | "months" | "M" |
     * "week" | "weeks" | "w" |
     * "day" | "days" | "d" |
     * "hour" | "hours" | "h" |
     * "minute" | "minutes" | "m" |
     * "second" | "seconds" | "s" |
     * millisecond" | "milliseconds" | "ms"
     *  @param value
     * @param unit
     * @returns {{start: string, end: string}}
     */
    getLatestInterval(value, unit) {
        return {
            start: moment().subtract(value, unit).toISOString(true),
            end: moment().toISOString(true)
        }
    }
}
