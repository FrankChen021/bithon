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
        this._formatters['nanoFormatter'] = (v) => nanoFormat(v, 2);
        this._formatters['millisecond'] = (v) => milliFormat(v, 2);
        this._formatters['microsecond'] = (v) => microFormat(v, 2);
        this._formatters['byte_rate'] = (v) => v.formatBinaryByte() + "/s";
        this._formatters['dateTime'] = (v) => new Date(v).format('yyyy-MM-dd hh:mm:ss');
        this._formatters['shortDateTime'] = (v) => new Date(v).format('MM-dd hh:mm:ss');
        this._formatters['timeDuration'] = (v) => v.formatTimeDuration();
        this._formatters['timeDiff'] = (v) => v.formatTimeDiff();
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

            // turn into metrics object into map
            chartDescriptor.metricMap = {};
            $.each(chartDescriptor.metrics, (index, metricDef) => {
                chartDescriptor.metricMap[metricDef.name] = metricDef;
            });

            // set up a data source to charts mapping
            const dataSourceName = chartDescriptor.dataSource;
            if (dataSource2Charts[dataSourceName] == null) {
                dataSource2Charts[dataSourceName] = [];
            }
            dataSource2Charts[dataSourceName].push(chartId);
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
        //
        for (const dataSourceName in dataSource2Charts) {
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
                        this.#initChartDetail(chartDescriptor);
                    });
                },
                errorCallback: (error) => {
                }
            });
        }
    }

    #initChartDetail(chartDescriptor) {
        // create detail view for this chart
        if (chartDescriptor.details == null) {
            return;
        }

        const chartComponent = this._chartComponents[chartDescriptor.id];

        let fields = [];
        let columns = [
            {
                field: 'id',
                title: 'No.',
                align: 'center',
                width: 20,
                formatter: (cell, row, index, field) => {
                    return (index + 1);
                }
            }
        ];

        const detailViewId = chartDescriptor.id + '_detailView';
        if (chartDescriptor.details.columns !== undefined) {
            const r = this.#createDetailViewColumns(detailViewId, chartDescriptor);
            fields = r[0];
            columns = columns.concat(r[1]);
        } else {
            const r = this.#createColumnsLagecy(chartDescriptor);
            fields = r[0];
            columns = columns.concat(r[1]);
        }

        const pageable = chartDescriptor.details.groupBy === undefined || chartDescriptor.details.groupBy.length === 0;
        const detailView = this.#createDetailView(detailViewId,
            chartComponent.getUIContainer(),
            columns,
            [{
                title: "Tracing Log",
                text: "Search...",
                visible: chartDescriptor.details.tracing !== undefined,
                onClick: (index, row, start, end) => this.#openTraceSearchPage(chartDescriptor, start, end, row)
            }],
            pageable,
            chartDescriptor.details.orderBy);
        chartComponent.setSelectionHandler(
            (option, start, end) => {
                this.#refreshDetailView(chartDescriptor, detailView, fields, option, start, end);
            },
            () => {
                detailView.clear();
                detailView.show();
            },
            () => {
                detailView.hide();
            },
            () => {
                detailView.clear();
            });
    }

    #createDetailViewColumns(detailViewId, chartDescriptor) {
        const tableFields = [];
        const tableColumns = []

        $.each(chartDescriptor.details.columns, (index, column) => {

            let columnName;
            let sortable = true;
            if (typeof column === 'object') {
                columnName = column.name;
                sortable = column.sortable || true;
            } else {
                columnName = column;
            }

            const tableColumn = {
                field: columnName,
                title: columnName,
                align: 'center',
                sortable: sortable
            };

            // set up lookup for this column
            let lookupFn = (v) => v;
            if (chartDescriptor.details.lookup !== undefined) {
                const lookupTable = chartDescriptor.details.lookup[name];
                if (lookupTable != null) {
                    lookupFn = (val) => {
                        const v = lookupTable[val];
                        if (v != null) {
                            return v + '(' + val + ')';
                        } else {
                            return val;
                        }
                    };
                }
            }

            let formatterFn = (v) => v;
            if (typeof column === 'object' && column.formatter !== undefined) {
                if (column.formatter === 'template') {
                    // a template based formatter
                    formatterFn = (v) => {
                        return column.template.replaceAll('{value}', v);
                    }
                } else {
                    // a built-in formatter
                    formatterFn = this._formatters[column.formatter];
                    if (formatterFn == null) {
                        console.log(`formatter ${column.formatter} is not a pre-defined one.`);
                    }
                }
            }

            if (typeof column === 'object' && column.view !== undefined) {
                tableColumn.format = column.view;
            }

            tableColumn.formatter = (val) => formatterFn(lookupFn(val));

            tableFields.push(columnName);
            tableColumns.push(tableColumn);
        });

        return [tableFields, tableColumns];
    }

    // create columns from groupBy/metrics/
    #createColumnsLagecy(chartDescriptor) {
        const fields = [];
        const columns = []

        $.each(chartDescriptor.details.dimensions, (index, dimension) => {
            fields.push(dimension.name);

            const column = {
                field: dimension.name,
                title: dimension.name,
                align: 'center',
                sortable: dimension.sortable || false
            };

            // set up lookup for this dimension
            let lookupFn = (v) => v;
            if (chartDescriptor.details.lookup !== undefined) {
                const dimensionLookup = chartDescriptor.details.lookup[dimension];
                if (dimensionLookup != null) {
                    lookupFn = (val) => {
                        const v = dimensionLookup[val];
                        if (v != null) {
                            return v + '(' + val + ')';
                        } else {
                            return val;
                        }
                    };
                }
            }

            // there's a user defined formatter
            let formatter = null;
            if (dimension.formatter !== undefined) {
                if (dimension.formatter === 'template') {
                    // a template based formatter
                    formatter = (v) => {
                        return dimension.template.replaceAll('{value}', lookupFn(v));
                    }
                } else {
                    // a built-in formatter
                    formatter = this._formatters[dimension.formatter];
                    if (formatter == null) {
                        console.log(`formatter ${dimension.formatter} is not a pre-defined one.`);
                    }
                }
            }

            // set up a default one
            if (formatter == null) {
                formatter = lookupFn;
            }
            column.formatter = formatter;
            columns.push(column);
        });

        //
        // create columns for grouped dimensions
        // TODO: combine with dimensions
        //
        $.each(chartDescriptor.details.groupBy, (index, dimension) => {
            let name;
            if (typeof dimension === 'object') {
                name = dimension.name;
            } else {
                name = dimension;
            }

            const column = {
                field: name,
                title: name,
                align: 'center',
                sortable: true,
                formatter: (v) => typeof v === 'string' ? v.htmlEncode() : v
            };

            // set up lookup for this dimension
            if (chartDescriptor.details.lookup !== undefined) {
                const dimensionLookup = chartDescriptor.details.lookup[name];
                if (dimensionLookup != null) {
                    column.formatter = (val) => {
                        const v = dimensionLookup[val];
                        if (v != null) {
                            return v + '(' + val + ')';
                        } else {
                            return val;
                        }
                    };
                }
            }

            columns.push(column);
        });

        $.each(chartDescriptor.details.aggregators, (index, aggregator) => {

            const column = {
                field: aggregator.name,
                title: aggregator.name,
                align: 'center',
                sortable: true,
                formatter: (v) => typeof v === 'string' ? v.htmlEncode() : v
            };

            if (aggregator.formatter !== undefined) {
                const formatter = this._formatters[aggregator.formatter];
                if (formatter != null) {
                    column.formatter = (v) => formatter(v);
                }
            }
            columns.push(column);
        });

        //
        // create columns for metrics
        //
        $.each(chartDescriptor.details.metrics, (index, metric) => {
            let metricName;
            if (typeof metric === 'object') {
                metricName = metric.name;
            } else {
                metricName = metric;
            }

            fields.push(metricName);

            // set up transformer and formatter for this metric
            const column = {field: metricName, title: metricName, align: 'center', sortable: true};

            // use the formatter of yAxis to format this metric
            let formatterFn = null;
            const metricDef = chartDescriptor.metricMap[metricName];
            if (metricDef != null && chartDescriptor.yAxis !== undefined) {
                const yAxis = metricDef.yAxis == null ? 0 : metricDef.yAxis;
                formatterFn = this._formatters[chartDescriptor.yAxis[yAxis].format];
            }
            if (formatterFn == null) {
                if (typeof metric === 'object' && metric.formatter !== undefined) {
                    // see if the descriptor provides formatter
                    formatterFn = this._formatters[metric.formatter];
                }
            }
            if (formatterFn == null) {
                // if this metric is not found, format in default ways
                formatterFn = (v) => {
                    return v === undefined ? "undefined" : v.formatCompactNumber();
                };
            }

            let transformerFn = metricDef == null ? null : metricDef.transformer;
            column.formatter = (val) => {
                const t = transformerFn == null ? val : transformerFn(val);
                return formatterFn == null ? t : formatterFn(t);
            };

            columns.push(column);
        });

        return [fields, columns];
    }

    #createDetailView(id, parent, columns, buttons, pageable, orderBy) {
        return new TableComponent({
            tableId: id,
            parent: parent,
            columns: columns,
            buttons: buttons,
            pagination: pageable,
            order: orderBy == null ? null : orderBy.order,
            orderBy: orderBy == null ? null : orderBy.name
        });
    }

    #refreshDetailView(chartDescriptor, detailView, fields, chartOption, startIndex, endIndex) {
        // get the time range
        const start = chartOption.timestamp.start;
        const interval = chartOption.timestamp.interval;

        const startTimestamp = start + startIndex * interval;
        const endTimestamp = start + endIndex * interval;

        const startISO8601 = moment(startTimestamp).utc().toISOString();
        const endISO8601 = moment(endTimestamp).utc().toISOString();

        const filters = this.vFilter.getSelectedFilters();
        if (chartDescriptor.filters != null) {
            $.each(chartDescriptor.filters, (index, filter) => {
                filters.push(filter);
            });
        }
        if (chartDescriptor.details.filters != null) {
            $.each(chartDescriptor.details.filters, (index, filter) => {
                filters.push(filter);
            });
        }

        let loadOptions;
        if (chartDescriptor.details.groupBy !== undefined && chartDescriptor.details.groupBy.length > 0) {
            loadOptions = {
                url: apiHost + "/api/datasource/groupBy",
                start: startTimestamp,
                end: endTimestamp,
                ajaxData: {
                    dataSource: chartDescriptor.dataSource,
                    startTimeISO8601: startISO8601,
                    endTimeISO8601: endISO8601,
                    filters: filters,
                    metrics: chartDescriptor.details.metrics !== undefined ? chartDescriptor.details.metrics.map(m => typeof m === 'object' ? m.name : m) : [],
                    groupBy: chartDescriptor.details.groupBy,
                    aggregators: chartDescriptor.details.aggregators
                }
            };
        } else {
            // list option
            loadOptions = {
                url: apiHost + "/api/datasource/list",
                ajaxData: {
                    dataSource: chartDescriptor.dataSource,
                    startTimeISO8601: startISO8601,
                    endTimeISO8601: endISO8601,
                    filters: filters,
                    columns: fields,
                    pageSize: 10,
                    pageNumber: 0
                }
            };
        }

        detailView.load(loadOptions);
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
    #openTraceSearchPage(chartDescriptor, start, end, row) {
        const startTime = moment(start).local().format('yyyy-MM-DD HH:mm:ss');
        const endTime = moment(end).local().format('yyyy-MM-DD HH:mm:ss');

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

    createListComponent(chartId, chartDescriptor) {

        const lookup = chartDescriptor.lookup;
        $.each(chartDescriptor.columns, (index, column) => {
            // handle lookup
            let lookupFn = null;
            if (lookup !== undefined && lookup !== null) {
                const fieldLookupTable = lookup[column.field];
                if (fieldLookupTable != null) {
                    lookupFn = (val) => {
                        const mapped = fieldLookupTable[val];
                        return mapped != null ? mapped + '(' + val + ')' : val;
                    }
                }
            }

            // handle format
            const formatterFn = this._formatters[column.format];
            if (lookupFn != null || formatterFn != null) {
                column.formatter = (v) => {
                    if (lookupFn != null) {
                        v = lookupFn(v);
                    }
                    if (formatterFn != null) {
                        v = formatterFn(v);
                    }
                    return v;
                };
            }
        });

        const vParent = $('#' + chartId);

        const vTable = new TableComponent({
                tableId: chartId + '_table',
                parent: vParent,
                columns: chartDescriptor.columns,
                pagination: true,
                detailView: false,

                // default order
                order: 'desc',
                orderBy: 'timestamp',
            }
        );

        this._chartComponents[chartId] = vTable;

        return vTable;
    }

    createTableComponent(chartId, chartDescriptor) {

        const lookup = chartDescriptor.lookup;
        const columns = chartDescriptor.query.columns.map((column) => {
            if (typeof column !== 'object') {
                return {
                    title: column,
                    field: column
                };
            }

            const tableColumn = {
                field: column.name,
                title: column.title || column.name,
            };

            // handle lookup
            let lookupFn = null;
            if (lookup !== undefined && lookup !== null) {
                const fieldLookupTable = lookup[column.name];
                if (fieldLookupTable != null) {
                    lookupFn = (val) => {
                        const mapped = fieldLookupTable[val];
                        return mapped != null ? mapped + '(' + val + ')' : val;
                    }
                }
            }

            // handle format
            if (column.format !== undefined) {
                const formatterFn = this._formatters[column.format];
                if (lookupFn != null || formatterFn != null) {
                    tableColumn.formatter = (v) => {
                        if (lookupFn != null) {
                            v = lookupFn(v);
                        }
                        if (formatterFn != null) {
                            v = formatterFn(v);
                        }
                        return v;
                    };
                }
            }
            return tableColumn;
        });

        const vParent = $('#' + chartId);

        const vTable = new TableComponent({
                tableId: chartId + '_table',
                parent: vParent,
                columns: columns,
                pagination: false,
                detailView: false,

                // default order
                order: chartDescriptor.query.orderBy?.order,
                orderBy: chartDescriptor.query.orderBy?.name,
            }
        );

        if (chartDescriptor.title !== undefined) {
            vTable.header('<b>' + chartDescriptor.title + '</b>');
        }
        this._chartComponents[chartId] = vTable;

        return vTable;
    }

    refreshTable(chartDescriptor, tableComponent, interval) {
        const filters = this.vFilter.getSelectedFilters();
        if (chartDescriptor.filters != null) {
            $.each(chartDescriptor.filters, (index, filter) => {
                filters.push(filter);
            });
        }

        const loadOptions = {
            url: apiHost + "/api/datasource/groupBy/v2",
            ajaxData: {
                dataSource: chartDescriptor.dataSource,
                interval: {
                    startISO8601: interval.start,
                    endISO8601: interval.end
                },
                filters: filters,
                columns: chartDescriptor.query.columns,
                orderBy: chartDescriptor.query.orderBy,
                limit: chartDescriptor.query.limit === undefined ? null : chartDescriptor.query.limit
            }
        };
        tableComponent.load(loadOptions);
    }

    refreshList(chartDescriptor, tableComponent, interval) {
        const filters = this.vFilter.getSelectedFilters();
        if (chartDescriptor.filters != null) {
            $.each(chartDescriptor.filters, (index, filter) => {
                filters.push(filter);
            });
        }

        const loadOptions = {
            url: apiHost + "/api/datasource/list",
            ajaxData: {
                dataSource: chartDescriptor.dataSource,
                startTimeISO8601: interval.start,
                endTimeISO8601: interval.end,
                filters: filters,
                columns: chartDescriptor.columns.map(column => column.field),
                pageSize: 10,
                pageNumber: 0
            }
        };
        tableComponent.load(loadOptions);
    }

    // PRIVATE
    createChartComponent(chartId, chartDescriptor) {
        if (chartDescriptor.type === 'list') {
            return this.createListComponent(chartId, chartDescriptor);
        }
        if (chartDescriptor.type === 'table') {
            return this.createTableComponent(chartId, chartDescriptor);
        }

        const chartOption = this.getDefaultChartOption();
        chartOption.legend.data = chartDescriptor.metrics.map(metric => {
            return {
                name: metric.name,
                icon: 'circle'
            }
        });

        let yAxisFormatters = null;
        if (chartDescriptor.yAxis != null) {
            yAxisFormatters = chartDescriptor.yAxis.map(y => this.getFormatter(y.format));
            for (let i = 1; i < yAxisFormatters.length; i++) {
                if (yAxisFormatters[i] == null)
                    yAxisFormatters[i] = yAxisFormatters[i - 1]; //default to prev config
            }
        }
        chartOption.tooltip.formatter = series => {
            const currentChartOption = this.getChartCurrentOption(chartId);

            const start = currentChartOption.timestamp.start;
            const interval = currentChartOption.timestamp.interval;
            const dataIndex = series[0].dataIndex;
            const timeText = moment(start + dataIndex * interval).local().format('yyyy-MM-DD HH:mm:ss');

            let tooltip = timeText;
            series.forEach(s => {
                //Use the yAxis defined formatter to format the data
                const yAxisIndex = currentChartOption.series[s.seriesIndex].yAxisIndex;

                let formatterFn = null;
                if (yAxisFormatters != null) {
                    // use yAxis formatter, maybe null
                    formatterFn = yAxisFormatters[yAxisIndex];
                }
                if (formatterFn == null) {
                    formatterFn = this._formatters['compact_number'];
                }

                const text = formatterFn(s.data);

                //Concat the tooltip
                //marker can be seen as the style of legend of this series
                tooltip += `<br />${s.marker}${s.seriesName}: ${text}`;
            });
            return tooltip;
        };
        if (chartDescriptor.yAxis != null) {
            $.each(chartDescriptor.yAxis, (index, y) => {
                chartOption.yAxis.push({
                    type: 'value',
                    min: 0 || y.min,
                    minInterval: 1 || y.minInterval,
                    interval: y.interval,
                    inverse: y.inverse === undefined ? false : y.inverse,
                    splitLine: {show: true},
                    axisLine: {show: false},
                    scale: false,
                    axisTick: {
                        show: false,
                    },
                    axisLabel: {
                        formatter: yAxisFormatters[index]
                    },
                });
            });
        } else {
            chartOption.yAxis = [{
                type: 'value',
                min: 0,
                minInterval: 1,
                scale: true,
                splitLine: {show: true},
                axisLine: {show: false},
                axisTick: {
                    show: false,
                },
                axisLabel: {},
            }];
        }
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
            metrics: chartDescriptor.metrics.filter(metric => metric.aggregator === undefined).map(metric => metric.name),
            aggregators: chartDescriptor.metrics.filter(metric => metric.aggregator !== undefined).map(metric => {
                return {
                    name: metric.name,
                    type: metric.aggregator
                };
            }),
        }).header('<b>' + chartDescriptor.title + '</b>')
            .setChartOption(chartOption);

        this._chartComponents[chartId] = chartComponent;

        return chartComponent;
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
            this.refreshChart(this._chartDescriptors[id], this._chartComponents[id], interval);
        }
    }

    refreshChart(chartDescriptor, chartComponent, interval, metricNamePrefix, mode) {
        // const appFilter = this.vFilter.getSelectedFilter('appName');
        // if (appFilter === undefined || appFilter === null) {
        //     return;
        // }

        if (chartDescriptor.type === 'list') {
            this.refreshList(chartDescriptor, chartComponent, interval);
            return;
        }
        if (chartDescriptor.type === 'table') {
            this.refreshTable(chartDescriptor, chartComponent, interval);
            return;
        }

        if (metricNamePrefix == null) {
            metricNamePrefix = '';
        }

        if (mode === undefined) {
            mode = 'refresh';
        }

        let dimensions = this.vFilter.getSelectedFilters();
        if (chartDescriptor.dimensions !== undefined) {
            $.each(chartDescriptor.dimensions, (index, filter) => {
                dimensions.push(filter);
            });
        }

        chartComponent.load({
            url: apiHost + "/api/datasource/timeseries/v2",
            ajaxData: JSON.stringify({
                dataSource: chartDescriptor.dataSource,
                interval: {
                    startISO8601: interval.start,
                    endISO8601: interval.end
                },
                filters: dimensions,
                groupBy: chartDescriptor.groupBy === undefined ? [] : chartDescriptor.groupBy,
                metrics: chartComponent.getOption().metrics,
                aggregators: chartComponent.getOption().aggregators
            }),
            processResult: (data) => {
                const timeLabels = [];
                for (let t = data.startTimestamp; t <= data.endTimestamp; t += data.interval) {
                    timeLabels.push(moment(t).local().format('HH:mm:ss'));
                }
                const series = [];
                $.each(data.metrics, (index, metric) => {
                    let metricName = metric.tags[metric.tags.length - 1];
                    let metricDef = chartDescriptor.metricMap[metricName];
                    if (metricDef === undefined) {
                        return;
                    }

                    let group = "";
                    for (let i = 0; i < metric.tags.length - 1; i++) {
                        group += metric.tags[i];
                        group += "-";
                    }

                    const chartType = metricDef.chartType || 'line';
                    const isLine = chartType === 'line';
                    const isArea = isLine && (metricDef.fill === undefined ? true : metricDef.fill);

                    const n = metricNamePrefix + group + (metricDef.displayName === undefined ? metricDef.name : metricDef.displayName);
                    let s = {
                        id: n,
                        name: n,
                        type: chartType,

                        data: metric.values.map(val => metricDef.transformer(val)),
                        yAxisIndex: metricDef.yAxis == null ? 0 : metricDef.yAxis,

                        areaStyle: isArea ? {opacity: 0.3} : null,
                        lineStyle: isLine ? {width: 1} : null,
                        itemStyle: isLine ? {opacity: 0} : null,
                        barWidth: 10,

                        // selected is not a property of series
                        // this is used to render default selected state of legend by chart-component
                        selected: metricDef.selected === undefined ? true : metricDef.selected
                    };
                    series.push(s);
                });

                // a groupBy query might return empty data
                if (series.length === 0) {
                    series.push({
                        id: 'empty',
                        name: 'empty',
                        type: 'line',
                        data: new Array(data.count).fill(0),
                        yAxisIndex: 0,
                        areaStyle: {opacity: 0.3},
                        lineStyle: {width: 1},
                        itemStyle: {opacity: 0},
                        selected: true
                    });
                }

                return {
                    // for a groupBy query, always replace the series because one group may not exist in a following query
                    refreshMode: chartDescriptor.groupBy !== undefined ? 'replace' : mode,

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

    // Unit conversion
    // PRIVATE
    createTransformers(schema) {
        $.each(this._dashboard.charts, (index, chartDescriptor) => {
            if (chartDescriptor.dataSource === schema.name) {
                // create transformers for those charts associated with this datasource
                $.each(chartDescriptor.metrics, (metricIndex, metric) => {
                    metric.transformer = this.createTransformer(schema, chartDescriptor, metricIndex);
                });
            }
        });
    }

    createTransformer(schema, chartDescriptor, metricIndex) {
        if (chartDescriptor.yAxis != null) {
            // get yAxis config for this metric
            const metricDescriptor = chartDescriptor.metrics[metricIndex];
            const metricName = metricDescriptor.name;
            const yIndex = metricDescriptor.yAxis == null ? 0 : metricDescriptor.yAxis;
            if (yIndex < chartDescriptor.yAxis.length) {
                const yAxis = chartDescriptor.yAxis[yIndex];
                const metricSpec = schema.metricsSpec[metricName];
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

                $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
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
