class TracePage {
    constructor(queryParams) {
        // Model
        this.mQueryParams = queryParams;
        this.mInterval = null;
        this.metricFilters = [];

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
        this.vFilters = new AppSelector({
            parentId: 'filterBar',
            intervalProvider: () => this.#getInterval()
        }).registerChangedListener((name, value) => {
            if (name === 'application') {
                window.location = `/web/app/trace/${value}`;
            } else {
                this.#refreshPage();
            }
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

            // get new interval
            this.mInterval = this.vIntervalSelector.getInterval();

            // refresh the page
            this.#refreshPage();
        });

        // View
        this.vIntervalSelector = new TimeInterval(window.queryParams['interval']).childOf(parent).registerIntervalChangedListener((selectedModel) => {
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
        //
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
            url: apiHost + '/api/trace/getTraceDistribution/v2',
            ajaxData: JSON.stringify({
                startTimeISO8601: interval.start,
                endTimeISO8601: interval.end,
                filters: this.#getFilters()
            }),
            processResult: (data) => {
                this._data = data;

                const timeLabels = data.map(val => {
                    return microFormat(val.lower) + "\n" + microFormat(val.upper);
                });

                const series = [{
                    name: 'percentage',
                    type: 'bar',
                    data: data.map(d => d['height']),
                    label: {
                        show: false,
                        formatter: (obj) => {
                            return obj.value > 0 ? "" + obj.value : "";
                        }
                    }
                }];

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
                },
                formatter: (series) => {
                    const dataIndex = series[0].dataIndex;

                    let tooltip = '';
                    series.forEach(s => {
                        //Concat the tooltip
                        //marker can be seen as the style of legend of this series
                        tooltip += `${s.axisValueLabel}<br />${s.marker}${s.seriesName}: ${s.data / 100.0}%`;
                    });
                    return tooltip;
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
                type: 'log',
                show: false,
                logBase: 10
            }
        };
    }

    #onClickChart(e) {
        const lower = this._data[e.dataIndex].lower;
        const upper = this._data[e.dataIndex].upper;

        this.metricFilters = [
            {
                type: 'metric',
                name: 'costTimeMs',
                matcher: {
                    type: 'between',
                    lower: lower,
                    upper: upper
                }
            }
        ];
        this.#refreshPage();
    }
}