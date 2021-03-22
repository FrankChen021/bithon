
class Dashboard {
    constructor(containerId, appName, schemaApi) {
        this._appName = appName;
        this._schemaApi = schemaApi;

        // View
        this._containerId = containerId;
        this._container = $('#'+containerId);
        this._stackLayoutRowFill = 0;
        this._stackLayoutRow = $('<div class="row"></div>');
        this._container.append(this._stackLayoutRow);

        // Model
        this._chartComponents = new Object();
        this._chartDescriptors = new Object();
        this._dimensions = new Object();
        this.addDimension('appName', appName);

        this._intervalFn = ()=>{
            return {
                start: moment().utc().subtract(5, 'minute').local().toISOString(),
                end: moment().utc().local().toISOString()
            };
        };
    }

    // PUBLIC
    load(dashboard) {
        this._dashboard = dashboard;
        //
        // App Filter
        //
        new AppSelector().childOf('appSelector').registerAppChangedListener((text, value)=>{
            // update appName for dimension filters
            this._appName = value;

            // update dimensions for dashboard chart
            this.addDimension('appName', value);

            this.refreshDashboard();
        });

        //
        // dataSource --> Charts
        //
        var dataSource2Charts = new Object();
        $.each(this._dashboard.charts, (index, chartDescriptor)=>{
            var chartId = 'chart_' + index;
            chartDescriptor['id'] = chartId;

            var dataSourceName = chartDescriptor.dataSource;
            if ( dataSource2Charts[dataSourceName] == null ) {
              dataSource2Charts[dataSourceName]  = new Array();
            }
            dataSource2Charts[dataSourceName].push(chartId);
        });

        var dataSourceFilter = this._dashboard.charts[0].dataSource;

        //
        // Loaded Dimension Filter
        //
        for(var dataSourceName in dataSource2Charts) {
          this._schemaApi.getSchema(
              dataSourceName,
              (schema)=>{
                  if ( schema.name == dataSourceFilter ) {
                      // create dimension filter
                      // Note: first two dimensions MUST be app/instance
                      var filterBar = $('#filterBar');
                      for(var index = 1; index < schema.dimensionsSpec.length; index++) {
                          var dimension = schema.dimensionsSpec[index];
                          if ( !dimension.visible )
                              continue;

                          this.createDimensionFilter(filterBar, dimension.name, dimension.displayText);
                      }
                  }

                  //
                  // This should be changed in future
                  // converts dimensionSpec from Array to Map
                  //
                  var dimensionMap = new Object();
                  for(var index = 0; index < schema.dimensionsSpec.length; index++) {
                      var dimension = schema.dimensionsSpec[index];
                      dimensionMap[dimension.name] = dimension;
                  }
                  schema.dimensionsSpec = dimensionMap;

                  //
                  // This should be changed in future
                  // converts metricsSpec from Array to Map
                  //
                  var metricMap = new Object();
                  for(var index = 0; index < schema.metricsSpec.length; index++) {
                      var metric = schema.metricsSpec[index];
                      metricMap[metric.name] = metric;
                  }
                  schema.metricsSpec = metricMap;

                  //
                  // Build Transformers
                  //
                  this.createTransformers(schema);

                  // refresh dashboard after schema has been retrieved
                  // because there may be value transformers on different units
                  var charts = dataSource2Charts[schema.name];
                  $.each(charts, (index, chartId)=>{
                    this.refreshChart(chartId);
                  })
              },
              (error)=>{}
          );
        }

        //
        // Create AutoRefresher
        //
        var parent = $('#filterBarForm');
        new AutoRefresher({
            timerLength: 10
        }).childOf(parent).registerRefreshListener(()=>{
            this.refreshDashboard();
        });

        //
        // Create TimeInterval
        //
        new TimeInterval().childOf(parent).registerIntervalChangedListener((fn)=>{
            this.setInterval(fn);
        });

        $.each(dashboard.charts, (index, chart)=>{
            this.createChartComponent(index, chart);
        });
    }

    // PRIVATE
    createDimensionFilter(filterBar, dimensionName, displayText) {
        var appendedSelect = filterBar.append(`<li class="nav-item"><select style="width:150px"></select></li>`).find('select').last();
        appendedSelect.select2({
            theme: 'bootstrap4',
            allowClear: true,
            dropdownAutoWidth : true,
            placeholder: displayText,
            //TODO: must be a function to support dynamic change
            ajax: this.getDimensionAjaxOptions(this._dashboard.charts[0].dataSource, dimensionName)
       }).on('change', (event) => {
             if ( event.target.selectedIndex == null || event.target.selectedIndex < 0 ) {
                 this.rmvDimension(dimensionName);
                 return;
             }

             // get selected dimension
             var dimensionValue = event.target.selectedOptions[0].value;
             this.addDimension(dimensionName, dimensionValue);
         });
    }

