class ExpressionComponent {
    /**
     * {
     *      container: jQuery object
     *      ediable: true/false
     * }
     */
    constructor(option) {
        //
        // Model
        //
        this.mApplication = option.application;
        this.mEditable = option.editable || false;
        this.mConditions = {};
        this.mSchemaManager = new SchemaManager();

        //
        // View
        //
        this.vContainer = option.container;
        this.vEditComponent = new ConditionEditComponent({
            application: option.application,
            schemaManager: this.mSchemaManager
        });
    }

    getConditions() {
        const conditionList = [];
        $.each(this.mConditions, (k, v) => conditionList.push(v));
        return conditionList;
    }

    addConditions(conditions) {
        $.each(conditions, (index, condition) => this.addCondition(condition));
        return this;
    }

    /**
     * @param condition has following definition
     * {
     *     id:
     *     dataSource:
     *     dimensions: []
     *     metric: {}
     * }
     *
     */
    addCondition(condition) {
        const rowText =
            '<div class="input-group">\n' +
            '<div class="input-group-prepend">' +
            '            <input type="text" class="form-control condition-sn" value="A" style="width:40px" maxlength="4" disabled="disabled"/>' +
            '</div>' +
            '            <input class="form-control condition-datasource" />\n' +
            '            <input class="form-control condition-metric"/>\n' +
            '    <div class="input-group-append">                                                                  ' +
            '        <button class="btn btn-outline-info btn-sm show-metric" title="Delete this condition">Metric</button>         ' +
            '        <button class="btn btn-outline-info btn-sm modify-condition" title="Delete this condition">Modify</button>         ' +
            '        <button class="btn btn-outline-info btn-sm delete-condition" title="Delete this condition">Delete</button>         ' +
            '    </div>                                                                                        ' +
            '</div>';
        const vRow = $(rowText);
        this.#bind(vRow, null, condition);

        vRow.find('.show-metric').click(() => this.#showMetric(condition.id));
        if (this.mEditable) {
            vRow.find('.modify-condition').click(() => this.#modifyCondition(condition.id));
            vRow.find('.delete-condition').click(() => this.#deleteCondition(condition.id));
        }

        this.vContainer.append(vRow);
        this.mConditions[condition.id] = condition;

        return vRow;
    }

    rmvCondition() {

    }

    #createRow() {
    }

    #bind(vRow, schema, condition) {
        const id = condition.id;

        vRow.attr("id", "condition-" + id);
        vRow.find(".condition-sn").val(id);
        vRow.find(".condition-datasource").val(condition.dataSource);

        let afterCond = vRow.find(".condition-datasource").parent().parent();

        // index[0] is the appName filter, skip it on the front page
        for (let i = 1; i < condition.dimensions.length; i++) {
            const filter = condition.dimensions[i];
            const dimensionCell = $(
                '<div class="multiSelectBox">             ' +
                '    <div class="input-group">                                            ' +
                '        <input class="form-control" style="width: 150px;"/>' +
                '    </div>                                                               ' +
                '</div>                                                                   '
            );
            $(dimensionCell).find("input").val(filter.name + " = " + filter.matcher.pattern);
            afterCond.after(dimensionCell);
            afterCond = dimensionCell;
        }

        vRow.find(".condition-metric").val(condition.metric.name + ' ' + condition.metric.comparator + ' ' + condition.metric.expected);
    }

    #showMetric(id) {
        const condition = this.mConditions[id];
        const dataSourceName = condition.dataSource;
        // if (isBlankString(dataSourceName)) {
        //     showMessage("Please choose a data source");
        //     return;
        // }

        const metricName = condition.metric.name;
        // if (isBlankString(metricName)) {
        //     showMessage("Please select a metric");
        //     return;
        // }

        const metricSpec = this.mSchemaManager.getDataSourceSchema(dataSourceName).metricsMap[metricName];
        const filters = [{
            dimension: 'appName',
            matcher: {
                type: "equal",
                pattern: this.mApplication
            }
        }];
        $.each(condition.dimensions, (index, filter) => {
            filters.push(filter);
        });

        const metricComponent = new MetricComponent();
        metricComponent.showDialog({
            dialogTitle: 'Realtime Metric Monitoring [' + dataSourceName + '-' + metricSpec.name + ']',
            dataSourceName: dataSourceName,
            dimensions: filters,
            metricCondition: condition.metric,
            end: Date.now(),
            hours: 3,
            metricSpec: metricSpec,
            threshold: condition.metric.expected
        });
    }

    #modifyCondition(id) {
        this.vEditComponent.edit(this.mConditions[id]);
    }

    #deleteCondition(id) {
        bootbox.confirm({
            title: 'Confirm',
            message: 'Are you sure to delete this condition?',
            callback: (result) => {
                if (result) {
                    this.vContainer.find('#condition-' + id).remove();
                    delete this.mConditions[id];
                }
            }
        })
    }

}