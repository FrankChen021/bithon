class TraceListComponent {

    constructor(parent, apiOption) {
        this.vTable = $(parent).append('<table></table>').find('table');

        this.vTable.bootstrapTable({
            url: apiOption.url,
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
            formatSearch: function () {
                return 'search by Trace Id';
            },

            uniqueId: 'traceId',
            columns: [{
                field: 'traceId',
                title: 'Trace Id',
                formatter: function (value, row) {
                    return `<a target="_blank" href="/web/trace/detail?id=${row.traceId}">${value}</a>`;
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
    }

    refresh() {

    }
}