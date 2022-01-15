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

        this.vTable = $(parent).append(tableTemplate).find('table');
        this.vTableElement = this.vTable[0];
        this.vDocElement = this.vTableElement.ownerDocument;

        const vHead = this.vDocElement.createElement('thead');
        const vHeadRow = this.vDocElement.createElement('tr');
        $.each(option.columns, (index, column) => {
            if (column.formatter === undefined) {
                column.formatter = this.#defaultFormatter;
            }

            // create UI cell
            const cell = this.vDocElement.createElement('th');
            cell.innerHTML = `<div class="th-inner">${column.title}</div>`;
            vHeadRow.appendChild(cell);
        });
        vHead.appendChild(vHeadRow);
        this.vTableElement.appendChild(vHead);

        // Model, cache the data for further API
        this.mRowDatas = [];
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
                this.#renderTable(option.responseHandler(data), 0, 0);
            },
            error: (data) => {
            }
        });

        return this;
    }

    #defaultFormatter(cellValue, row, rowIndex) {
        return cellValue;
    }

    #renderTable(roots, depth, padding) {
        let rowIndex = -1;
        for (let i = 0; i < roots.length; i++) {
            rowIndex = this.#renderRow(roots[i], ++rowIndex, depth, padding);
        }
    }

    #renderRow(rowData, rowIndex, depth, padding) {
        this.mRowDatas.push(rowData);

        const row = this.vDocElement.createElement('tr');
        $.each(this.mColumns, (index, col) => {

            const cell = this.vDocElement.createElement('td');

            cell.innerHTML = col.formatter(rowData[col.field], rowData, rowIndex);

            row.appendChild(cell);
        });
        this.vTableElement.appendChild(row);

        const children = this.mGetChildren(rowData);
        for (let i = 0; i < children.length; i++) {
            rowIndex = this.#renderRow(children[i], ++rowIndex, depth + 1, padding);
        }

        return rowIndex;
    }

    getRowData() {
        return this.mRowDatas;
    }
}