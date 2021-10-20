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
        this._stackLayoutRow = $('<div class="row"></div>');
        this._container.append(this._stackLayoutRow);

        // Model
        this._schema = {};
        this._chartComponents = {};
        this._chartDescriptors = {};
        this._selectedDimensions = {};
        this.addDimension('appName', appName);

        // UI Component
        this._timeSelector = null;

        // Y Axis Formatter
        this._formatters = {};
        this._formatters['binary_byte'] = binaryByteFormat;
        this._formatters['compact_number'] = compactFormat;
        this._formatters['percentage'] = (v) => v + '%';
        this._formatters['nanoFormatter'] = (v) => nanoFormat(v, 0);
        this._formatters['millisecond'] = (v) => milliFormat(v, 0);
        this._formatters['microsecond'] = (v) => microFormat(v, 0);
    }

    // PUBLIC
    load(dashboard) {
        this._dashboard = dashboard;

        //
        // App Filter
        //
        new AppSelector(this._appName).childOf('appSelector').registerAppChangedListener((text, value) => {
            window.location = `/web/app/metric/${value}/${this._dashboardName}`;
            // update appName for dimension filters
            //this._appName = value;

            // update dimensions for dashboard chart
            //this.addDimension('appName', value);

            //this.refreshDashboard();
        });

        //
        // dataSource --> Charts
        //
        const dataSource2Charts = {};
        $.each(this._dashboard.charts, (index, chartDescriptor) => {
            const chartId = 'chart_' + index;
            chartDescriptor['id'] = chartId;

            const dataSourceName = chartDescriptor.dataSource;
            if (dataSource2Charts[dataSourceName] == null) {
                dataSource2Charts[dataSourceName] = [];
            }
            dataSource2Charts[dataSourceName].push(chartId);
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
                        this._schema = schema;

                        // create dimension filter
                        // Note: first two dimensions MUST be app/instance
                        const filterBar = $('#filterBar');
                        for (index = 1; index < schema.dimensionsSpec.length; index++) {
                            const dimension = schema.dimensionsSpec[index];
                            if (!dimension.visible)
                                continue;

                            this.createDimensionFilter(filterBar, index, dimension.name, dimension.displayText);
                        }
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
                },
                (error) => {
                }
            );
        }

        //
        // Create AutoRefresher
        //
        const parent = $('#filterBarForm');
        new AutoRefresher({
            timerLength: 10
        }).childOf(parent).registerRefreshListener(() => {
            this.refreshDashboard();
        });

        //
        // Create TimeInterval
        //
        this._timeSelector = new TimeInterval(this._defaultInterval).childOf(parent).registerIntervalChangedListener((selectedModel) => {
            g_MetricSelectedInterval = selectedModel.id;
            this.refreshDashboard();
        });

        $.each(dashboard.charts, (index, chartDescriptor) => {

            this.layout(chartDescriptor.id, chartDescriptor.width * 3);

            this.createChartComponent(chartDescriptor.id, chartDescriptor)
                .setOpenHandler(() => {
                    this.openChart(chartDescriptor.id);
                });

            this._chartDescriptors[chartDescriptor.id] = chartDescriptor;
        });
    }

    // PRIVATE
    createDimensionFilter(filterBar, dimensionIndex, dimensionName, displayText) {
        const appendedSelect = filterBar.append(`<li class="nav-item"><select style="width:150px"></select></li>`).find('select').last();
        if (dimensionIndex === 1 && g_SelectedInstance != null) {
            appendedSelect.append(`<option value="${g_SelectedInstance}">${g_SelectedInstance}</option>`);
        }
        appendedSelect.select2({
            theme: 'bootstrap4',
            allowClear: true,
            dropdownAutoWidth: true,
            placeholder: displayText,
            ajax: this.getDimensionAjaxOptions(this._dashboard.charts[0].dataSource, dimensionIndex, dimensionName),
        }).on('change', (event) => {
            if (event.target.selectedIndex == null || event.target.selectedIndex < 0) {
                if (dimensionIndex === 1) {
                    g_SelectedInstance = null;
                }
                this.rmvDimension(dimensionName);
                return;
            }

            // get selected dimension
            const dimensionValue = event.target.selectedOptions[0].value;

            if (dimensionIndex === 1) {
                g_SelectedInstance = dimensionValue;
            }
            this.addDimension(dimensionName, dimensionValue);
        });

        if (dimensionIndex === 1 && g_SelectedInstance != null) {
            appendedSelect.change();
        }
    }

    // PRIVATE
    getDimensionAjaxOptions(dataSourceName, dimensionIndex, dimensionName) {
        return {
            cache: true,
            type: 'POST',
            url: apiHost + '/api/datasource/dimensions',
            data: () => {
                const filters = [];

                for (let p = 0; p < dimensionIndex; p++) {
                    const dim = this._schema.dimensionsSpec[p];
                    if (this._selectedDimensions[dim.name] != null) {
                        filters.push(this._selectedDimensions[dim.name]);
                    }
                }

                const interval = this.getSelectedTimeInterval();
                return JSON.stringify({
                    dataSource: dataSourceName,
                    dimension: dimensionName,
                    conditions: filters,
                    startTimeISO8601: interval.start,
                    endTimeISO8601: interval.end,
                })
            },
            dataType: "json",
            contentType: "application/json",
            processResults: (data) => {
                return {
                    results: data.map(dimension => {
                        return {
                            "id": dimension.value,
                            "text": dimension.value
                        };
                    })
                };
            }
        }
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
                    const s = formatterFn != null ? formatterFn(p.data) : p.data;
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
            this._stackLayoutRow = $('<div class="row"></div>');
            this._container.append(this._stackLayoutRow);
        }
        this._stackLayoutRowFill += width;

        return this._stackLayoutRow.append(`<div class="form-group col-md-${width}" id="${id}" style="margin-bottom: 0;padding-bottom: 10px;padding-left: 5px;padding-right: 5px"></div>`);
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

    // PUBLIC
    /*
     * {
     *     "dimension": "appName",
     *     "matcher": {
     *         "type": "equal",
     *         "pattern": this._appName
     *     }
     * }
     */
    addDimension(dimensionName, dimensionValue) {
        this._selectedDimensions[dimensionName] = {
            dimension: dimensionName,
            matcher: {
                type: 'equal',
                pattern: dimensionValue
            }
        };
        this.refreshDashboard();
    }

    rmvDimension(dimensionName) {
        delete this._selectedDimensions[dimensionName];
        this.refreshDashboard();
    }

    refreshChart(chartDescriptor, chartComponent, interval, metricNamePrefix) {
        if (metricNamePrefix == null) {
            metricNamePrefix = '';
        }
        var dimensions = {};
        if ( chartDescriptor.dimensions === undefined) {
            dimensions = this._selectedDimensions;
        } else {
            Object.assign(dimensions, this._selectedDimensions);
            Object.assign(dimensions, chartDescriptor.dimensions);
        }
        chartComponent.load({
            url: apiHost + "/api/datasource/metrics",
            ajaxData: JSON.stringify({
                dataSource: chartDescriptor.dataSource,
                startTimeISO8601: interval.start,
                endTimeISO8601: interval.end,
                dimensions: dimensions,
                metrics: chartComponent.getOption().metrics
            }),
            processResult: (data) => {
                const timeLabels = data.map(d => moment(d.timestamp).local().format('HH:mm:ss'));

                const series = chartDescriptor.metrics.map(metric => {

                    return {
                        name: metricNamePrefix + (metric.displayName === undefined ? metric.name : metric.displayName),
                        type: metric.chartType || 'line',
                        areaStyle: {opacity: 0.3},
                        data: data.map(d => metric.transformer(d, metric.name)),
                        lineStyle: {width: 1},
                        itemStyle: {opacity: 0},
                        yAxisIndex: metric.yAxis == null ? 0 : metric.yAxis,

                        // selected is not a property of series
                        // this is used to render default selected state of legend by chart-component
                        selected: metric.selected === undefined ? true : metric.selected
                    }
                });

                return {
                    xAxis: {data: timeLabels},
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
                if (yAxis.format === 'millisecond' && schema.metricsSpec[metricName].unit === 'nanosecond') {
                    return function (data, metricName) {
                        const val = data[metricName];
                        return val == null ? 0 : (val / 1000 / 1000).toFixed(2);
                    }
                }
            }
        }
        return function (data, metricName) {
            const val = data[metricName];
            return val == null ? 0 : val.toFixed(2);
        }
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
                        text + '-');
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
        return this._timeSelector.getInterval();
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
