class EventPage {
    constructor(appName) {
        // Model
        this.mApplication = appName;
        this.mInterval = null;

        // View
        new AppSelector(this.mApplication).childOf('appSelector').registerAppChangedListener((text, value) => {
            window.location = `/web/app/event/${value}`;
        });

        const parent = $('#filterBarForm');

        // View - Refresh Button
        parent.append('<button class="btn btn-outline-secondary" style="border-radius:0px;border-color: #ced4da" type="button"><i class="fas fa-sync-alt"></i></button>')
            .find("button").click(() => {
            this.#refreshPage();
        });

        // View
        this.vIntervalSelector = new TimeInterval('today').childOf(parent).registerIntervalChangedListener((selectedModel) => {
            this.mInterval = this.vIntervalSelector.getInterval();
            this.#refreshPage();
        });
        this.mInterval = this.vIntervalSelector.getInterval();

        // View, will also trigger refresh automatically
        $('#table').bootstrapTable({
            toolbar: '#toolbar',//工具栏

            url: apiHost + '/api/event/getEventList',
            method: 'post',
            contentType: "application/json",
            showRefresh: false,

            buttonsAlign: 'right',
            sidePagination: "server",
            pagination: true,
            paginationPreText: '<',
            paginationNextText: '>',
            pageNumber: 1,
            pageSize: 10,
            pageList: [10, 25, 50, 100],

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
                return '';
            },

            uniqueId: 'traceId',
            columns: [{
                field: 'timestamp',
                title: 'Time',
                formatter: function (value) {
                    return new Date(value).format('yyyy-MM-dd hh:mm:ss');
                },
                width: 200
            }, {
                field: 'application',
                title: 'Application',
                width: 200
            }, {
                field: 'instance',
                title: 'Instance',
                width: 200
            }, {
                field: 'type',
                title: 'Event',
                width: 100
            }, {
                field: 'args',
                title: 'Args',
                formatter: function (value, row, index) {
                    return `<button class="btn btn-sm btn-outline-info" onclick="javascript:toggleArgument(${index})">Toggle argument</button>`;
                }
            }],

            detailView: true,
            detailFormatter: (index, row) => {
                return "<pre>" + JSON.stringify(JSON.parse(row.args), null, 2) + "</pre>";
            }
        });
    }

    #getInterval() {
        return this.mInterval;//this.mInterval != null ? this.mInterval : this.vIntervalSelector.getInterval();
    }

    #refreshPage() {
        //
        // refresh the list
        //
        $('#table').bootstrapTable('refresh');
    }
}

function toggleArgument(index) {
    $('#table').bootstrapTable('toggleDetailView', index);
}