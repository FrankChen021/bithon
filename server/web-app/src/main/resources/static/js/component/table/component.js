function onTableComponentButtonClick(id, rowIndex, buttonIndex) {
    const tableComponent = window.gTableComponents[id];
    if (tableComponent != null) {
        tableComponent.onButtonClick(rowIndex, buttonIndex);
    }
}

function onTableComponentColumnToggle(id, col) {
    if (window.event.target.localName === 'input') {
        const tableComponent = window.gTableComponents[id];
        if (tableComponent != null) {
            tableComponent.toggleColumn(col);
        }
    }
}

class TableComponent {
    /**
     *
     * @param option
     *     parent: p,
     *     columns: columns
     *     pagination: either a list or an object. If it's an object, should be { pages: [], side: 'server' | 'client' }
     *     tableId: an unique id for the table component
     */
    constructor(option) {
        // view
        this.vComponentContainer = $(`<div class="card card-block rounded-0"></div>`);
        this.vTable = this.vComponentContainer.append(`<div class="table-container"><table id="${option.tableId}"></table></div>`).find('table');
        option.parent.append(this.vComponentContainer);

        this.mShowColumn = option.toolbar !== undefined ? (option.toolbar.showColumns === true) : false;
        this.mColumns = option.columns;
        this.mCreated = false;

        this.mHasPagination = option.pagination !== undefined && option.pagination.length > 0;
        if (Array.isArray(option.pagination)) {
            this.mPagination = option.pagination;
            this.mPaginationSide = 'server';
        } else if (option.pagination !== undefined) {
            this.mPagination = option.pagination.pages;
            this.mPaginationSide = option.pagination.side;
        } else {
            // Make sure the variable has been defined
            this.mPaginationSide = 'server';
            this.mPagination = [10];
        }

        this.mDetailViewField = null;
        this.mColumnMap = {};

        this.mDefaultOrder = option.order;
        this.mDefaultOrderBy = option.orderBy;

        this.mFormatters = {};
        this.mFormatters['shortDateTime'] = (v) => new Date(v).format('MM-dd hh:mm:ss');
        this.mFormatters['detail'] = (val, row, index) => val !== "" ? `<button class="btn btn-sm btn-outline-info" onclick="toggleTableDetailView('${option.tableId}', ${index})">Toggle</button>` : '';
        this.mFormatters['dialog'] = (val, row, index, field) => val !== "" ? `<button class="btn btn-sm btn-outline-info" onclick="showTableDetailViewInDlg('${option.tableId}', ${index}, '${field}')">Show</button>` : '';
        this.mFormatters['block'] = (val, row, index) => `<pre>${val}</pre>`;
        this.mFormatters['template'] = (val, row, index, field) => {
            const column = this.mColumnMap[field];
            return column.template.replaceAll('{value}', val);
        };
        this.mFormatters['timeDuration'] = (val) => val.formatTimeDuration();

        for (let i = 0; i < this.mColumns.length; i++) {

            const column = this.mColumns[i];

            this.mColumnMap[column.field] = column;

            if (column.format !== undefined && column.formatter == null) {
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
        this.mTableHasDetailView = this.mDetailViewField != null;

        this.mButtons = option.buttons;
        $.each(this.mButtons, (buttonIndex, button) => {
            this.mColumns.push(
                {
                    field: 'id',
                    title: button.title,
                    align: 'center',
                    visible: button.visible,
                    formatter: (cell, row, rowIndex, field) => {
                        // href is not set, so a 'class' is needed to make default global css render it as an Anchor
                        // style cursor is explicitly set
                        return `<a class="non-exist" style="cursor: pointer" onclick="onTableComponentButtonClick('${option.tableId}', ${rowIndex}, ${buttonIndex})"><span class="fa fa-forward"></span></a>`;
                    }
                }
            );
        })
        if (window.gTableComponents === undefined) {
            window.gTableComponents = {};
        }
        window.gTableComponents[option.tableId] = this;
    }

    #ensureHeader() {
        if (this._header == null) {
            this._header = $(this.vComponentContainer).prepend(
                '<div class="card-header d-flex" style="padding: 0.5em 1em">' +
                '<span class="header-text btn-sm"></span>' +
                '<div class="tools ml-auto">' +
                '<button class="btn btn-sm btn-toggle"><span class="far fa-window-minimize"></span></button>' +
                '</div>' +
                '</div>');

            const toggleButton = this._header.find('.btn-toggle');
            toggleButton.click(() => {
                const tableContainer = this.vComponentContainer.find('.table-container');
                tableContainer.toggle();

                if (tableContainer.is(':visible')) {
                    toggleButton.find('span').removeClass('fa-window-maximize').addClass("fa-window-minimize");
                } else {
                    toggleButton.find('span').removeClass('fa-window-minimize').addClass("fa-window-maximize");
                }
            });
        }
        return this._header;
    }

    header(text) {
        this.#ensureHeader().find('.header-text').html(text);
        return this;
    }

    toggleColumn(columnName) {
        let visible = null;

        const cols = this.vTable.bootstrapTable('getVisibleColumns');
        for (let i = 0; i < cols.length; i++) {
            if (cols[i].field === columnName) {
                visible = true;
                break;
            }
        }

        if (visible != true) {
            const cols = this.vTable.bootstrapTable('getHiddenColumns');
            for (let i = 0; i < cols.length; i++) {
                if (cols[i].field === columnName) {
                    visible = false;
                    break;
                }
            }
        }

        if (visible === true) {
            this.vTable.bootstrapTable('hideColumn', columnName);
        } else if (visible === false) {
            this.vTable.bootstrapTable('showColumn', columnName);
        }
    }

    onButtonClick(rowIndex, buttonIndex) {
        const row = this.vTable.bootstrapTable('getData')[rowIndex];
        this.mButtons[buttonIndex].onClick(rowIndex, row, this.mStartTimestamp, this.mEndTimestamp);
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

        if (option.start !== undefined) {
            this.mStartTimestamp = option.start;
            this.mEndTimestamp = option.end;
        } else {
            this.mStartTimestamp = option.ajaxData.interval.startISO8601;
            this.mEndTimestamp = option.ajaxData.interval.endISO8601;
        }
        this.mQueryParam = option.ajaxData;

        if (!this.mCreated) {
            this.mCreated = true;

            const tableOption = {
                url: option.url,
                method: 'post',
                contentType: "application/json",
                showRefresh: false,
                responseHandler: option.responseHandler,

                sidePagination: this.mPaginationSide,
                pagination: this.mHasPagination,

                serverSort: this.mHasPagination,
                sortName: this.mDefaultOrderBy,
                sortOrder: this.mDefaultOrder,

                queryParamsType: '',
                queryParams: (params) => this.#getQueryParams(params),

                columns: this.mColumns,

                detailView: this.mTableHasDetailView,
                detailFormatter: (index, row) => this.#showDetail(index, row),

                stickyHeader: true,
                stickyHeaderOffsetLeft: parseInt($('body').css('padding-left'), 10),
                stickyHeaderOffsetRight: parseInt($('body').css('padding-right'), 10),
                theadClasses: 'thead-light'
            };
            if (this.mHasPagination) {
                tableOption.paginationPreText = '<';
                tableOption.paginationNextText = '>';
                tableOption.pageNumber = 1;
                tableOption.pageSize = this.mPagination[0];
                tableOption.pageList = this.mPagination;
            }

            this.vTable.bootstrapTable(tableOption);

            this.#createShowColumnDropdownList();
        } else {
            this.vTable.bootstrapTable('refresh');
        }
    }

    #createShowColumnDropdownList() {
        if (!this.mShowColumn)
            return;

        let headerText = this.#ensureHeader().find('.header-text').html();
        if (headerText === '') {
            headerText = 'Columns';
        }

        // Build the dropdown list
        let dropDownList = '<div class="btn-group dropright"><button type="button" class="btn btn-sm dropdown-toggle" data-toggle="dropdown" aria-expanded="false">' + headerText +
            '<div class="dropdown-menu dropdown-menu-lg-right">';
        {
            const tableId = this.vTable.attr('id');
            for (let i = 0; i < this.mColumns.length; i++) {
                const col = this.mColumns[i];
                dropDownList += `<label class="dropdown-item dropdown-item-marker" onclick="onTableComponentColumnToggle('${tableId}', '${col.field}'); event.stopPropagation();"><input type="checkbox" checked=${col.visible || true} ><span>&nbsp;${col.title}</span></label>`;
            }
        }
        dropDownList += '</div></button></div>';

        // Add the dropdown list to DOM
        this.#ensureHeader().find('.header').replaceWith(dropDownList);
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
        if (this.mHasPagination && this.mPaginationSide === 'server') {
            // Compatible with old interface
            this.mQueryParam.pageNumber = params.pageNumber - 1;
            this.mQueryParam.pageSize = params.pageSize;

            this.mQueryParam.limit = {
                limit: params.pageSize,
                offset: this.mQueryParam.pageNumber * this.mQueryParam.pageSize
            }
        }

        if (params.sortName === undefined || params.sortName == null) {
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
        this.vComponentContainer.show();
    }

    clear() {
        this.vTable.bootstrapTable('removeAll');
    }

    hide() {
        this.vComponentContainer.hide();
    }

    getColumns() {
        return this.mColumns;
    }
}

function toggleTableDetailView(tableId, index) {
    $('#' + tableId).bootstrapTable('toggleDetailView', index);
}

function showTableDetailViewInDlg(tableId, index, field) {
    const rows = $('#' + tableId).bootstrapTable('getData');
    const row = rows[index];
    const cell = row[field];

    bootbox.dialog({
        centerVertical: true,
        size: 'xl',
        onEscape: true,
        backdrop: true,
        message: `<pre id="tagValueView">${cell}\n</pre>`,
    });
}