class TableComponent {
    /**
     *
     * @param option
     *     parent: p,
     *     columns: columns
     *     pagination: [true/false]
     */
    constructor(option) {
        this.vTable = option.parent.append(`<table id="${option.tableId}"></table>`).find('table');
        this.mColumns = option.columns;
        this.mCreated = false;
        this.mPagination = option.pagination === undefined ? false : option.pagination;
        this.mDetailViewField = null;
        this.mColumnMap = {};

        this.mFormatters = {};
        this.mFormatters['shortDateTime'] = (v) => new Date(v).format('MM-dd hh:mm:ss');
        this.mFormatters['detail'] = (val, row, index) => `<button class="btn btn-sm btn-outline-info" onclick="javascript:toggleTableDetailView('${option.tableId}', ${index})">Toggle</button>`;
        this.mFormatters['block'] = (val, row, index) => `<pre>${val}</pre>`;
        this.mFormatters['link'] = (val, row, index, field) => {
            const column = this.mColumnMap[field];
            const href = column.link.replaceAll('{val}', val);
            return `<a target="_blank" href="${href}">${val}</a>`;
        };

        for (let i = 0; i < this.mColumns.length; i++) {

            const column = this.mColumns[i];

            this.mColumnMap[column.field] = column;

            if (column.format !== undefined) {
                // formatter is an option provided by bootstrap-table
                column.formatter = this.mFormatters[column.format];
                if (column.format === 'detail') {
                    this.mDetailViewField = column.field;
                }
            }

            // original sorter uses string.localeCompare
            // That comparator returns different order from the result ordered by the server
            // So here we define a new comparator
            column.sorter = (a, b) => this.#compare(a, b);
        }
        this.mDetailView = this.mDetailViewField != null;
    }

    /**
     * public interface of component in dashboard
     */
    setOpenHandler(openHandler) {
    }

    /**
     * public interface of component in dashboard
     */
    resize() {
    }

    load(option) {
        console.log(option);

        this.mQueryParam = option.ajaxData;

        if (!this.mCreated) {
            this.mCreated = true;

            const tableOption = {
                url: option.url,
                method: 'post',
                contentType: "application/json",
                showRefresh: false,

                sidePagination: "server",
                pagination: this.mPagination,
                serverSort: false,

                queryParamsType: '',
                queryParams: (params) => this.#getQueryParams(params),

                columns: this.mColumns,

                detailView: this.mDetailView,
                detailFormatter: (index, row) => this.#showDetail(index, row),

                stickyHeader: true,
                stickyHeaderOffsetLeft: parseInt($('body').css('padding-left'), 10),
                stickyHeaderOffsetRight: parseInt($('body').css('padding-right'), 10),
                theadClasses: 'thead-light'
            };
            if (this.mPagination) {
                tableOption.paginationPreText = '<';
                tableOption.paginationNextText = '>';
                tableOption.pageNumber = 1;
                tableOption.pageSize = 10;
                tableOption.pageList = [10, 25, 50, 100];
            }

            this.vTable.bootstrapTable(tableOption);
        } else {
            this.vTable.bootstrapTable('refresh');
        }
    }

    #compare(a, b) {
        if (a === b) {
            return 0;
        } else {
            return a < b ? -1 : 1;
        }
    }

    #showDetail(index, row) {
        return '<pre>' + row[this.mDetailViewField] + '</pre>';
    }

    #getQueryParams(params) {
        this.mQueryParam.pageNumber = params.pageNumber - 1;
        this.mQueryParam.pageSize = params.pageSize;
        if (params.sortName === undefined) {
            delete this.mQueryParam.orderBy;
        } else {
            this.mQueryParam.orderBy = {
                name: params.sortName,
                order: params.sortOrder
            };
        }
        return this.mQueryParam;
    }

    show() {
        this.vTable.show();
    }

    clear() {
        this.vTable.bootstrapTable('removeAll');
    }

    hide() {
        this.vTable.hide();
    }
}

function toggleTableDetailView(tableId, index) {
    $('#' + tableId).bootstrapTable('toggleDetailView', index);
}