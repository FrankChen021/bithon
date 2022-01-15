function onTableComponentButtonClick(id, rowIndex, buttonIndex) {
    const tableComponent = window.gDetailTables[id];
    if ( tableComponent != null ) {
        tableComponent.onButtonClick(rowIndex, buttonIndex);
    }
}

class TableComponent {
    /**
     *
     * @param parent
     * @param columns arrays of colum. Each of which is {field, title, width}
     * @param buttons arrays of button, Each of which is {text, class, onclick }
     */
    constructor(id, parent, columns, buttons) {
        this.vTable = parent.append(`<table id="${id}"></table>`).find('table');
        this.mColumns = columns;
        this.mCreated = false;
        this.mButtons = buttons;

        $.each(buttons, (buttonIndex, button) => {
            this.mColumns.push(
                {
                    field: 'id',
                    title: button.text,
                    align: 'center',
                    visible: button.visible,
                    formatter: (cell, row, rowIndex, field) => {
                        return `<a href="#" class="badge badge-info" onclick="javascript:onTableComponentButtonClick('${id}', ${rowIndex}, ${buttonIndex})">&gt;</a>`;
                    }
                }
            );
        })
        if (window.gDetailTables === undefined) {
            window.gDetailTables = {};
        }
        window.gDetailTables[id] = this;
    }

    onButtonClick(rowIndex, buttonIndex) {
        const row = this.vTable.bootstrapTable('getData')[rowIndex];
        this.mButtons[buttonIndex].onClick(rowIndex, row, this.mStartTimestamp, this.mEndTimestamp);
    }

    load(option) {
        console.log(option);

        this.mQueryParam = option.ajaxData;
        this.mStartTimestamp = option.start;
        this.mEndTimestamp = option.end;

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