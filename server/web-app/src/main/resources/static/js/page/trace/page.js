class TracePage {
    constructor(appName) {
        // Model
        this._appName = appName;
        this._interval = null;

        // View
        this._chartComponent = new ChartComponent({
            containerId: 'distribution',
            height: '150px',
            showLegend: false
        });//.header('Distribution');
        this._chartComponent.setClickHandler((e) => {
            this.#onClickChart(e);
        });

        // View
        new AppSelector(this._appName).childOf('appSelector').registerAppChangedListener((text, value) => {
            window.location = `/web/app/trace/${value}?interval=${g_MetricSelectedInterval}`;
        });

        // View
        const parent = $('#filterBarForm');
        this._timeSelector = new TimeInterval(this._defaultInterval).childOf(parent).registerIntervalChangedListener((selectedModel) => {
            this._interval = null;
            this.#refreshPage();
        });

        // View
        $('#table').bootstrapTable({
            toolbar: '#toolbar',//工具栏

            url: apiHost + '/api/trace/getTraceList',
            method: 'post',
            contentType: "application/json",
            showRefresh: false,

            buttonsAlign: 'right',
            sidePagination: "server",
            pagination: true,
            paginationPreText: '<',              //上一页按钮样式
            paginationNextText: '>',             //下一页按钮样式
            pageNumber: 1,
            pageSize: 10,
            pageList: [10, 25, 50],
            sortName: 'lastAlertAt',
            sortOrder: 'desc',

            queryParamsType: '',
            queryParams: (params) => {
                const interval = this.#getInterval();
                return {
                    pageSize: params.pageSize,
                    pageNumber: params.pageNumber - 1,
                    traceId: params.searchText,
                    application: g_SelectedApp,
                    startTimeISO8601: interval.start,
                    endTimeISO8601: interval.end,
                    orderBy: params.sortName,
                    order: params.sortOrder
                };
            },

            filterControl: false,
            filterShowClear: false,
            search: false,
            showSearchClearButton: false,
            searchOnEnterKey: true,
            formatSearch: function () {
                return 'search by Trace Id';
            },

            uniqueId: 'traceId',
            columns: [{
                field: 'traceId',
                title: 'Trace Id',
                formatter: function (value, row) {
                    return `<a target="_blank" href="/web/trace/detail?id=${row.traceId}">${value}</a>`;
                }
            }, {
                field: 'startTime',
                title: 'Time',
                formatter: function (value) {
                    return new Date(value / 1000).format('yyyy-MM-dd hh:mm:ss');
                }
            }, {
                field: 'costTime',
                title: 'Duration',
                formatter: function (value, row) {
                    return nanoFormat(value * 1000);
                }
            }, {
                field: 'tags',
                title: 'URL',
                formatter: function (value, row) {
                    return value.uri;
                }
            }]
        });

        this.#refreshPage();
    }

    #getInterval() {
        return this._interval != null ? this._interval : this._timeSelector.getInterval();
    }

    #refreshPage() {
        //
        // refresh the list
        //
        $('#table').bootstrapTable('refresh');

        //
        // refresh the distribution chart
        //
        const interval = this.#getInterval();
        this._chartComponent.load({
            url: apiHost + '/api/trace/getTraceDistribution',
            ajaxData: JSON.stringify({
                startTimeISO8601: interval.start,
                endTimeISO8601: interval.end,
                application: g_SelectedApp
            }),
            processResult: (data) => {
                this._data = data;

                const timeLabels = data.data.map(val => {
                    return moment(val.timestamp).local().format('HH:mm') + "\n"
                        + moment(val.timestamp + data.bucket * 1000).local().format('HH:mm')
                });

                const series = [{
                    name: "count",
                    displayName: "count",
                    chartType: "bar"
                }].map(metric => {
                    return {
                        name: (metric.displayName === undefined ? metric.name : metric.displayName),
                        type: metric.chartType || 'bar',
                        //areaStyle: {opacity: 0.3},
                        data: data.data.map(d => d[metric.name]),
                        //lineStyle: {width: 1},
                        //itemStyle: {opacity: 0},
                        //yAxisIndex: metric.yAxis == null ? 0 : metric.yAxis,
                        label: {
                            show: true,
                            formatter: (obj) => {
                                return obj.value > 0 ? "" + obj.value : "";
                            }
                        },
                        // selected is not a property of series
                        // this is used to render default selected state of legend by chart-component
                        selected: metric.selected === undefined ? true : metric.selected
                    }
                });
                const op = this.getDefaultChartOption();
                op.xAxis = {data: timeLabels, type: 'category'};
                op.series = series;
                return op;
            }
        })

        //
        // refresh the table list
        //
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
                left: 0,
                right: 0,
                bottom: 40,
                top: 10,
            },
            xAxis: {
                type: 'category',
                boundaryGap: false,
                axisLabel: {},
                data: [],
            },
            yAxis: {
                type: 'value',
                show: false
            }
        };
    }

    #onClickChart(e) {
        const s = moment(this._data.data[e.dataIndex].timestamp).utc();
        const interval = {
            start: s.toISOString(),
            end: s.add(this._data.bucket, 'second').toISOString()
        }
        if (this._interval == null || (this._interval.start !== interval.start && this._interval.end !== interval.end)) {
            this._interval = interval;
            this.#refreshPage();
        }
    }
}