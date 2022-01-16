class TracePage {
    constructor(appName) {
        // Model
        this.mApplication = appName;
        this.mInterval = null;

        // View
        this.vChartComponent = new ChartComponent({
            containerId: 'distribution',
            height: '150px',
            showLegend: false
        });//.header('Distribution');
        this.vChartComponent.setClickHandler((e) => {
            this.#onClickChart(e);
        });

        // View
        new AppSelector(this.mApplication).childOf('appSelector').registerAppChangedListener((text, value) => {
            window.location = `/web/app/trace/${value}`;
        });

        const parent = $('#filterBarForm');

        // View - Refresh Button
        parent.append('<button class="btn btn-outline-secondary" style="border-radius:0px;border-color: #ced4da" type="button"><i class="fas fa-sync-alt"></i></button>')
            .find("button").click(() => {
            this.mInterval = this.vIntervalSelector.getInterval();
            this.#refreshPage();
        });

        // View
        this.vIntervalSelector = new TimeInterval(this._defaultInterval).childOf(parent).registerIntervalChangedListener((selectedModel) => {
            this.mInterval = this.vIntervalSelector.getInterval();
            this.#refreshPage();
        });
        this.mInterval = this.vIntervalSelector.getInterval();

        // View, will also trigger refresh automatically
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
            pageList: [10, 25, 50, 100],
            sortName: 'startTime',
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
                    var timestamp = row.startTime / 1000;
                    timestamp = Math.floor(timestamp / 1000 / 60) * 1000 * 60;
                    timestamp -= 3600 * 1000; // search the detail from 1 hour before current start time
                    return `<a target="_blank" href="/web/trace/detail?id=${row.traceId}&type=trace&interval=${encodeURI(moment(timestamp).local().toISOString(true) + '/')}">${value}</a>`;
                },
            }, {
                field: 'instanceName',
                title: 'Instance'
            }, {
                field: 'startTime',
                title: 'Time',
                formatter: function (value) {
                    return new Date(value / 1000).format('yyyy-MM-dd hh:mm:ss');
                },
                sortable: true
            }, {
                field: 'costTime',
                title: 'Duration',
                formatter: function (value, row) {
                    return nanoFormat(value * 1000);
                },
                sortable: true
            }, {
                field: 'tags',
                title: 'URL',
                formatter: function (value, row) {
                    return value.uri;
                }
            }, {
                field: 'tags',
                title: 'Status',
                formatter: function (value, row) {
                    return value.status;
                }
            }],

            rowStyle: (row, index) => {
                if (row.tags.status !== "200") {
                    return {
                        classes: 'alert-warning'
                    }
                }
                return {};
            }
        });

        this.#refreshChart();
    }

    #getInterval() {
        return this.mInterval;//this.mInterval != null ? this.mInterval : this.vIntervalSelector.getInterval();
    }

    #refreshPage() {
        //
        // refresh the list
        //
        $('#table').bootstrapTable('refresh');

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
                application: g_SelectedApp
            }),
            processResult: (data) => {
                this._data = data;

                const labelFormat = data.bucket < 60 ? 'HH:mm:ss' : 'HH:mm';
                const timeLabels = data.data.map(val => {
                    // it's unix timestamp, so * 1000 is needed to convert it to milliseconds
                    return moment(val._timestamp * 1000 ).local().format(labelFormat) + "\n"
                        + moment(val._timestamp * 1000 + data.bucket * 1000).local().format(labelFormat)
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
        const s = moment(this._data.data[e.dataIndex]._timestamp * 1000).utc();
        const interval = {
            start: s.toISOString(),
            end: s.add(this._data.bucket, 'second').toISOString()
        }
        if (this.mInterval == null || (this.mInterval.start !== interval.start || this.mInterval.end !== interval.end)) {
            this.mInterval = interval;
            this.#refreshPage();
        }
    }
}