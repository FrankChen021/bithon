class MetricComponent {

    constructor() {
        this._ctrl = null;
    }

    #createElement(id, clazz) {
        return "<div style='width: 100%' class='" + clazz + "'><div class='btn-group btn-group-sm' role='group' aria-label='...'>" +
        "<button class='btn btn-default select-time-length' id='select-hour-1' data-value='1'>1H</button>" +
        "<button class='btn btn-default select-time-length' id='select-hour-3' data-value='3'>3H</button>" +
        "<button class='btn btn-default select-time-length' id='select-hour-6' data-value='6'>6H</button>" +
        "<button class='btn btn-default select-time-length' id='select-hour-12' data-value='12'>12H</button>" +
        "<button class='btn btn-default select-time-length' id='select-hour-24' data-value='24'>24H</button></div></div>" +
        "<div id='" + id + "' style='height:400px;width:100%'></div></div>";
    }

    bindChangeHourEvents() {
        $(".select-time-length").click((e) => {
            const oldHours = this.option.hours;
            const newHours = parseInt($(e.target).attr('data-value'));
            if (oldHours === newHours) {
                return;
            }

            $("#select-hour-" + oldHours).removeClass('btn-primary').addClass('btn-default');
            $(e.target).removeClass('btn-default').addClass('btn-primary');

            this.option.hours = newHours;
            this.loadData(this.chart);
        });
    }

    showAfter(element, option) {
        this.option = $.extend({
            hours: 1,
            class: ''
        }, option)

        if (this._ctrl === null) {
            this._ctrl = $(this.#createElement(this.option.componentId, this.option.class));
            $(element).append(this._ctrl);
            $("#select-hour-" + this.option.hours).addClass('btn-primary');
            this.bindChangeHourEvents();
            this.chart = echarts.init(document.getElementById(this.option.componentId));
            window.addEventListener("resize", () => {
                this.chart.resize();
            });
        }

        this.loadData(this.chart);
    }

    dispose() {
        if (this._ctrl !== null) {
            this._ctrl.remove();
        }
        if (this.chart != null) {
            this.chart.dispose();
            this.chart = null;
        }
    }

    showDialog(option) {
        this.option = $.extend({
            hours: 3,
            class: '',
            title: '',
            dialogTitle: 'Metric'
        }, option)

        // show popup first
        bootbox.dialog({
            title: this.option.dialogTitle,
            size: 'xl',
            onEscape: true,
            backdrop: true,
            message: this.#createElement('popup_charts'),
            onShown: (e) => {
                $("#select-hour-" + this.option.hours).addClass('btn-primary');
                this.chart = echarts.init(document.getElementById("popup_charts"));
                window.addEventListener("resize", () => {
                    this.chart.resize();
                });

                this.bindChangeHourEvents();
                this.loadData(this.chart);
            },
            onHidden(e) {
                if (this.chart != null) {
                    this.chart.dispose();
                    this.chart = null;
                }
                this.option = null;
            }
        });
    }

    loadData(chart) {
        const dataSourceName = this.option.dataSourceName;
        const filterExpression = this.option.filterExpression;
        const metric = this.option.metric;
        const start = this.option.start;
        const end = Math.floor(this.option.end / 60000) * 60000;
        const window = this.option.window;
        const groupBy = this.option.groupBy;

        //$(".bootbox").find('.alert').css('display', 'none');
        chart.showLoading({text: 'Loading...'});

        $.ajax({
            type: "POST",
            url: "/api/datasource/timeseries/v3",
            async: true,
            data: JSON.stringify({
                dataSource: dataSourceName,
                interval: {
                    startISO8601: moment(end - 3600 * 1000 * this.option.hours).local().toISOString(true),
                    endISO8601: moment(end).local().toISOString(true),
                    minBucketLength: window * 60
                },
                filterExpression: filterExpression,
                fields: [{
                    field: metric.field,
                    name: metric.name,
                    aggregator: metric.aggregator
                }],
                groupBy: groupBy
            }),
            dataType: "json",
            contentType: "application/json",
            success: (data) => {
                chart.hideLoading();
                if (data.code > 200) {
                    this.showMessage(data.message);
                    return;
                }
                this.showLines(start, end, chart, metric, this.option.threshold, data);
            },
            error: (data) => {
                chart.hideLoading();
                console.log(data);
                this.showMessage(data.responseJSON.message);
            }
        });
    }

    showLines(start,
              end,
              chart,
              metric,
              expected,
              data) {

        const series = [];
        series.push({
            name: metric.name,
            type: 'line',
            data: data.data[0].values
        });

        const markLines = []
        if (this.option.range != null) {
            markLines.push({
                // The format text must be in lower case
                xAxis: new Date(this.option.range.start).format("hh:mm"),
                lineStyle: {color: 'red'}
            });
            markLines.push({
                xAxis: new Date(this.option.range.end).format("hh:mm"),
                lineStyle: {color: 'red'}
            });
        }

        if (this.option.threshold != null) {
            markLines.push({
                silent: false,
                lineStyle: {
                    type: "solid",
                    color: 'red', //#90c31d",
                },
                label: {
                    position: 'middle',
                    formatter: "Threshold = " + this.option.threshold + ""
                },
                yAxis: this.option.threshold
            });
        }
        if (markLines.length > 0) {
            series[0].markLine = {
                symbol: "none",
                label: {
                    position: "end",
                },
                data: markLines
            };
        }

        const timeLabels = [];
        for (let t = data.startTimestamp; t <= data.endTimestamp; t += data.interval) {
            timeLabels.push(moment(t).local().format('HH:mm'));
        }

        const startTimestamp = data.startTimestamp;
        const interval = data.interval;

        const option = {
            title: {
                text: this.option.title === undefined ? metric.aggregator + '(' + metric.field + ')' : this.option.title,
                left: "center",
                textStyle: {
                    fontSize: 14
                }
            },
            color: ['#0098d9', '#90c31d'],
            tooltip: {
                trigger: 'axis',
                formatter: (s) => {
                    const dataIndex = s[0].dataIndex;

                    const start = startTimestamp + dataIndex * interval;
                    const end = start + interval;
                    const startTimeText = moment(start).local().format('yy-MM-DD HH:mm');
                    const endTimeText = moment(end).local().format('yy-MM-DD HH:mm');
                    return `${startTimeText}<br/>${endTimeText}<br/>${s[0].seriesName}: ${s[0].value.toFixed(2)}`;
                }
            },
            grid: [{
                right: '0',
                left: '20',
                containLabel: true
            }],
            xAxis: [
                {
                    type: 'category',
                    axisTick: {
                        alignWithLabel: true
                    },
                    gridIndex: 0,
                    data: timeLabels
                }
            ],
            yAxis: [
                {
                    type: 'value',
                    name: metric.name,
                    position: 'left',
                    max: value => value.max > expected ? value.max : expected
                }
            ],
            series: series
        };

        chart.setOption(option);
    }

    showMessage(message) {
        this.chart.clear();
        this.chart.showLoading('default', {
            text: message,
            showSpinner: false,
            textColor: 'red'
        });
    }
}