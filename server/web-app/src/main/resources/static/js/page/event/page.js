class EventPage {
    constructor(appName) {
        // Model
        this.mApplication = appName;
        this.mInterval = null;

        // View
        this.vFilter = new AppSelector({
            appName: appName,
            parentId: 'filterBar',
            intervalProvider: () => this.#getInterval(),
        }).registerChangedListener((name, value) => {
            if (name === 'application') {
                g_SelectedApp = value;
                window.history.pushState('', '', `/web/app/event/${value}`);
            }

            this.#refreshPage();
        }).createAppSelector(appName)
            .createFilterByFields('event', ['instanceName', 'type']);

        const parent = $('#filterBarForm');

        // View - Refresh Button
        parent.append('<button class="btn btn-outline-secondary" style="border-radius:0;border-color: #ced4da" type="button"><i class="fas fa-sync-alt"></i></button>')
            .find("button").click(() => {
            this.mInterval = this.vIntervalSelector.getInterval();
            this.#refreshPage();
        });

        // View
        this.vIntervalSelector = new TimeInterval('all', true).childOf(parent).registerIntervalChangedListener((selectedModel) => {
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
                    filters: this.vFilter.getSelectedFilters(),
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
                if (row.formattedArgs === undefined) {
                    row.formattedArgs = "<pre>" + JSON.stringify(JSON.parse(row.args), null, 2) + "</pre>";
                }
                return row.formattedArgs;
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