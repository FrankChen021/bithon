class Dashboard {
    constructor(containerId, dashboardName, defaultInterval) {
        this._defaultInterval = defaultInterval;

        // View
        this._container = $('#' + containerId);
        this._stackLayoutRowFill = 0;
        this._stackLayoutRow = $('<div style="display: flex"></div>');
        this._container.append(this._stackLayoutRow);
        this._timeSelector = null;
        this._timeSelectAll = false;

        // Model
        this._chartComponents = {};
        this._chartDescriptors = {};
        this._selectedInterval = null;

        // Y Axis Formatter
        this._formatters = {};
        this._formatters['binary_byte'] = (v) => v.formatBinaryByte();
        this._formatters['compact_number'] = (v) => {
            return v === undefined || v === null ? 'null' : v.formatCompactNumber();
        };
        this._formatters['percentage'] = (v) => v === 'NaN' ? '0%' : v.formatWithNoTrailingZeros(2) + '%';
        this._formatters['nanosecond'] = (v) => nanoFormat(v, 2);
        this._formatters['millisecond'] = (v) => milliFormat(v, 2);
        this._formatters['microsecond'] = (v) => microFormat(v, 2);
        this._formatters['byte_rate'] = (v) => v.formatBinaryByte() + "/s";
        this._formatters['rate'] = (v) => v.formatCompactNumber() + "/s";
        this._formatters['dateTime'] = (v) => new Date(v).format('yyyy-MM-dd hh:mm:ss');
        this._formatters['shortDateTime'] = (v) => new Date(v).format('MM-dd hh:mm:ss');
        this._formatters['timeDuration'] = (v) => v.formatTimeDuration();
        this._formatters['timeDiff'] = (v) => v.formatTimeDiff();

        // deprecated
        this._formatters['nanoFormatter'] = (v) => nanoFormat(v, 2);

        window.onpopstate = (e) => this.#onNavigateBack(e);
    }

    // PUBLIC
    allowTimeSelectAll(allowed) {
        // A view model
        this._timeSelectAll = allowed;
    }

    // PUBLIC
    load(dashboard) {
        this._dashboard = dashboard;
        //
        // Create customer filter input
        //
        if (dashboard.filter !== undefined && dashboard.filter.showFilterInput === true) {
            // Get the value from the URL
            let inputFilterExpression = window.queryParams['filter'];
            if (inputFilterExpression === undefined || inputFilterExpression === null) {
                inputFilterExpression = '';
            }

            // Insert the filter expression before the dashboard container
            const p = this._container.before( '<div class="input-group" style="padding-left: 5px; padding-right: 5px">      ' +
                                    '     <div class="input-group-prepend">                                       ' +
                                    '         <span class="input-group-text rounded-0" id="filter-input-span">Filter&nbsp;<i id="tooltip" class="far fa-question-circle"></i></span> ' +
                                    '     </div>                                                                  ' +
                                    '    <input type="text"                                                       ' +
                                    '           class="form-control"                                              ' +
                                    '           id="filter-input" placeholder="SQL style filter expression, hover your mouse on the question mark to learn more. Press Enter once input complete."' +
                                    '           aria-describedby="filter-input-span"/>                           ' +
                                    ' </div>')
                                    .parent();

            p.find('#filter-input')
            .val(inputFilterExpression)
            .on('keydown', (event) => {
                if (event.keyCode === 13) {
                    this.#updatePageURL();

                    this.refreshDashboard();
                }
            });

            p.find('#tooltip').popover({
                placement: 'bottom',
                html: true,
                trigger: 'hover',
                title: 'Syntax',
                content: '<b>Comparators</b>: &lt;, &lt;=, &gt, &gt;=, =, !=, in, not in, contains, not contains, startsWith, endsWith, not startsWith, not endsWith<br/>' +
                       '<b>Operators</b>: AND, OR, NOT<br/>' +
                       '<b>Functions</b>: hasToken(field, \'xxx\')<br/><br/>' +
                       '<b>Example 1</b>: <u>level in (\'DEBUG\', \'INFO\')</u><br/>' +
                       '<b>Example 2</b>: <u>message contains \'start\' AND level = \'INFO\'</u><br/>' +
                       '<b>Example 3</b>: <u>message contains \'start\' AND message contains \'complete\'</u><br/>'
            });
        }

        //
        // Create App Filter
        // The 'appSelector' element is defined in app-layout.html
        //
        this.vFilter = new AppSelector({
            parentId: 'filterBar',
            intervalProvider: () => this.getSelectedTimeInterval(),
        }).registerChangedListener((name, value) => {
            if (name === 'application') {
                g_SelectedApp = value;

                // Reset the instanceName
                this.vFilter.resetFilter('instanceName');
            }

            // TODO: reset all filters after this one

            this.refreshDashboard();

            this.#updatePageURL();
        });

        //
        // Set up id
        //
        $.each(this._dashboard.charts, (index, chartDescriptor) => {
            chartDescriptor['id'] = 'chart_' + index;
        });

        const parent = $('#filterBarForm');

        //
        // Create TimeSpanSelector
        //
        const intervalList = dashboard.filter === undefined || dashboard.filter.interval === undefined ? null : dashboard.filter.interval.list;
        this._timeSelector = new TimeSpanSelector(this._defaultInterval, this._timeSelectAll, intervalList)
                                .childOf(parent)
                                .registerIntervalChangedListener((selectedModel) => {
            g_MetricSelectedInterval = selectedModel.id;
            this._selectedInterval = {
                id: selectedModel.id,
                start: selectedModel.start,
                end: selectedModel.end
            };

            this.refreshDashboard();
            this.#updatePageURL();
        });
        this._selectedInterval = this._timeSelector.getInterval();

        //
        // Create AutoRefresher by default
        // the filterBarForm is defined in the app-layout.html
        //
        const allowAutoRefresh = dashboard.filter === undefined
         || dashboard.filter.interval === undefined
         || dashboard.filter.interval.allowAutoRefresh === undefined
         || dashboard.filter.interval.allowAutoRefresh;
        new AutoRefresher({
            timerLength: 10,
            allowAutoRefresh: allowAutoRefresh
        }).childOf(parent).registerRefreshListener(() => {
            this._selectedInterval = this._timeSelector.getInterval();
            this.refreshDashboard();
        });

        $.each(dashboard.charts, (index, chartDescriptor) => {

            this.layout(chartDescriptor.id, chartDescriptor.width * 3);

            // create chart
            this.createChartComponent(chartDescriptor.id, chartDescriptor)
                .setOpenHandler(() => {
                    this.openChart(chartDescriptor.id);
                });

            this._chartDescriptors[chartDescriptor.id] = chartDescriptor;

            this.createChartDetail(chartDescriptor);
        });

        //
        // Connect the charts to show tooltip synchronize
        //
        const charts = [];
        for (const id in this._chartComponents) {
            try {
                const chartInstance = this._chartComponents[id].getEchartInstance();
                charts.push(chartInstance);

                // Ignore brush event for connection
                chartInstance.on('brushEnd', (params) => params.escapeConnect = true);
                chartInstance.on('brushSelected', (params) => params.escapeConnect = true);
            } catch (ignored) {
                // this chart component might be TableComponent
            }
        }
        echarts.connect(charts);

        //
        // Loaded Dimension Filter
        // This is a legacy implementation.
        // Should be refactored to decouple the filter from the dataSource field
        //
        const filterSpecs = [];
        if (dashboard.filter !== undefined) {
            $.each(dashboard.filter.selectors, (index, selector) => {
                if (selector.type === 'datasource') {
                    $.each(selector.fields, (fieldIndex, field) => {
                        let name;
                        let alias;
                        let displayText;
                        let width;
                        let allowClear = true;
                        let allowEdit = true;
                        let defaultValue = '';
                        if (typeof field === 'object') {
                            name = field.name;
                            alias = field.alias === undefined ? name : field.alias;
                            displayText = field.displayText === undefined ? name : field.displayText;
                            width = field.width === undefined ? 150 : field.width;
                            allowClear = field.allowClear === undefined ? true : field.allowClear;
                            allowEdit = field.allowEdit === undefined ? true : field.allowEdit;
                            defaultValue = field.defaultValue === undefined ? '' : field.defaultValue;
                        } else {
                            name = field;
                            alias = field;
                            displayText = field;
                            width = 150;
                        }
                        filterSpecs.push({
                            filterType: 'select',
                            sourceType: 'datasource',
                            source: selector.name,
                            name: name,
                            alias: alias,
                            displayText: displayText,
                            defaultValue: defaultValue,
                            width: width,
                            filterExpression: field.filterExpression,
                            allowClear: allowClear,
                            allowEdit: allowEdit,
                            onPreviousFilters: true
                        });
                    });
                }
            });
            this.vFilter.createFilters(filterSpecs);
        }

        this.refreshDashboard();
    }

    #updatePageURL() {
        let url = window.location.pathname + '?';

        const filters = this.vFilter.getSelectedFilters();
        $.each(filters, (index, filter) => {
            url += `${filter.field}=${filter.expected}&`;
        });
        url += `interval=${this._selectedInterval.id}&`;

        const inputFilterExpression = this.#getInputFilterExpression();
        if (inputFilterExpression.length > 0) {
            url += `filter=${encodeURIComponent(inputFilterExpression)}`;
        }

        window.history.pushState('updated' /*state*/, '', url);
    }

    createChartDetail(chartDescriptor) {
        // create detail view for this chart
        if (chartDescriptor.details == null) {
            return;
        }

        const detailViewId = chartDescriptor.id + '_detailView';
        if (chartDescriptor.details.query === undefined) {
            if (chartDescriptor.details.columns !== undefined) {
                const pair = this.#fromDetailV2(chartDescriptor);
                chartDescriptor.details.query.fields = pair[0];
                chartDescriptor.details.columns = pair[1];
            } else {
                const pair = this.#fromDetailV1(chartDescriptor);
                chartDescriptor.details.query.fields = pair[0];
                chartDescriptor.details.columns = pair[1];
            }

            chartDescriptor.details.query.order = chartDescriptor.details.orderBy;

            if (chartDescriptor.details.groupBy !== undefined && chartDescriptor.details.groupBy.length > 0) {
                chartDescriptor.details.query.type = "groupBy";
            } else {
                chartDescriptor.details.query.type = "list";
                chartDescriptor.details.pagination = [25, 50, 100];
            }
        }

        //
        // Use the format definition in the chart as default
        //
        for (let i = 0; i < chartDescriptor.details.columns.length; i++) {
            let column = chartDescriptor.details.columns[i];
            if (column.format !== undefined) {
                continue;
            }

            let columnName;
            if (typeof column === 'string') {
                columnName = column;
            } else {
                columnName = column.name;
            }

            const chartColumn = chartDescriptor.columnMap[columnName];
            if (chartColumn !== undefined && chartColumn.format !== undefined) {
                if (typeof column === 'string') {
                    chartDescriptor.details.columns[i] = {name: columnName};
                    column = chartDescriptor.details.columns[i];
                }
                column.format = chartColumn.format;
            } else {
                // this column might be a dimension
            }
        }
        if (chartDescriptor.details.query.dataSource === undefined) {
            chartDescriptor.details.query.dataSource = chartDescriptor.query.dataSource !== undefined ? chartDescriptor.query.dataSource : chartDescriptor.dataSource;
        }

        let columnButtons = undefined;
        if (chartDescriptor.details.tracing !== undefined) {
            columnButtons = [{
                title: "Tracing Log",
                text: "Search...",
                onClick: (index, row, start, end) => this.#openTraceSearchPage(chartDescriptor, start, end, row)
            }]
        }

        const chartComponent = this._chartComponents[chartDescriptor.id];

        // Create detailed component
        const detailTableComponent = this.createTableComponent(detailViewId,
            chartComponent.getUIContainer(),
            chartDescriptor.details,
            // Insert index column
            true,
            columnButtons,
            {
                minimize: false,
                close: true
            }
        )

        // Bind event on parent component
        chartComponent.setSelectionHandler(
            (chartOption, startIndex, endIndex) => {
                // get the time range
                const interval = chartComponent.getInterval(startIndex, endIndex);

                const startISO8601 = moment(interval.start).utc().toISOString();
                const endISO8601 = moment(interval.end).utc().toISOString();

                this.refreshTable(chartDescriptor.details.query,
                    detailTableComponent,
                    {
                        start: startISO8601,
                        end: endISO8601
                    },
                    true
                    );
            },
            () => {
                detailTableComponent.clear();
                detailTableComponent.show();
            },
            () => {
                detailTableComponent.hide();
            },
            () => {
                detailTableComponent.clear();
            });
    }

    #fromDetailV2(chartDescriptor) {

        const tableColumns = chartDescriptor.details.columns.map((column) => {
            let columnName;
            if (typeof column === 'object') {
                columnName = column.name;
            } else {
                columnName = column;
            }

            return {
                name: columnName,
                format: column.formatter,
                sortable: column.sortable | true
            };
        });

        const tableFields = [];
        chartDescriptor.details.groupBy.forEach((groupBy) => tableFields.push(groupBy));
        chartDescriptor.details.aggregators.forEach((aggregator) => tableFields.push({
            name: aggregator.name,
            field: aggregator.field,
            aggregator: aggregator.type
        }));

        return [tableFields, tableColumns];
    }

    // create columns from groupBy/metrics/
    #fromDetailV1(chartDescriptor) {
        const fields = []
        const columns = [];

        $.each(chartDescriptor.details.dimensions, (index, dimension) => {
            fields.push(dimension.name);
            columns.push({
                name: dimension.name,
                format: dimension.formatter,

                // a parameter from template formatter
                template: dimension.template,

                sortable: dimension.sortable | true
            });
        });

        $.each(chartDescriptor.details.groupBy, (index, groupBy) => {
            if (typeof groupBy === 'object') {
                fields.push(groupBy.name);
                columns.push({name: groupBy.name, format: groupBy.formatter, sortable: true});
            } else {
                fields.push(groupBy);
                columns.push({name: groupBy, sortable: true});
            }
        });

        //
        // create columns for metrics
        //
        $.each(chartDescriptor.details.metrics, (index, metric) => {
            if (typeof metric === 'object') {
                fields.push(metric.name);
                columns.push({name: metric.name, format: metric.formatter, sortable: true});
            } else {
                fields.push(metric);
                columns.push({name: metric, sortable: true});
            }
        });

        return [fields, columns];
    }

    /**
     * Tracing Spec Example On Dashboard
     *
     *  "tracing": {
     *    "mappings": {
     *      "cluster": "tags.clickhouse.cluster",
     *      "user": "tags.clickhouse.user",
     *      "queryType": "tags.clickhouse.queryType",
     *      "exceptionCode": {
     *        "type" : "switch",
     *        "cases" : {
     *          "-400": ["status", "400"],
     *          "-404": ["status", "404"],
     *          "-500": ["status", "500"],
     *          "-504": ["status", "504"],
     *          "0":    ["status", "200"],
     *          "default": "tags.clickhouse.exceptionCode"
     *        }
     *      },
     *      "filter": "name = 'httpclient' AND kind = 'CLIENT'"
     *    }
     *  }
     */
    #openTraceSearchPage(chartDescriptor, startTime, endTime, row) {
        let filterExpression = '';

        if (g_SelectedApp !== undefined && g_SelectedApp !== null) {
            filterExpression += `appName = '${g_SelectedApp}' `;
        }

        const instanceFilter = this.vFilter.getSelectedFilter("instanceName");
        if (instanceFilter != null) {
            filterExpression += `AND instanceName = '${instanceFilter.expected}' `;
        }

        //
        // Get filter expression from current row and pattern in the 'tracing' definition
        //
        const tracingSpec = chartDescriptor.details.tracing;
        const columns = chartDescriptor.details.groupBy === undefined ?
            chartDescriptor.details.columns
            // groupBy is a legacy property
            : chartDescriptor.details.groupBy;

        // dimensionMaps is the old name, use it as compatibility
        const mappings = tracingSpec.mappings === undefined ? tracingSpec.dimensionMaps : tracingSpec.mappings;
        $.each(columns, (index, col) => {
            if (typeof col === "object") {
                col = col.name;
            }
            const mappingField = mappings[col];
            if (mappingField == null) {
                return;
            }

            const val = row[col];
            if (typeof mappingField === "string") {
                filterExpression += `AND ${mappingField} = '${val}' `;
            } else {
                // the mapping is an object
                const val = row[col];
                if (mappingField.type === 'switch') { // currently, only switch is supported
                    let f = mappingField.cases[val];
                    if (f == null) {
                        // case not found, use the default case
                        f = mappingField.cases.default;
                    }
                    const fType = $.type(f);
                    if (fType === 'array') {
                        // f is a pair, f[0] is the field name, f[1] is the value
                        if (f[1] != null && f[1] !== '') {
                            filterExpression += `AND ${f[0]} = '${f[1]}' `;
                        }
                    } else if (fType === 'string') {
                        filterExpression += `AND ${f} = '${val}' `;
                    }
                } else {
                    console.log(`mapping type ${mappingField.type} is not supported yet.`);
                }
            }
        });

        if (tracingSpec.filter !== undefined) {
            filterExpression += `AND ${tracingSpec.filter} `;
        }
        if (filterExpression.startsWith('AND ')) {
            filterExpression = filterExpression.substring('AND '.length)
        }

        window.open(`/web/trace/search?interval=c:${startTime}/${endTime}&filter=${encodeURIComponent(filterExpression)}`);
    }

    createTableComponent(chartId,
                         parentElement,
                         tableDescriptor,
                         insertIndexColumn,
                         columnButtons,
                         toolbar) {

        //
        // type of string column is allowed in definition, convert it to object first to simply further processing
        //
        for (let i = 0, size = tableDescriptor.columns.length; i < size; i++) {
            let column = tableDescriptor.columns[i];

            // string type of column is allowed for simple configuration
            // during rendering, it's turned into an object for simple processing
            if (typeof column === 'string') {
                tableDescriptor.columns[i] = {name: column};
            }
        }

        const lookup = tableDescriptor.lookup;
        const tableColumns = tableDescriptor.columns.map((column) => {

            const tableColumn = typeof column === 'object' ? Object.assign({
                    field: column.name,
                    title: column.title || column.name
                }, column)
                : {
                    title: column,
                    field: column
                };

            // handle lookup
            let lookupFn = null;
            if (lookup !== undefined) {
                const fieldLookupTable = lookup[tableColumn.field];
                if (fieldLookupTable !== undefined) {
                    lookupFn = (val) => {
                        const mapped = fieldLookupTable[val];
                        return mapped != null ? mapped + '(' + val + ')' : val;
                    }
                }
            }

            // handle format
            const formatterFn = this._formatters[column.format];
            if (lookupFn != null || formatterFn !== undefined) {
                tableColumn.formatter = (v) => {
                    if (lookupFn != null) {
                        v = lookupFn(v);
                    }
                    if (formatterFn !== undefined) {
                        v = formatterFn(v);
                    }
                    return v;
                };
            }

            // Default to sortable
            if (tableColumn.sortable === undefined) {
                tableColumn.sortable = true;
            }

            return tableColumn;
        });

        if (insertIndexColumn) {
            tableColumns.unshift({
                field: 'id',
                title: 'No.',
                align: 'center',
                width: 20,
                formatter: (cell, row, index) => {
                    return (index + 1);
                }
            });
        }

        const vTable = new TableComponent({
                tableId: chartId + "_table",
                parent: parentElement,
                columns: tableColumns,

                // If the table has pagination OR the table query defines the LIMIT, the SORT should be done at the server side
                serverSort: (tableDescriptor.pagination !== undefined) || (tableDescriptor.query !== undefined && tableDescriptor.query.limit !== undefined),

                pagination: tableDescriptor.pagination,
                detailView: false,
                toolbar: Object.assign({
                    showColumns: tableDescriptor.showColumns,
                    ...tableDescriptor.toolbar
                }, toolbar),
                buttons: columnButtons,

                stickyHeader: tableDescriptor.stickyHeader,

                // default order
                order: tableDescriptor.query.orderBy?.order,
                orderBy: tableDescriptor.query.orderBy?.name,
            }
        );
        if (tableDescriptor.title !== undefined && tableDescriptor.title !== null && tableDescriptor.title !== '') {
            vTable.header('<b>' + tableDescriptor.title + '</b>');
        }

        this._chartComponents[chartId] = vTable;

        return vTable;
    }

    createLineComponent(chartId, chartDescriptor) {
        const chartOption = this.getDefaultChartOption();

        // runtime properties
        chartDescriptor.columnMap = {};

        if (chartDescriptor.yAxis === undefined) {
            chartDescriptor.yAxis = [];
        }
        for (let i = 0, size = chartDescriptor.columns.length; i < size; i++) {
            let column = chartDescriptor.columns[i];

            // string type of column is allowed for simple configuration
            // during rendering, it's turned into an object for simple processing
            if (typeof column === 'string') {
                chartDescriptor.columns[i] = {name: column};
                column = chartDescriptor.columns[i];
            }

            // Set up a map
            chartDescriptor.columnMap[column.name] = column;

            // legend
            chartOption.legend.data.push({
                name: column.title || column.name,
                icon: 'circle'
            });

            // formatter
            const yAxisIndex = column.yAxis || 0;
            // Make sure the array has enough objects for further access
            while (chartDescriptor.yAxis.length < yAxisIndex + 1) {
                chartDescriptor.yAxis.push({});
            }
            const yAxis = chartDescriptor.yAxis[yAxisIndex];
            if (yAxis.format === undefined) {
                yAxis.format = column.format === undefined ? 'compact_number' : column.format;
            }

            if (yAxis.formatter === undefined) {
                yAxis.formatter = this.getFormatter(yAxis.format);
            }
        }

        chartOption.yAxis = chartDescriptor.yAxis.map((yAxis) => {
            return {
                type: 'value',
                min: 0 || yAxis.min,
                minInterval: 1 || yAxis.minInterval,
                interval: yAxis.interval,
                inverse: yAxis.inverse === undefined ? false : yAxis.inverse,
                splitLine: {show: true},
                axisLine: {show: false},
                scale: false,
                axisTick: {
                    show: false,
                },
                axisLabel: {
                    formatter: yAxis.formatter
                },
            }
        });

        chartOption.tooltip.formatter = series => {
            const currentChartOption = this.getChartCurrentOption(chartId);
            const currentChartComponent = this._chartComponents[chartId];

            const dataIndex = series[0].dataIndex;
            const interval = currentChartComponent.getInterval(dataIndex);
            const format = currentChartComponent.isWithinOneDay() ? 'HH:mm:ss' : 'MM-DD HH:mm:ss';
            let tooltip = moment(interval.start).local().format(format) + '<br/>' + moment(interval.end).local().format(format);
            series.forEach(s => {
                //Use the yAxis defined formatter to format the data
                const yAxisIndex = currentChartOption.series[s.seriesIndex].yAxisIndex;

                let formatterFn = currentChartOption.yAxis[yAxisIndex].axisLabel.formatter;

                const text = formatterFn(s.data);

                //Concat the tooltip
                //marker can be seen as the style of legend of this series
                tooltip += `<br />${s.marker}${s.seriesName}: ${text}`;
            });
            return tooltip;
        };
        if (chartOption.yAxis.length === 1) {
            chartOption.grid.right = 15;
        }
        if (chartDescriptor.details != null) {
            chartOption.brush = {
                xAxisIndex: 'all',
                brushLink: 'all',
                outOfBrush: {
                    colorAlpha: 0.1
                }
            };
            chartOption.toolbox = {
                // the toolbox is disabled because the ChartComponent takes over the functionalities
                show: false
            };
        }
        const chartComponent = new ChartComponent({
            containerId: chartId,

            // Keep the query object in the chart for further queries
            query: chartDescriptor.query
        }).header('<b>' + chartDescriptor.title + '</b>')
            .setChartOption(chartOption);

        // Apply click event to refresh the time interval
        if (chartDescriptor.zoomOnTime === true) {
            chartComponent.setClickHandler((e) => {
                const interval = chartComponent.getInterval(e.dataIndex);
                this._timeSelector.setInterval(interval.start, interval.end);
            });
        }

        this._chartComponents[chartId] = chartComponent;

        return chartComponent;
    }

    refreshTable(query, tableComponent, interval, showInterval) {
        const thisQuery = Object.assign({}, query);
        if (thisQuery.interval === undefined) {
            thisQuery.interval = {};
        }
        thisQuery.interval.startISO8601 = interval.start;
        thisQuery.interval.endISO8601 = interval.end;
        thisQuery.filterExpression = String.join(' AND ',
            this.vFilter.getSelectedFilterExpression(),
            this.#getInputFilterExpression(),
            thisQuery.filter);
        delete thisQuery.filter;

        let path;
        if (query.type === 'list') {
            path = '/api/datasource/list/v2';
        } else {
            path = '/api/datasource/groupBy/v3';
        }
        const loadOptions = {
            url: apiHost + path,
            ajaxData: thisQuery,
            responseHandler: (res) => {
                return {
                    total: res.total,
                    rows: res.data
                }
            },
            showInterval: showInterval
        };
        tableComponent.load(loadOptions);
    }

    // PRIVATE
    createChartComponent(chartId, chartDescriptor) {
        if (chartDescriptor.type === 'table') {
            return this.createTableComponent(chartId, $('#' + chartId), chartDescriptor);
        }

        // Process legacy list type
        if (chartDescriptor.type === 'list') {
            // Convert an old format to new format
            const query = {
                type: 'list',
                dataSource: chartDescriptor.dataSource,
                filters: chartDescriptor.filters
            };
            query.fields = chartDescriptor.columns.map((column) => {
                if (typeof column === 'object') {
                    return column.field;
                }
                return column;
            });
            chartDescriptor.query = query;
            chartDescriptor.pagination = [25, 50, 100];
            chartDescriptor.type = 'table';

            return this.createTableComponent(chartId, $('#' + chartId), chartDescriptor);
        }
        if (chartDescriptor.type === 'line') {
            return this.createLineComponent(chartId, chartDescriptor);
        }

        //
        // Old timeseries chart format, convert it to the latest
        //
        const query = {
            type: 'timeseries',
            dataSource: chartDescriptor.dataSource,
            fields: []
        };

        const columns = [];
        chartDescriptor.metrics.forEach((metric) => {
            query.fields.push(metric.name);

            const column = {
                name: metric.name,
                format: metric.format
            };
            const yAxisIndex = metric.yAxis || 0;
            if (chartDescriptor.yAxis !== undefined) {
                const yAxis = chartDescriptor.yAxis[yAxisIndex];
                if (yAxis.format !== undefined) {
                    // Don't override the format set above
                    column.format = yAxis.format;
                }
                column.yAxis = yAxisIndex;
            }
            if (metric.chartType !== undefined) {
                column.chartType = metric.chartType;
            }
            if (metric.fill !== undefined) {
                column.fill = metric.fill;
            }
            if (metric.displayName !== undefined) {
                column.title = metric.displayName;
            }
            if (metric.selected !== undefined) {
                column.selected = metric.selected;
            }
            columns.push(column);
        });
        delete chartDescriptor.metrics;

        // predefined filter
        if (chartDescriptor.dimensions !== undefined) {
            query.filters = [];
            $.each(chartDescriptor.dimensions, (name, value) => {
                query.filters.push(value);
            });

            delete chartDescriptor.dimensions;
        }

        if (chartDescriptor.groupBy !== undefined) {
            chartDescriptor.groupBy.forEach((groupBy) => query.fields.unshift(groupBy));
            delete chartDescriptor.groupBy;
        }

        chartDescriptor.columns = columns;
        chartDescriptor.query = query;
        chartDescriptor.type = 'line';
        console.log(JSON.stringify(chartDescriptor, null, 4));
        return this.createLineComponent(chartId, chartDescriptor);
    }

    // PRIVATE
    layout(id, width) {
        if (this._stackLayoutRowFill + width > 12) {
            // create a new row
            this._stackLayoutRowFill = 0;
            this._stackLayoutRow = $('<div style="display: flex"></div>');
            this._container.append(this._stackLayoutRow);
        }
        this._stackLayoutRowFill += width;

        return this._stackLayoutRow.append(`<div class="form-group col-md-${width}" id="${id}" style="margin-bottom: 0;padding-bottom: 10px; padding-left: 1px; padding-right: 1px"></div>`);
    }

    // PUBLIC
    refreshDashboard() {
        if (this._dashboard == null) {
            return;
        }

        // refresh each chart
        const interval = this.getSelectedTimeInterval();
        for (const id in this._chartComponents) {
            const chartComponent = this._chartComponents[id];

            const chartDescriptor = this._chartDescriptors[id];
            if (chartDescriptor !== undefined) {
                // detail view has no root descriptor, do not refresh
                // this can be fixed to allow refresh the detail view if it's shown
                this.refreshChart(chartDescriptor, chartComponent, interval);
            }
        }
    }

    refreshLine(chartDescriptor, chartComponent, interval, metricNamePrefix, mode) {
        if (metricNamePrefix === undefined) {
            metricNamePrefix = '';
        }

        const thisQuery = Object.assign({}, chartDescriptor.query);
        if (thisQuery.dataSource === undefined) {
            thisQuery.dataSource = chartDescriptor.dataSource;
        }
        if (thisQuery.interval === undefined) {
            thisQuery.interval = {};
        }
        thisQuery.interval.startISO8601 = interval.start;
        thisQuery.interval.endISO8601 = interval.end;
        if (chartDescriptor.query.bucketCount !== undefined && chartDescriptor.query.bucketCount != null) {
            thisQuery.interval.bucketCount = chartDescriptor.query.bucketCount;
        }
        thisQuery.filterExpression = String.join(' AND ',
            this.vFilter.getSelectedFilterExpression(),
            this.#getInputFilterExpression(),
            thisQuery.filter);
        delete thisQuery.filter;

        const queryFieldsCount = chartDescriptor.query.fields.length;

        chartComponent.load({
            url: apiHost + "/api/datasource/timeseries/v4",
            ajaxData: JSON.stringify(thisQuery),
            processResult: (data) => {
                const timeLabels = [];
                for (let t = data.startTimestamp; t <= data.endTimestamp; t += data.interval) {
                    timeLabels.push(moment(t).local().format('HH:mm:ss'));
                }

                const series = [];
                $.each(data.data, (index, metric) => {
                    let metricName = metric.tags[metric.tags.length - 1];

                    let column = chartDescriptor.columnMap[metricName];
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

                    const n = metricNamePrefix + group + (column.title === undefined ? column.name : column.title);

                    //Use the yAxis defined formatter to format the data
                    const yAxisIndex = column.yAxis || 0;

                    let s = {
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
                                if (v.value > 0) {
                                    const yAxis = chartDescriptor.yAxis[yAxisIndex];
                                    return yAxis.formatter(v.value);
                                } else {
                                    return '';
                                }
                            }
                        },

                        // selected is not a property of series
                        // this is used to render default selected state of legend by chart-component
                        selected: column.selected === undefined ? true : column.selected
                    };
                    series.push(s);
                });

                // a groupBy query might return empty data
                if (series.length === 0) {
                    const count = (data.endTimestamp - data.startTimestamp) / data.interval;
                    series.push({
                        id: 'empty',
                        name: 'empty',
                        type: 'line',
                        data: new Array(count).fill(0),
                        yAxisIndex: 0,
                        areaStyle: {opacity: 0.3},
                        lineStyle: {width: 1},
                        itemStyle: {opacity: 0},
                        selected: true
                    });
                }

                if (mode === undefined) {
                    if (data.data.length === 0) {
                        // no data returned
                        mode = 'replace';
                    } else {
                        // If the returned count of series is less than the given fields count, that's a group-by.
                        // In this case, we always replace the series because one group may not exist in the following queries.
                        mode = data.data.length < queryFieldsCount ? 'replace' : 'refresh'
                    }
                }

                const roundDownStart = data.startTimestamp;
                const bucketLength = data.interval; // in millisecond
                const absoluteStart = moment(interval.start).valueOf();
                const absoluteEnd = moment(interval.end).valueOf();

                // save the timestamp for further processing
                chartComponent.getInterval = (startIndex, endIndex) => {
                    if (endIndex === undefined) {
                        endIndex = startIndex + 1;
                    }

                    const startTimestamp = roundDownStart + startIndex * bucketLength;
                    const endTimestamp = roundDownStart + endIndex * bucketLength;
                    return {
                        start: startTimestamp < absoluteStart ? absoluteStart : startTimestamp,
                        end: endTimestamp > absoluteEnd ? absoluteEnd : endTimestamp
                    }
                };

                // endTimestamp is exclusive, so we use (endTimestamp-1) to get the inclusive date
                const inOneDay = moment(data.startTimestamp).format('yyyy-MM-DD') === moment(data.endTimestamp - 1).format('yyyy-MM-DD');
                chartComponent.isWithinOneDay = () => inOneDay;

                return {
                    refreshMode: mode,

                    xAxis: {
                        data: timeLabels
                    },
                    series: series
                }
            }
        });
    }

    refreshChart(chartDescriptor, chartComponent, interval, metricNamePrefix, mode) {
        // Check if the filter satisfies the requirement of this chart
        const query = chartDescriptor.query;
        if (query.precondition !== undefined
            && query.precondition.filters !== undefined) {
            for (let i = 0; i < query.precondition.filters.length; i++) {
                const filter = query.precondition.filters[i];
                if (this.vFilter.getSelectedFilter(filter) == null) {
                    chartComponent.showHint(`Select ${filter} to load`);
                    return;
                }
            }
        }

        if (chartDescriptor.type === 'table') {
            this.refreshTable(chartDescriptor.query, chartComponent, interval);
            return;
        }
        if (chartDescriptor.type === 'line') {
            this.refreshLine(chartDescriptor, chartComponent, interval, metricNamePrefix, mode);
            return;
        }

        console.log('Unknown chart type: ' + chartDescriptor.type);
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
            axisPointer: {
                link: [
                    {
                        xAxisIndex: 'all'
                    }
                ],
                label: {
                    backgroundColor: '#777'
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
                left: 60,
                right: 60,
                bottom: 20,
                top: 40,
            },
            xAxis: {
                type: 'category',
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
        let baseline = [];
        $.ajax({
            type: 'POST',
            url: apiHost + "/api/metric/baseline/get",
            data: JSON.stringify({}),
            async: false,
            dataType: "json",
            contentType: "application/json",
            success: (data) => {
                baseline = data;
            },
            error: (data) => {
            }
        });

        const chartDescriptor = this._chartDescriptors[chartId];
        let dialogContent =
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
            '               <a class="dropdown-item btn-compare-add" href="#" data-id="0" data-value="0">today</a>' +
            '               <a class="dropdown-item btn-compare-add" href="#" data-id="1" data-value="1">-1d</a>' +
            '               <a class="dropdown-item btn-compare-add" href="#" data-id="3" data-value="3">-3d</a>     ' +
            '               <a class="dropdown-item btn-compare-add" href="#" data-id="7" data-value="7">-7d</a>     ' +
            ' {baseline}' +
            '           </div>' +
            '       </div>' +
            '       <div id="compare_charts" style="padding-top:5px;height:470px;width:100%"></div>' +
            '   </div>' +
            '</div>';

        let baselineDropdown = '';
        $.each(baseline, (index, l) => {
            baselineDropdown += `<a class='dropdown-item btn-compare-add' href='#' data-id='${l}' data-date='${l}' data-absolute='true'>${l}</a>`;
        })
        dialogContent = dialogContent.replace('{baseline}', baselineDropdown);

        bootbox.dialog({
            centerVertical: true,
            size: 'xl',
            onEscape: true,
            backdrop: true,
            message: dialogContent,
            onShown: () => {
                const latestCharts = this.createChartComponent('latest_charts', chartDescriptor).height('400px');
                const compareChart = this.createChartComponent('compare_charts', chartDescriptor).height('400px');

                $('a[data-toggle="tab"]').on('shown.bs.tab', () => {
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
                    const removeButtonId = 'btn-popup-compare-' + $(e.target).attr('data-id');
                    const removeButtonContainer = $('#btn-remove-buttons');
                    if (removeButtonContainer.find('#' + removeButtonId).length > 0) {
                        return;
                    }

                    const day = $(e.target).attr('data-value');

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
                    removeButtonContainer.append(removeButton);

                    let start, end;
                    if ($(e.target).attr('data-absolute') === 'true') {
                        const date = moment($(e.target).attr('data-date'));
                        start = date.toISOString(true);
                        end = date.add(1, 'day').toISOString(true);
                    } else {
                        const todayStart = moment().startOf('day');
                        const baseStart = todayStart.clone().subtract(day, 'day');
                        const baseEnd = baseStart.clone().add(1, 'day');
                        start = baseStart.toISOString(true);
                        end = baseEnd.toISOString(true);
                    }
                    this.refreshChart(chartDescriptor,
                        compareChart,
                        {
                            start: start,
                            end: end
                        },
                        text + '-',
                        'add');
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
        return this._selectedInterval;
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

    #getInputFilterExpression() {
        const v = $('#filter-input').val();
        return v === undefined || v === null ? '' : v.trim();
    }

    #setInputFilterExpression(val) {
        $('#filter-input').val(val);
    }

    #onNavigateBack(e) {
        if (e.state === null) {
            // We set the state manually, so if the state is null, it's triggered by another source that needs to be ignored
            return;
        }

        // For simply implementation, we reload the page to let the page re-create to load all filters
        // In the future we can reset filters only as shown above
        //        // Update the global parameters first
        //        window.queryParams = toQueryParameters(window.location.href);
        //
        //        //
        //        this._timeSelector.setIntervalById(window.queryParams['interval']);
        //        this.vFilter.resetToFilters(window.queryParams);
        //        this.#setInputFilterExpression(window.queryParams['filter']);
        //
        //        // Refresh the dashboard at last
        //        this.refreshDashboard();
        window.location.reload();
    }
}
