class TreeTable {

    constructor(parent, option) {
        this.mColumns = option.columns;

        const tableTemplate =
            '<div class="bootstrap-table bootstrap4">\n' +
            '    <div class="fixed-table-toolbar"></div>\n' +
            '    <div class="fixed-table-container" style="padding-bottom: 0;">\n' +
            '        <div class="fixed-table-body">\n' +
            '            <div class="fixed-table-loading table table-bordered table-hover" style="top: 50px;">\n' +
            '               <span class="loading-wrap">\n' +
            '                   <span class="loading-text" style="font-size: 32px;">Loading, please wait</span>\n' +
            '                   <span class="animation-wrap"><span class="animation-dot"></span></span>' +
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
        const vHeadRow = this.vDocElement.createElement('tr');
        $.each(option.columns, (index, column) => {
            if (column.formatter === undefined) {
                column.formatter = this.#defaultFormatter;
            }

            // create UI cell
            const cell = this.vDocElement.createElement('th');
            if ( column.width === undefined ) {
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

        // Model, cache the data for further API
        this.mRowDatas = [];
        this.mTreeColumn = option.treeColumn;
        this.mMaxDepth = 0;
    }

    load(option) {
        this.mGetChildren = option.getChildren;

        let data = option.data;
        if (data instanceof Function) {
            data = data.apply();
        }
        $.ajax({
            url: option.url,
            data: JSON.stringify(data),
            type: option.method,
            async: true,
            dataType: "json",
            contentType: "application/json",
            success: (data) => {
                this.#renderTable(option.responseHandler(data));
            },
            error: (data) => {
            }
        });

        return this;
    }

    #defaultFormatter(cellValue, row, rowIndex) {
        return cellValue;
    }

    #renderTable(roots, depth) {
        let rowIndex = -1;
        for (let i = 0; i < roots.length; i++) {
            rowIndex = this.#renderRow(roots[i], ++rowIndex, 0, (i + 1));
        }
    }

    #renderRow(rowData, rowIndex, depth, levelId) {
        if (depth > this.mMaxDepth) {
            this.mMaxDepth = depth;
        }

        rowData.__levelId = levelId;
        rowData.__vExpanded = true;
        rowData.__vVisible = true;
        rowData.__vRowIndex = rowIndex;
        this.mRowDatas.push(rowData);

        const children = this.mGetChildren(rowData);

        //
        // build an HTML row for current row data
        //
        const thisRowIndex = rowIndex;
        const row = this.vDocElement.createElement('tr');
        for (let i = 0; i < this.mColumns.length; i++) {
            const col = this.mColumns[i];

            let cellHTML = '';
            if (i === this.mTreeColumn) {
                cellHTML += this.#getIndent(depth);
                cellHTML += this.#createExpander(children.length > 0);
            }

            cellHTML += col.formatter(rowData[col.field], rowData, rowIndex);

            const vCell = this.vDocElement.createElement('td');
            vCell.innerHTML = cellHTML;
            row.appendChild(vCell);
        }
        this.vTableBody.appendChild(row);

        //
        // build child rows recursively
        //
        for (let i = 0; i < children.length; i++) {
            rowIndex = this.#renderRow(children[i], ++rowIndex, depth + 1, levelId + '.' + (i + 1));
        }

        // after building the child, the rowIndex holds the last descendant's index in the table rows
        if (rowIndex > thisRowIndex) {
            const expanderElement = row.querySelector('.treegrid-expander');
            if (expanderElement != null) {
                expanderElement.addEventListener('click', (e) => {
                    this.#toggle(thisRowIndex, expanderElement);
                });
            }
        }

        return rowIndex;
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