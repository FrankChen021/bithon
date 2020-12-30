
class Dashboard {
    constructor(containerId, appName, schemaApi) {
        this._containerId = containerId;
        this._appName = appName;
        this._schemaApi = schemaApi;

        this._container = $('#'+containerId);

        this._stackLayoutRowFill = 0;
        this._stackLayoutRow = $('<div class="row"></div>');
        this._container.append(this._stackLayoutRow);

        this._chartComponents = new Object();
        this._chartDescriptors = new Object();
    }

    // PUBLIC
    load(dashboard) {
        this._dashboard = dashboard;

        this._schemaApi.getSchema(
            this._dashboard.dataSource,
            (schema)=>{
                // create filter
            },
            (error)=>{}
        );

        $.each(dashboard.charts, (index, chart)=>{
            this.createChartComponent(index, chart);
        });

        this.refreshDashboard();
    }

    // PRIVATE
    createChartComponent(index, chartDescriptor) {
        var chartId = 'chart_' + index;
        chartDescriptor['id'] = chartId;
        var chartContainer = this.layout(chartDescriptor.id, chartDescriptor.width*3);

        var chartOption = this.getDefaultChartOption();
        chartOption.legend.data = chartDescriptor.metrics.map(metric=> { return {
            name: metric.name,
            icon: 'circle'
        }});
        if ( chartDescriptor.yAxis != null ) {
            var formatterFns = chartDescriptor.yAxis.map( y => this.getFormatter(y.formatter) );
            for(var i = 1; i < formatterFns.length; i++) {
                if ( formatterFns[i] == null )
                    formatterFns[i] = formatterFns[i-1]; //default to prev config
            }
            chartOption.tooltip.formatter = params => {
                var currentChartOption = this.getChartCurrentOption(chartId);
                let result = (params[0] || params).axisValue;
                params.forEach(p => {
                    var yAxisIndex = currentChartOption.series[p.seriesIndex].yAxisIndex;
                    var formatterFn = formatterFns[yAxisIndex];
                    const s = formatterFn != null ? formatterFn(p.data) : p.data;
                    result += `<br />${p.marker}${p.seriesName}: ${s}`;
                });
                return result;
            };
            $.each(chartDescriptor.yAxis, (index, y)=>{
                chartOption.yAxis.push({
                   type: 'value',
                   min: 0 || y.min,
                   minInterval: 1 || y.minInterval,
                   interval: y.interval,
                   scale: false,
                   splitLine: { show: true },
                   axisLine: { show: false },
                   axisTick: {
                   show: false,
                   },
                   axisLabel: {
                       formatter: formatterFns[index]
                   },
               });
            });
        } else {
            chartOption.yAxis =[{
                type: 'value',
                min: 0,
                minInterval: 1,
                scale: true,
                splitLine: { show: true },
                axisLine: { show: false },
                axisTick: {
                show: false,
                },
                axisLabel: {
                },
            }];
        }

        var chartComponent = new ChartComponent({
            containerId: chartDescriptor.id,
            metrics: chartDescriptor.metrics.map(metric=>metric.name),
        }).header('<b>' + chartDescriptor.title + '</b>')
        .setChartOption(chartOption);

        this._chartComponents[chartDescriptor.id] = chartComponent;
        this._chartDescriptors[chartDescriptor.id] = chartDescriptor;
    }

    // PRIVATE
    layout(id, width) {
        if ( this._stackLayoutRowFill + width > 12 ) {
            // create a new row
            this._stackLayoutRowFill = 0;
            this._stackLayoutRow = $('<div class="row"></div>');
            this._container.append(this._stackLayoutRow);
        }
        this._stackLayoutRowFill += width;

        return this._stackLayoutRow.append(`<div class="form-group col-md-${width}" id="${id}"></div>`);
    }

    // PRIVATE
    refreshDashboard() {
        if ( this._interval == null ) {
            this._interval = setInterval(()=>{this.refreshDashboard();}, 10*1000);
        }

        // refresh each chart
        for(var id in this._chartComponents) {
            this.refreshChart(id);
        }
    }

    refreshChart(chartId) {
        var chartComponent = this._chartComponents[chartId];
        chartComponent.load({
            url: apiHost + "/api/datasource/metrics",
            ajaxData: JSON.stringify({
                dataSource: this._dashboard.dataSource,
                startTimeISO8601: moment().utc().subtract(10, 'minute').local().toISOString(),
                endTimeISO8601: moment().utc().local().toISOString(),
                dimensions: [{
                    "dimension": "appName",
                    "matcher": {
                        "type": "equal",
                        "pattern": this._appName
                    }
                }],
                metrics: chartComponent.getOption().metrics,
            }),
            processResult: (data)=>{
                var chartDescriptor = this._chartDescriptors[chartId];

                var timeLabels = data.map(d=> moment(d.timestamp).local().format('HH:mm:ss'));

                var series = chartDescriptor.metrics.map(metric => { return {
                    name: metric.name,
                    type: metric.chartType || 'line',
                    areaStyle: { opacity: 0.3 },
                    data: data.map(d=> { var val = d[metric.name]; return val == null ? 0 : val.toFixed(2)}),
                    lineStyle: { width: 1 },
                    itemStyle: { opacity: 0 },
                    yAxisIndex: metric.yAxis == null ? 0 : metric.yAxis
                }});

                return {
                    xAxis: { data: timeLabels },
                    series: series
                }
            }
        });
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
          series: [
          ],
        };
    }

    //PRIVATE
    getFormatter(type) {
        switch(type) {
            case 'binary_byte': return function(v){
                return binaryByteFormat(v);
            };
            case 'percentage': return function(v) {
                return v + '%';
            };
            case 'nano2Millisecond': return function(v) {
                return (v / 1000 / 1000).toFixed(2) + 'ms';
            }
            default: return null;
        }
    }
}