class TreeTable {

    constructor(parent, option) {
        this.mColumns = option.columns;

        const tableTemplate =
            '<div class="bootstrap-table bootstrap4">\n' +
            '    <div class="fixed-table-container" style="padding-bottom: 0;">\n' +
            '        <div class="fixed-table-body">\n' +
            '            <div class="fixed-table-loading table table-bordered table-hover" style="top: 50px;">\n' +
            '               <span class="loading-wrap" style="margin-top: 50px">\n' +
            '                   <span class="loading-text" style="font-size: 20px;">Loading, please wait</span>\n' +
            '               </span>\n' +
            '            </div>\n' +
            '            <table class="table table-bordered table-hover"></table>\n' +
            '        </div>\n' +
            '    </div>\n' +
            '</div>';

        const vTable = $(parent).append(tableTemplate).find('table');
        const vTableElement = vTable[0];
        this.vDocElement = vTableElement.ownerDocument;

        const vHead = this.vDocElement.createElement('thead');
        if (option.headerStyle !== undefined) {
            $(vHead).addClass(option.headerStyle);
        }
        const vHeadRow = this.vDocElement.createElement('tr');
        $.each(option.columns, (index, column) => {
            if (column.formatter === undefined) {
                column.formatter = this.#defaultFormatter;
            }

            // create UI cell
            const cell = this.vDocElement.createElement('th');
            if (column.width === undefined) {
                cell.innerHTML = `<div class="th-inner"}>${column.title}</div>`;
            } else {
                cell.innerHTML = `<div class="th-inner" style="width: ${column.width}px">${column.title}</div>`;
            }
            vHeadRow.appendChild(cell);
        });
        vHead.appendChild(vHeadRow);
        vTableElement.appendChild(vHead);

        this.vTableBody = this.vDocElement.createElement('tbody')
        vTableElement.appendChild(this.vTableBody);

        this.vLoading = $(parent).find('.fixed-table-loading');

        // Model, cache the data for further API
        this.mRowDatas = [];
        this.mTreeColumn = option.treeColumn;
        this.mRowStyle = option.rowStyle;
        this.mMaxDepth = 0;
    }

    load(option) {
        this.mGetChildren = option.getChildren;

        let data = option.data;
        if (data instanceof Function) {
            data = data.apply();
        }
        this.#showLoading("Loading, please wait...");
        $.ajax({
            url: option.url,
            data: JSON.stringify(data),
            type: option.method,
            async: true,
            dataType: "json",
            contentType: "application/json",
            success: (data) => {
                const rows = option.responseHandler(data);
                if (rows.length === 0) {
                    this.#showLoading("No tracing logs found for this trace. You can reload the page to retry.");
                } else {
                    this.#renderTable(rows);
                    this.#closeLoading();
                }
            },
            error: (data) => {
                this.#closeLoading();
            }
        });

        return this;
    }

    #showLoading(text, showing) {
        this.vLoading.find(".loading-text").text(text);
        this.vLoading.toggleClass('open', true);
    }

    #closeLoading() {
        this.vLoading.toggleClass('open', false);
    }

    #defaultFormatter(cellValue, row, rowIndex, rows) {
        return cellValue;
    }

    #renderTable(roots) {
        let globalRowIndex = -1;
        for (let i = 0; i < roots.length; i++) {
            globalRowIndex = this.#renderRow(roots[i], ++globalRowIndex, 0, (i + 1));
        }
    }

    #renderRow(rowData, globalRowIndex, depth, levelId) {
        if (depth > this.mMaxDepth) {
            this.mMaxDepth = depth;
        }

        rowData.__levelId = levelId;
        rowData.__vExpanded = true;
        rowData.__vVisible = true;
        rowData.__vRowIndex = globalRowIndex;
        this.mRowDatas.push(rowData);

        const children = this.mGetChildren(rowData);

        //
        // build an HTML row for current row data
        //
        const thisRowIndex = globalRowIndex;
        const row = this.vDocElement.createElement('tr');
        if (this.mRowStyle !== undefined) {
            const style = this.mRowStyle(this.mRowDatas, globalRowIndex);
            if (style != null) {
                $(row).addClass(style);
            }
        }
        for (let i = 0; i < this.mColumns.length; i++) {
            const col = this.mColumns[i];

            let cellHTML = '';
            if (i === this.mTreeColumn) {
                cellHTML += this.#getIndent(depth);
                cellHTML += this.#createExpander(children.length > 0);
            }

            cellHTML += col.formatter(rowData[col.field], rowData, globalRowIndex, this.mRowDatas);

            const vCell = this.vDocElement.createElement('td');
            vCell.innerHTML = cellHTML;
            row.appendChild(vCell);
        }
        this.vTableBody.appendChild(row);

        //
        // build child rows recursively
        //
        for (let i = 0; i < children.length; i++) {
            globalRowIndex = this.#renderRow(children[i], ++globalRowIndex, depth + 1, levelId + '.' + (i + 1));
        }

        // after building the child, the rowIndex holds the last descendant's index in the table rows
        if (globalRowIndex > thisRowIndex) {
            const expanderElement = row.querySelector('.treegrid-expander');
            if (expanderElement != null) {
                expanderElement.addEventListener('click', (e) => {
                    this.#toggle(thisRowIndex, expanderElement);
                });
            }
        }

        return globalRowIndex;
    }

    #getIndent(depth) {
        return depth === 0 ? '' : `<span style="width: ${depth * 16}px; height: 16px; display: inline-block; position: relative"></span>`;
    }

    #createExpander(expanded) {
        return `<span class="treegrid-expander ${expanded ? 'treegrid-expander-expanded' : ''}"></span>`;
    }

    #toggle(parentRowIndex, expanderElement) {
        const expander = $(expanderElement);
        const rowData = this.mRowDatas[parentRowIndex];
        if (rowData.__vExpanded) {
            expander.removeClass('treegrid-expander-expanded')
                .addClass('treegrid-expander-collapsed');

            this.#collaps(parentRowIndex);

            rowData.__vExpanded = false;
        } else {
            expander.removeClass('treegrid-expander-collapsed')
                .addClass('treegrid-expander-expanded');

            // set this flag first
            rowData.__vExpanded = true;

            this.#expand(parentRowIndex);
        }
    }

    #collaps(parentRowIndex) {
        const parentRow = this.mRowDatas[parentRowIndex];
        if (!parentRow.__vExpanded) {
            // already collapsed
            return;
        }

        const children = this.mGetChildren(parentRow);
        for (let i = 0; i < children.length; i++) {
            const vChildRowIndex = children[i].__vRowIndex;

            console.log("collapse " + vChildRowIndex);

            // change the UI state
            $(this.vTableBody.children[vChildRowIndex]).hide();

            this.#collaps(children[i].__vRowIndex);
        }
    }

    #expand(parentRowIndex) {
        const parentRow = this.mRowDatas[parentRowIndex];
        if (!parentRow.__vExpanded) {
            // already collapsed
            return;
        }

        const children = this.mGetChildren(parentRow);
        for (let i = 0; i < children.length; i++) {
            // change the UI state
            const vChildRowIndex = children[i].__vRowIndex;

            $(this.vTableBody.children[vChildRowIndex]).show();

            this.#expand(children[i].__vRowIndex);
        }
    }

    getRowData() {
        return this.mRowDatas;
    }
}