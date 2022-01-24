class TableComponent {
    /**
     *
     * @param parent
     * @param columns arrays of colum. Each of which is {field, title, width}
     */
    constructor(parent, columns) {
        this.vTable = parent.append('<table id="table"></table>').find('table');
        this.mColumns = columns;
        this.mCreated = false;
    }

    load(option) {
        console.log(option);

        this.mQueryParam = option.ajaxData;

        if (!this.mCreated) {
            this.mCreated = true;

            this.vTable.bootstrapTable({
                url: option.url,
                method: 'post',
                contentType: "application/json",
                showRefresh: false,

                sidePagination: "server",
                pagination: false,
                serverSort: false,
                // paginationPreText: '<',
                // paginationNextText: '>',
                // pageNumber: 1,
                // pageSize: 10,
                // pageList: [10, 25, 50, 100],

                queryParamsType: '',
                queryParams: (params) => this.#getQueryParams(),

                columns: this.mColumns
            });
        } else {
            this.vTable.bootstrapTable('refresh');
        }
    }

    #getQueryParams() {
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