class ProfilingPage {
    constructor() {
        // Model
        this.mStartTime = 0;

        // this View Model
        this.mInterval = null;

        // View
        this.vChartComponent = new ChartComponent({
            containerId: 'profiling',
            height: '700px',
            showLegend: false
        }).header('<b>Profiling</b>');
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
            }
            this.#refreshPage();
        }).createFilter('trace_span_summary', true);

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
            // get the new interval
            this.mInterval = this.vIntervalSelector.getInterval();

            // refresh the page
            this.#refreshPage();
        });

        // View
        this.vIntervalSelector = new TimeSpanSelector(window.queryParams['interval']).childOf(parent).registerIntervalChangedListener((selectedModel) => {
            this.mInterval = this.vIntervalSelector.getInterval();
            this.#refreshPage();
        });
        this.mInterval = this.vIntervalSelector.getInterval();
    }

    #getInterval() {
        return this.mInterval;
    }

    #getFilterExpression() {
        let summaryTableFilter = this.vFilters.getSelectedFilterExpression();
        let tagFilters = this.vTagFilter.getSelectedFilterExpression();
        return String.join(
            ' AND ',
            summaryTableFilter,
            tagFilters,
            "kind in ('SERVER', 'CONSUMER', 'TIMER')"
            );
    }

    #refreshPage() {
        this.#refreshChart();
    }

    #refreshChart() {
        const filters = this.vFilters.getSelectedFilters();
        let isAppSelected = false;
        let isInstanceSelected = false;
        for (let i = 0; i < filters.length; i++) {
            switch (filters[i].field) {
                case 'appName':
                    isAppSelected = true;
                    break;
                case 'instanceName':
                    isInstanceSelected = true;
                    break;
                default:
                    break;
            }
        }
        if (!isAppSelected) {
            bootbox.alert('Please select an application.');
            return;
        }
        if (!isInstanceSelected) {
            bootbox.alert('Please select an instance.');
            return;
        }

        const interval = this.#getInterval();
        this.vChartComponent.load({
            url: apiHost + '/api/trace/getTraceList',
            ajaxData: JSON.stringify({
                startTimeISO8601: interval.start,
                endTimeISO8601: interval.end,
                expression: this.#getFilterExpression(),
                orderBy: 'startTime',
                order: 'asc',
                pageNumber: 0,
                pageSize: 1000
            }),
            processResult: (data) => {
                const ret = this.#toProfilingLogs(data.rows);
                this._data = ret.spans;

                const categories = [];
                for (let i = 0; i < ret.rows; i++) {
                    categories.push(i);
                }
                const op = this.getDefaultChartOption();
                op.yAxis = {
                    data: categories
                };
                //op.xAxis = {data: timeLabels, type: 'category'};
                op.series[0].data = this._data;
                return op;
            }
        });
    }

    #toProfilingLogs(spans) {
        const rows = [];

        this.mStartTime = spans.length > 0 ? spans[0].startTime : 0;
        for (let i = 0; i < spans.length; i++) {
            const span = spans[i];

            // always search the row from index 0 so that the latest rows are used
            let found = -1;
            for (let j = 0; j < rows.length; j++) {
                if (span.startTime > rows[j].endTime) {
                    // this span can be put in this row
                    found = j;
                    break;
                }
            }
            if (found === -1) {
                // add a new layer
                rows.push({
                    index: rows.length,
                    endTime: span.endTime
                });
                found = rows.length - 1;
            }
            rows[found].endTime = span.endTime;

            span.value = [
                found,
                span.startTime - this.mStartTime,
                span.endTime - this.mStartTime,
                span.costTime / 1000
            ];

            // apply item style
            this.#applySpanLogStyle(span);
        }

        return {
            spans: spans,
            rows: rows.length
        };
    }

    #applySpanLogStyle(span) {
        if (span.status !== '200') {
            let color = '';
            switch (span.status.charAt(0)) {
                case '4':
                    color = 'orange';
                    break;
                case '5':
                    color = 'red';
                    break;
                default:
                    color = 'yellow';
                    break;
            }
            span.itemStyle = {
                normal: {
                    color: color
                }
            };
        }
    }

    #format(obj, name, formatter = (v) => v) {
        const val = obj[name];
        if (val !== undefined) {
            return `<b>${name}</b> : ${formatter(val)}<br/>`;
        }
        return '';
    }

    // PRIVATE
    getDefaultChartOption() {
        return {
            tooltip: {
                formatter: (params) => {
                    const span = params.data;
                    if (span.tooltip === undefined) {
                        let tooltip = this.#format(span, 'normalizedUri');
                        tooltip += this.#format(span, 'status');
                        tooltip += this.#format(span, 'startTime', (v) => new Date(v / 1000).format('MM-dd hh:mm:ss'));
                        tooltip += this.#format(span, 'costTime', microFormat);

                        const filterNames = this.vTagFilter.getFilterName();
                        for (let i = 0; i < filterNames; i++) {
                            tooltip += this.#format(span, filterNames[i]);
                        }

                        span.tooltip = tooltip;
                    }
                    return span.tooltip;
                }
            },
            dataZoom: [
                {
                    type: 'slider',
                    filterMode: 'weakFilter',
                    showDataShadow: false,
                    bottom: 20,
                    labelFormatter: ''
                },
                {
                    type: 'inside',
                    filterMode: 'weakFilter'
                }
            ],
            grid: {
                height: 550,
                left: 50,
                right: 80
            },
            xAxis: {
                min: 0,
                scale: true,
                axisLabel: {
                    formatter: (val) => microFormat(val) + '\n' + new Date((this.mStartTime + val) / 1000).format('MM-dd hh:mm:ss')
                }
            },
            series: [
                {
                    type: 'custom',
                    renderItem: this.#renderItem,
                    itemStyle: {
                        opacity: 0.8
                    },
                    encode: {
                        x: [1, 2],
                        y: 0
                    }
                }
            ]
        };
    }

    #renderItem(params, api) {
        const categoryIndex = api.value(0);
        const start = api.coord([api.value(1), categoryIndex]);
        const end = api.coord([api.value(2), categoryIndex]);
        const height = api.size([0, 1])[1] * 0.6;
        const rectShape = echarts.graphic.clipRectByRect(
            {
                x: start[0],
                y: start[1] - height / 2,
                width: end[0] - start[0],
                height: height
            },
            {
                x: params.coordSys.x,
                y: params.coordSys.y,
                width: params.coordSys.width,
                height: params.coordSys.height
            }
        );
        return (
            rectShape && {
                type: 'rect',
                transition: ['shape'],
                shape: rectShape,
                style: api.style()
            }
        );
    }

    #onClickChart(e) {
        const span = this._data[e.dataIndex];
        window.open(`/web/trace/detail/?id=${span.traceId}`);
    }
}