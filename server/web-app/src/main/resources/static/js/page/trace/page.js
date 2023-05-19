class TracePage {
    /**
     * @param options {
     *     queryParams: {},
     *     filterExpression: '',
     *     showSelector: true | false,
     *     excludeColumns: an array of String that contains the columns that are excluded from the list
     * }
     */
    constructor(options) {
        // Model
        this.mQueryParams = options.queryParams;
        this.filterExpression = options.filterExpression
        this.mInterval = null;

        // View
        this.vChartComponent = new ChartComponent({
            containerId: 'distribution',
            height: '150px',
            showLegend: false
        }).setChartOption(this.getDefaultChartOption());
        this.vChartComponent.setClickHandler((e) => {
            this.#onClickChart(e);
        });

        // View
        this.vFilters = null;
        this.vTagFilter = null;
        if (options.showSelector) {
            this.vFilters = new AppSelector({
                parentId: 'filterBar',
                intervalProvider: () => this.#getInterval()
            }).registerChangedListener((name, value) => {
                if (name === 'application') {
                    g_SelectedApp = value;
                    window.history.pushState('', '', `/web/app/trace/list?appName=${value}`);
                }
                this.#refreshPage();
            }).createAppSelector(this.mQueryParams['appName'])
                .createFilter('trace_span_summary');

            // View, tag filter
            this.vTagFilter = new AppSelector({
                parentId: 'filterBar',
                intervalProvider: () => this.#getInterval()
            }).registerChangedListener((name, value) => {
                this.#refreshPage();
            }).createFilter('trace_span_tag_index');
        }

        const parent = $('#filterBarForm');

        // View
        this.vIntervalSelector = new TimeInterval(window.queryParams['interval'])
            .childOf(parent)
            .registerIntervalChangedListener((selectedModel) => {
                this.mInterval = this.vIntervalSelector.getInterval();
                this.#refreshPage();
            });
        this.mInterval = this.vIntervalSelector.getInterval();

        // View - Auto Refresh Button
        parent.append('<button class="btn btn-outline-secondary" style="border-radius:0;border-color: #ced4da" type="button"><i class="fas fa-sync-alt"></i></button>')
            .find("button").click(() => {
            // get a new interval
            this.mInterval = this.vIntervalSelector.getInterval();

            // refresh the page
            this.#refreshPage();
        });

        // View, will also trigger refresh automatically
        this.vTraceList = new TraceListComponent({
            parent: $('#table'),
            excludeColumns: options.excludeColumns || [],
            getQueryParams: (params) => {
                return {
                    filters: this.#getFilters(),
                    startTimeISO8601: this.mInterval.start,
                    endTimeISO8601: this.mInterval.end,
                    expression: this.filterExpression
                };
            }
        });

        // If the tag filters are not selectable, add them to the filterExpression
        const tagFilters = {};
        if (this.vTagFilter !== null) {
            $.each(this.vTagFilter.getFilterName(), (index, name) => {
                tagFilters["tags." + name] = true;
            });
        }
        $.each(options.queryParams, (name, value) => {
            if (name.startsWith("tags.") && tagFilters[name] === undefined) {
                if (this.filterExpression.length > 0) {
                    this.filterExpression += ' AND ';
                }
                this.filterExpression += `${name} = '${value}'`
            }
        });

        $("#filter-input")
            .val(this.filterExpression)
            .on('keydown', (event) => {
                this.filterExpression = event.target.value;
                if (event.keyCode === 13) {
                    this.#refreshPage();
                }
            })

        //
        // Model for distribution chart
        //
        this.columns = {
            "minResponse": {chartType: 'line', fill: false, yAxis: 0, formatter: (v) => microFormat(v, 2)},
            "maxResponse": {chartType: 'line', fill: false, yAxis: 0, formatter: (v) => microFormat(v, 2)},
            "avgResponse": {chartType: 'line', fill: false, yAxis: 0, formatter: (v) => microFormat(v, 2)},
            "count": {chartType: 'bar', fill: false, yAxis: 1}
        };

        this.#refreshChart();
    }

    #getInterval() {
        return this.mInterval;
    }

    #getFilters() {
        if (this.vFilters !== null) {
            let summaryTableFilter = this.vFilters.getSelectedFilters();
            let tagFilters = this.vTagFilter.getSelectedFilters();
            return summaryTableFilter.concat(tagFilters);
        } else {
            return [];
        }
    }

    #refreshPage() {
        // refresh the list
        //
        this.vTraceList.refresh();

        //
        // refresh the distribution chart
        //
        this.#refreshChart();
    }

    #refreshChart() {
        const interval = this.#getInterval();
        this.vChartComponent.load({
            url: apiHost + '/api/trace/getTraceDistribution',
            ajaxData: JSON.stringify({
                startTimeISO8601: interval.start,
                endTimeISO8601: interval.end,
                filters: this.#getFilters(),
                expression: this.filterExpression
            }),
            processResult: (data) => {
                this._data = data;

                const timeLabels = [];
                for (let t = data.startTimestamp; t <= data.endTimestamp; t += data.interval) {
                    timeLabels.push(moment(t).local().format('HH:mm:ss'));
                }

                const series = [];
                $.each(data.metrics, (index, metric) => {
                    // The last position is the name of metric
                    let metricName = metric.tags[metric.tags.length - 1];

                    let column = this.columns[metricName];
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
                    const isBar = column.chartType === 'bar';

                    const n = group + metricName;
                    const s = {
                        id: n,
                        name: n,
                        type: chartType,

                        data: metric.values,
                        yAxisIndex: column.yAxis || 0,

                        areaStyle: isArea ? {opacity: 0.3} : null,
                        lineStyle: isLine ? {width: 1} : null,
                        itemStyle: isLine ? {opacity: 0} : null,

                        label: {
                            show: isBar,
                            formatter: (v) => {
                                return v.value > 0 ? v.value.toString() : '';
                            }
                        },

                        // selected is not a property of series
                        // this is used to render default selected state of legend by chart-component
                        selected: column.selected === undefined ? true : column.selected
                    };
                    series.push(s);
                });

                return {
                    refreshMode: 'refresh',

                    // save the timestamp for further processing
                    timestamp: {
                        start: data.startTimestamp,
                        interval: data.interval
                    },
                    xAxis: {
                        data: timeLabels
                    },
                    series: series
                };
            }
        });
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
                    }
                },
                formatter: (series) => {
                    const dataIndex = series[0].dataIndex;

                    const option = this.vChartComponent.getChartOption();
                    const start = option.timestamp.start;
                    const interval = option.timestamp.interval;
                    let tooltip = moment(start + dataIndex * interval).local().format('MM-DD HH:mm:ss')
                        + '<br/>'
                        + moment(start + dataIndex * interval + interval).local().format('MM-DD HH:mm:ss');
                    series.forEach(s => {
                        const formatter = this.columns[s.seriesName].formatter;
                        const text = formatter === undefined ? s.data : formatter(s.data);
                        //Concat the tooltip
                        //marker can be seen as the style of this series's legend
                        tooltip += `<br />${s.marker}${s.seriesName}: ${text}`;
                    });
                    return tooltip;
                }
            },
            grid: {
                left: '3.5%',
                right: 0,
                bottom: 30,
                top: 10,
            },
            xAxis: {
                type: 'category',
                axisLabel: {},
                data: [],
            },
            yAxis: [{
                type: 'value',
                min: 0,
                minInterval: 1,
                splitLine: {show: true},
                axisLine: {show: false},
                scale: false,
                axisTick: {
                    show: false,
                },
                axisLabel: {
                    formatter: (v) => microFormat(v, 2)
                },
            }, {
                type: 'value',
                min: 0,
                minInterval: 1,
                splitLine: {show: true},
                axisLine: {show: false},
                scale: false,
                axisTick: {
                    show: false,
                }
            }]
        };
    }

    #onClickChart(e) {
        for(let i = 0; i < this._data.metrics.length; i++) {
            const metric = this._data.metrics[i];
            if (metric.tags[0] === 'count' && metric.values[e.dataIndex] === 0) {
                // The 'count' metric is zero, no need to response the click event
                return;
            }
        }

        const startTimestamp = this._data.startTimestamp + this._data.interval * e.dataIndex;
        const endTimestamp = startTimestamp + this._data.interval;

        const startISO8601 = moment(startTimestamp).utc().local().toISOString(true);
        const endISO8601 = moment(endTimestamp).utc().local().toISOString(true);
        if (startISO8601 === this.mInterval.start && endISO8601 === this.mInterval.end) {
            return;
        }

        this.vIntervalSelector.setInterval(startTimestamp, endTimestamp);
    }
}