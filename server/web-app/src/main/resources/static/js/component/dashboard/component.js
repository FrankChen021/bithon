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
        this._formatters['percentage'] = (v) => v + '%';
        this._formatters['nanoFormatter'] = (v) => nanoFormat(v, 2);
        this._formatters['millisecond'] = (v) => milliFormat(v, 2);
        this._formatters['microsecond'] = (v) => microFormat(v, 2);
        this._formatters['byte_rate'] = (v) => v.formatBinaryByte() + "/s";
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
            appName: this._appName,
            intervalProvider: () => this.getSelectedTimeInterval(),
        }).registerChangedListener((name, value) => {
            if (name === 'application') {
                window.location = `/web/app/metric/${value}/${this._dashboardName}?interval=${g_MetricSelectedInterval}`;
            } else {
                this.refreshDashboard();
            }
        });

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

        const dataSourceFilter = this._dashboard.charts[0].dataSource;

        //
        // Loaded Dimension Filter
        //
        for (const dataSourceName in dataSource2Charts) {
            this._schemaApi.getSchema(
                dataSourceName,
                (schema) => {
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
                (error) => {
                }
            );
        }
    }

    #initChartDetail(chartDescriptor) {
        // create detail view for this chart
        if (chartDescriptor.details == null) {
            return;
        }

        const chartComponent = this._chartComponents[chartDescriptor.id];

        const columns = [
            {
                field: 'id',
                title: 'No',
                align: 'center',
                formatter: (cell, row, index, field) => {
                    return (index + 1);
                }
            }
        ];

        //
        // create columns for dimensions
        //
        $.each(chartDescriptor.details.groupBy, (index, dimension) => {

            const column = {field: dimension, title: dimension, align: 'center', sortable: true};

            // set up lookup for this dimension
            if (chartDescriptor.details.lookup !== undefined) {
                const dimensionLookup = chartDescriptor.details.lookup[dimension];
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

        //
        // create columns for metrics
        //
        $.each(chartDescriptor.details.metrics, (index, metric) => {
            // set up transformer and formatter for this metric
            const column = {field: metric, title: metric, align: 'center', sortable: true};

            // use the formatter of yAxis to format this metric
            let formatterFn = null;
            const metricDef = chartDescriptor.metricMap[metric];
            if (metricDef != null && chartDescriptor.yAxis !== undefined) {
                const yAxis = metricDef.yAxis == null ? 0 : metricDef.yAxis;
                formatterFn = this._formatters[chartDescriptor.yAxis[yAxis].format];
            } else {
                // if this metric is not found, format in default ways
                formatterFn = (v) => {
                    return v.formatCompactNumber();
                };
            }

            let transformerFn = metricDef == null ? null : metricDef.transformer;
            if (transformerFn != null || formatterFn != null) {
                column.formatter = (val) => {
                    const t = transformerFn == null ? val : transformerFn(val);
                    return formatterFn == null ? t : formatterFn(t);
                };
            }

            columns.push(column);
        });

        const detailView = this.#createDetailView(chartDescriptor.id + '_detailView',
            chartComponent.getUIContainer(),
            columns,
            [{
                text: "Trace",
                visible: chartDescriptor.details.tracing !== undefined,
                onClick: (index, row, start, end) => this.#openTraceSearchPage(chartDescriptor, start, end, row)
            }]);
        chartComponent.setSelectionHandler(
            (option, start, end) => {
                this.#refreshDetailView(chartDescriptor, detailView, option, start, end);
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

    #createDetailView(parent, columns, buttons) {
        return new TableComponent({parent: parent, columns: columns, buttons: buttons});
    }

    #refreshDetailView(chartDescriptor, detailView, option, startIndex, endIndex) {
        // get the time range
        const start = option.timestamp.start;
        const interval = option.timestamp.interval;

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

        const loadOptions = {
            url: apiHost + "/api/datasource/groupBy",
            start: startTimestamp,
            end: endTimestamp,
            ajaxData: {
                dataSource: chartDescriptor.dataSource,
                startTimeISO8601: startISO8601,
                endTimeISO8601: endISO8601,
                filters: filters,
                metrics: chartDescriptor.details.metrics,
                groupBy: chartDescriptor.details.groupBy
            }
        };
        detailView.load(loadOptions);
    }

    #openTraceSearchPage(chartDescriptor, start, end, row) {
        const startTimeISO8601 = moment(start).utc().toISOString(true);
        const endIimeISO8601 = moment(end).utc().toISOString(true);

        let url = `/web/trace/search?appName=${this._appName}&`;
        const instanceFilter = this._selectedDimensions["instanceName"];
        if(instanceFilter != null) {
            url += `instanceName=${encodeURI(instanceFilter.matcher.pattern)}&`;
        }
        url += `startTime=${encodeURI(startTimeISO8601)}&endTime=${encodeURI(endIimeISO8601)}&`;

        // "tracing": {
        //     "filters": {
        //         "mapping": {
        //             "uri": "tags.uri",
        //                 "statusCode": "tags.status"
        //         },
        //         "kind": "SERVER"
        //     }
        // }
        const tracingFilters = chartDescriptor.details.tracing.filters;

        $.each(chartDescriptor.details.groupBy, (index, dimension) => {
            const mapTo = tracingFilters.mapping[dimension];
            if (mapTo != null) {
                url += `${mapTo}=${encodeURI(row[dimension])}&`;
            } else {
                url += `${dimension}=${encodeURI(row[dimension])}&`;
            }
        });

        $.each(tracingFilters.extra, (prop, value) => {
            url += `${prop}=${encodeURIComponent(value)}&`;
        });

        window.open(url);
    }

    createTableComponent(chartId, chartDescriptor) {
        const vParent = $('#' + chartId);

        const vTable = new TableComponent({
                tableId: chartId + '_table',
                parent: vParent,
                columns: chartDescriptor.columns,
                pagination: true,
                detailView: false
            }
        );
        // const chartComponent = new ChartComponent({
        //     containerId: chartId,
        //     metrics: chartDescriptor.metrics.map(metric => metric.name),
        // }).header('<b>' + chartDescriptor.title + '</b>')
        //     .setChartOption(chartOption);

        this._chartComponents[chartId] = vTable;

        return vTable;
    }

    refreshTable(chartDescriptor, tableComponent, interval) {
        const filters = this.vFilter.getSelectedFilters();
        if (chartDescriptor.dimensions !== undefined) {
            $.each(chartDescriptor.dimensions, (name, value) => {
                filters.push(value);
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
                order: 'desc',
                orderBy: 'timestamp',
                pageSize: 10,
                pageNumber: 0
            }
        };
        tableComponent.load(loadOptions);
    }

    // PRIVATE
    createChartComponent(chartId, chartDescriptor) {

        const chartOption = this.getDefaultChartOption();
        chartOption.legend.data = chartDescriptor.metrics.map(metric => {
            return {
                name: metric.name,
                icon: 'circle'
            }
        });
        if (chartDescriptor.yAxis != null) {
            const formatterFns = chartDescriptor.yAxis.map(y => this.getFormatter(y.format));
            for (let i = 1; i < formatterFns.length; i++) {
                if (formatterFns[i] == null)
                    formatterFns[i] = formatterFns[i - 1]; //default to prev config
            }
            chartOption.tooltip.formatter = params => {
                const currentChartOption = this.getChartCurrentOption(chartId);
                let result = (params[0] || params).axisValue;
                params.forEach(p => {
                    const yAxisIndex = currentChartOption.series[p.seriesIndex].yAxisIndex;
                    const formatterFn = formatterFns[yAxisIndex];
                    const s = formatterFn != null ? formatterFn(p.data) : p.data.toFixed(2);
                    result += `<br />${p.marker}${p.seriesName}: ${s}`;
                });
                return result;
            };
            $.each(chartDescriptor.yAxis, (index, y) => {
                chartOption.yAxis.push({
                    type: 'value',
                    min: 0 || y.min,
                    minInterval: 1 || y.minInterval,
                    interval: y.interval,
                    scale: false,
                    splitLine: {show: true},
                    axisLine: {show: false},
                    axisTick: {
                        show: false,
                    },
                    axisLabel: {
                        formatter: formatterFns[index]
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
            metrics: chartDescriptor.metrics.map(metric => metric.name),
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
        if (chartDescriptor.type === 'list') {
            this.refreshTable(chartDescriptor, chartComponent, interval);
            return;
        }

        if (mode === undefined) {
            mode = 'refresh';
        }

        if (chartDescriptor.groupBy !== undefined) {
            // in future, the version 2 method should be used for all cases
            this.refreshChart2(chartDescriptor, chartComponent, interval, metricNamePrefix, mode);
            return;
        }

        if (metricNamePrefix == null) {
            metricNamePrefix = '';
        }
        const filters = this.vFilter.getSelectedFilters();
        if (chartDescriptor.dimensions !== undefined) {
            $.each(chartDescriptor.dimensions, (name, value) => {
                filters.push(value);
            });
        }

        chartComponent.load({
            url: apiHost + "/api/datasource/metrics",
            ajaxData: JSON.stringify({
                dataSource: chartDescriptor.dataSource,
                startTimeISO8601: interval.start,
                endTimeISO8601: interval.end,
                filters: filters,
                groups: chartDescriptor.groupBy,
                metrics: chartComponent.getOption().metrics
            }),
            processResult: (data) => {
                const timeLabels = data.map(d => moment(d._timestamp).local().format('HH:mm:ss'));

                const series = chartDescriptor.metrics.map(metric => {
                    return {
                        name: metricNamePrefix + (metric.displayName === undefined ? metric.name : metric.displayName),
                        type: metric.chartType || 'line',

                        data: data.map(d => metric.transformer(d[metric.name])),
                        yAxisIndex: metric.yAxis == null ? 0 : metric.yAxis,

                        areaStyle: {opacity: 0.3},
                        lineStyle: {width: 1},
                        itemStyle: {opacity: 0},

                        // selected is not a property of series
                        // this is used to render default selected state of legend by chart-component
                        selected: metric.selected === undefined ? true : metric.selected
                    }
                });


                return {
                    // save the timestamp for further processing
                    timestamp: {
                        start: data.length === 0 ? 0 : data[0]._timestamp,
                        interval: data.length === 0 ? 0 : data[1]._timestamp - data[0]._timestamp
                    },
                    xAxis: {
                        data: timeLabels
                    },
                    series: series
                }
            }
        });
    }

    refreshChart2(chartDescriptor, chartComponent, interval, metricNamePrefix, mode) {
        if (metricNamePrefix == null) {
            metricNamePrefix = '';
        }

        let dimensions = this.vFilter.getSelectedFilters();
        if (chartDescriptor.dimensions !== undefined) {
            $.each(chartDescriptor.dimensions, (index, filter) => {
                dimensions.push(filter);
            });
        }

        chartComponent.load({
            url: apiHost + "/api/datasource/timeseries",
            ajaxData: JSON.stringify({
                dataSource: chartDescriptor.dataSource,
                startTimeISO8601: interval.start,
                endTimeISO8601: interval.end,
                dimensions: dimensions,
                groups: chartDescriptor.groupBy,
                metrics: chartComponent.getOption().metrics
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

                    const n = metricNamePrefix + group + (metricDef.displayName === undefined ? metricDef.name : metricDef.displayName);
                    let s = {
                        id: n,
                        name: n,
                        type: metricDef.chartType || 'line',

                        data: metric.values,
                        yAxisIndex: metricDef.yAxis == null ? 0 : metricDef.yAxis,

                        areaStyle: {opacity: 0.3},
                        lineStyle: {width: 1},
                        itemStyle: {opacity: 0},

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
                    replace: chartDescriptor.groupBy != null && mode === 'refresh',

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
        return (val) => val === null ? 0 : val;
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