    // PRIVATE
    getDimensionAjaxOptions(dataSourceName, dimensionName) {
        var filters = [{
            dimension: 'appName',
            matcher: {
                type: 'equal',
                pattern: this._appName
            }
        }];
//                for(var p = 0; p < dimensionIndex; p++) {
//                    var dim = dataSource.dimensions[p];
//                    filters.push({
//                        type: '==',
//                        dimension: dim.name,
//                        expected: {
//                            value: gEditingConditions[rowId].dimensions[dim.name].expected.value
//                        }
//                    });
//                }

        return {
            cache: true,
            type: 'POST',
            url: apiHost + '/api/datasource/dimensions',
            data: () => {
                var interval = this._intervalFn.apply();
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
                return { results: data.map(dimension => {
                    return {
                        "id": dimension.value,
                        "text": dimension.value
                    };
                }) };
            }
        }
    }

    // PRIVATE
    createChartComponent(index, chartDescriptor) {
        var chartId = chartDescriptor.id;
        var chartContainer = this.layout(chartDescriptor.id, chartDescriptor.width*3);

        var chartOption = this.getDefaultChartOption();
        chartOption.legend.data = chartDescriptor.metrics.map(metric=> { return {
            name: metric.name,
            icon: 'circle'
        }});
        if ( chartDescriptor.yAxis != null ) {
            var formatterFns = chartDescriptor.yAxis.map( y => this.getFormatter(y.unit) );
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
        if ( chartOption.yAxis.length == 1 ) {
            chartOption.grid.right = 15;
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

    //
    setInterval(intervalFn) {
        this._intervalFn = intervalFn;

        this.refreshDashboard();
    }

    // PUBLIC
    refreshDashboard() {
        if ( this._dashboard == null ) {
            return;
        }

        // refresh each chart
        for(var id in this._chartComponents) {
            this.refreshChart(id);
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
        this._dimensions[dimensionName] = {
            dimension: dimensionName,
            matcher: {
                type: 'equal',
                pattern: dimensionValue
            }
        };
        this.refreshDashboard();
    }

    rmvDimension(dimensionName) {
        delete this._dimensions[dimensionName];
        this.refreshDashboard();
    }

    refreshChart(chartId) {
        var interval = this._intervalFn.apply();

        var chartDescriptor = this._chartDescriptors[chartId];
        var chartComponent = this._chartComponents[chartId];
        chartComponent.load({
            url: apiHost + "/api/datasource/metrics",
            ajaxData: JSON.stringify({
                dataSource: chartDescriptor.dataSource,
                startTimeISO8601: interval.start,
                endTimeISO8601: interval.end,
                dimensions: this._dimensions,
                metrics: chartComponent.getOption().metrics,
            }),
            processResult: (data)=>{
                var chartDescriptor = this._chartDescriptors[chartId];

                var timeLabels = data.map(d=> moment(d.timestamp).local().format('HH:mm:ss'));

                var series = chartDescriptor.metrics.map(metric => { return {
                    name: metric.name,
                    type: metric.chartType || 'line',
                    areaStyle: { opacity: 0.3 },
                    data: data.map(d=>metric.transformer(d, metric.name)),
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

    // Unit conversion
    // PRIVATE
    createTransformers(schema) {
        $.each(this._dashboard.charts, (index, chartDescriptor) => {
            if ( chartDescriptor.dataSource == schema.name ) {
                // create transformers for those charts associated with this datasource
                $.each(chartDescriptor.metrics, (metricIndex, metric) => {
                    metric.transformer = this.createTransformer(schema, chartDescriptor, metricIndex);
                });
            }
        });
    }
    createTransformer(schema, chartDescriptor, metricIndex) {
        if ( chartDescriptor.yAxis != null ) {
            // get yAxis config for this metric
            var metricDescriptor = chartDescriptor.metrics[metricIndex];
            var metricName = metricDescriptor.name;
            var yIndex = metricDescriptor.yAxis == null ? 0 :  metricDescriptor.yAxis;
            if ( yIndex < chartDescriptor.yAxis.length ) {
                var yAxis = chartDescriptor.yAxis[yIndex];
                if ( yAxis.unit == 'millisecond' && schema.metricsSpec[metricName].unit == 'nanosecond' ) {
                    return function(data, metricName) {
                        var val = data[metricName];
                        return val == null ? 0 : (val / 1000 / 1000).toFixed(2);
                    }
                }
            }
        }
        return function(data, metricName) {
          var val = data[metricName];
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
          series: [
          ],
        };
    }

    //PRIVATE
    getFormatter(unit) {
        switch(unit) {
            case 'binary_byte': return function(v){
                return binaryByteFormat(v);
            };
            case 'percentage': return function(v) {
                return v + '%';
            };
            case 'nano2Millisecond': return function(v) {
                return (v / 1000 / 1000).toFixed(2) + 'ms';
            }
            case 'millisecond': return function(v) {
                return v + 'ms';
            }
            case 'microsecond': return function(v) {
                return v + 'µs';
            }
            default: return null;
        }
    }
}