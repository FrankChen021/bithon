class TraceListComponent {

    constructor(parent, apiOption) {
        this.vTable = $(parent).append('<table></table>').find('table');

        this.vTable.bootstrapTable({
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
            // sortName: 'startTime',
            // sortOrder: 'desc',

            stickyHeader: true,
            stickyHeaderOffsetLeft: parseInt($('body').css('padding-left'), 10),
            stickyHeaderOffsetRight: parseInt($('body').css('padding-right'), 10),
            theadClasses: 'thead-light',

            queryParamsType: '',
            queryParams: (params) => {
                let queryParams = apiOption.getQueryParams(params);
                queryParams = $.extend(queryParams, {
                    pageSize: params.pageSize,
                    pageNumber: params.pageNumber - 1,
                    orderBy: params.sortName,
                    order: params.sortOrder
                });
                return queryParams;
            },

            filterControl: false,
            filterShowClear: false,
            search: false,
            showSearchClearButton: false,
            searchOnEnterKey: true,

            uniqueId: 'traceId',
            columns: [{
                field: 'traceId',
                title: 'Trace Id',
                formatter: function (value, row) {
                    return `<a target="_blank" href="/web/trace/detail?id=${row.traceId}&type=trace">${value}</a>`;
                },
            }, {
                field: 'appName',
                title: 'Application',
                visible: apiOption.showApplicationName === undefined ? false : apiOption.showApplicationName
            }, {
                field: 'instanceName',
                title: 'Instance'
            }, {
                field: 'startTime',
                title: 'Time',
                formatter: function (value) {
                    return new Date(value / 1000).format('yyyy-MM-dd hh:mm:ss.S');
                },
                sortable: true
            }, {
                field: 'costTime',
                title: 'Duration',
                formatter: function (value) {
                    return nanoFormat(value * 1000);
                },
                sortable: true
            }, {
                field: 'tags',
                title: 'URL',
                formatter: function (value) {
                    return value['uri'] || value['http.uri'];
                }
            }, {
                field: 'status',
                title: 'Status'
            }],

            rowStyle: (row, index) => {
                if (row.status !== "200") {
                    return {
                        classes: 'alert-warning'
                    }
                }
                return {};
            }
        });
    }

    refresh() {
        this.vTable.bootstrapTable('refresh');
    }
}