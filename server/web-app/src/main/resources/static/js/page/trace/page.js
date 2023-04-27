class TracePage {
    constructor(queryParams) {
        // Model
        this.mQueryParams = queryParams;
        this.mInterval = null;
        this.metricFilters = [];
        this.moreFilter = '';

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
        this.vFilters = new AppSelector({
            parentId: 'filterBar',
            intervalProvider: () => this.#getInterval()
        }).registerChangedListener((name, value) => {
            if (name === 'application') {
                g_SelectedApp = value;
                window.history.pushState('', '', `/web/app/trace/${value}`);
            }
            this.#refreshPage();
        }).createAppSelector(this.mQueryParams['appName'])
            .createFilter('trace_span_summary');

        // View, tag filter
        this.vTagFilter = new AppSelector({
            parentId: 'filterBar',
            queryVariablePrefix: 'tags.',
            intervalProvider: () => this.#getInterval()
        }).registerChangedListener((name, value) => {
            this.#refreshPage();
        }).createFilter('trace_span_tag_index');

        const parent = $('#filterBarForm');

        // View - Refresh Button
        parent.append('<button class="btn btn-outline-secondary" style="border-radius:0;border-color: #ced4da" type="button"><i class="fas fa-sync-alt"></i></button>')
            .find("button").click(() => {
            // reset the metric filter
            this.metricFilters = [];

            // get a new interval
            this.mInterval = this.vIntervalSelector.getInterval();

            // refresh the page
            this.#refreshPage();
        });

        // View
        this.vIntervalSelector = new TimeInterval(window.queryParams['interval'])
            .childOf(parent)
            .registerIntervalChangedListener((selectedModel) => {
                this.mInterval = this.vIntervalSelector.getInterval();
                this.#refreshPage();
            });
        this.mInterval = this.vIntervalSelector.getInterval();

        // View, will also trigger refresh automatically
        this.vTraceList = new TraceListComponent(
            $('#table'),
            {
                showApplicationName: false,
                getQueryParams: (params) => {
                    return {
                        filters: this.#getFilters(),
                        startTimeISO8601: this.mInterval.start,
                        endTimeISO8601: this.mInterval.end
                    };
                }
            }
        );

        //
        // Process additional tags filter
        this.moreFilter = window.queryParams['more'];
        if (this.moreFilter !== undefined && this.moreFilter !== '') {
            this.moreFilter = atob(this.moreFilter);
        }
        $("#filter-input").val(this.moreFilter);

        // Model for distribution chart
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
        let summaryTableFilter = this.vFilters.getSelectedFilters();
        let tagFilters = this.vTagFilter.getSelectedFilters();
        return summaryTableFilter.concat(tagFilters, this.metricFilters);
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
                expression: this.moreFilter
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
        const startTimestamp = this._data.startTimestamp + this._data.interval * e.dataIndex;
        const endTimestamp = startTimestamp + this._data.interval;

        const startISO8601 = moment(startTimestamp).utc().local().toISOString(true);
        const endISO8601 = moment(endTimestamp).utc().local().toISOString(true);
        if (startISO8601 === this.mInterval.start && endISO8601 === this.mInterval.end) {
            return;
        }

        this.vIntervalSelector.setInternal(startTimestamp, endTimestamp);
    }
}