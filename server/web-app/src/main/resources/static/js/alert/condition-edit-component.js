class ConditionEditComponent {

    constructor(option) {
        // will be assigned when dialog is created
        this.vDimensionContainer = null;
        this.vMetricSelector = null;
        this.vMetricUnit = null;

        this.vDialogContent =
            '<div class="input-group">' +
            '   <div class="input-group-prepend"><span class="input-group-text">Data Source</span></div>' +
            '   <select class="form-control data-source" style="width: 300px;"></select>' +
            '</div>' +
            '<hr/>' +
            '<div class="dimensions"></div>' +
            '<hr/>' +
            '<div class="input-group">' +
            '   <div class="input-group-prepend"><span class="input-group-text">Metric</span></div>' +
            '   <select class="form-control metric-select"></select>' +
            '            <select class="form-control comparison" name="comparison" id="comparison"             ' +
            '                    style="width: 105px;">                                                        ' +
            '                <option value=">=">&gt;=</option>                                                 ' +
            '                <option value=">">&gt;</option>                                                   ' +
            '                <option value="<=">&lt;=</option>                                                 ' +
            '                <option value="<">&lt;</option>                                                   ' +
            '            </select>                                                                             ' +
            '            <input type="text" class="form-control threshold"                                     ' +
            '                   style="width: 80px;" placeholder="threshold" maxlength="10"/>                         ' +
            '<input class="form-control unit" style="width:60px;" disabled="disabled"/>  ' +
            '</div>';

        this.vMetricOptionsCache = {};

        // Model
        this.mApplication = option.application;
        this.mSchemas = option.schemaManager.getDataSourceSchemas();

        // Model
        this.mDataSourceName = null;
    }

    //
    edit() {

    }


    edit(condition) {
        this.mDataSourceName = condition.dataSource;
        this.mDimensions = {};

        // turn list into map
        $.each(condition.dimensions, (index, dimension) => {
            this.mDimensions[dimension.name] = dimension;
        });
        this.#show();
    }

    #show() {
        bootbox.dialog({
            size: 'xl',
            backdrop: true,
            onEscape: true,
            title: 'Edit Condition',
            message: this.vDialogContent,
            onShow: (e) => this.#createDialog(e),
            buttons: {
                cancel: {
                    label: "Cancel"
                },
                ok: {
                    label: "OK",
                    callback: () => {
                    }
                }
            }
        });
    }

    #createDialog(e) {
        const dlg = $(e.currentTarget);
        this.vDimensionContainer = dlg.find('.dimensions');
        this.vMetricSelector = dlg.find('.metric-select');
        this.mMetricUnit = dlg.find('.unit');

        const vDataSourceSelector = dlg.find('.data-source');

        //
        // Set dataSource options
        //
        let dataSourceOptions = '';
        $.each(this.mSchemas, (name, schema) => {
            dataSourceOptions += "<option value=\"" + schema.name + "\" >" + schema.name + "</option>";
        });
        vDataSourceSelector.html(dataSourceOptions);

        //
        // Bind events
        //
        vDataSourceSelector.on('change', (event) => {
            const dataSourceName = event.target.value;
            this.#onDataSourceChanged(this.mSchemas[dataSourceName]);
        });
        this.vMetricSelector.on('change', (event) => {
            const metricIndex = event.target.selectedIndex;
            const metricName = event.target.value;
            this.#onMetricChanged(metricIndex, metricName);
        });

        // bind control event
        //this.vDialogContent
    }

    #onDataSourceChanged(dataSource) {
        this.mDataSourceName = dataSource.name;

        // clear previous dimension selectors
        this.vDimensionContainer.empty();

        //
        // update all dimensions
        //
        for (let index = 0; index < dataSource.dimensionsSpec.length; index++) {
            const dimension = dataSource.dimensionsSpec[index];
            if (!dimension.visible) {
                continue;
            }

            const vDimensionFilter = $(
                '<div class="input-group">' +
                '   <div class="input-group-prepend">' +
                '       <span class="input-group-text"></span></div>' +
                '       <select class="form-control" style="width: 300px;"></select>' +
                '   </div>' +
                '</div>')
                .attr('data-name', dimension.name);
            vDimensionFilter.find('.input-group-text').text(dimension.displayText);
            this.vDimensionContainer.append(vDimensionFilter);

            const thisIndex = index;
            $(vDimensionFilter)
                .find("select")
                .attr('name', dimension.name)
                .select2({
                    theme: 'bootstrap4',
                    dropdownAutoWidth: true,
                    placeholder: 'Optional',
                    allowClear: true,
                    tags: true,
                    ajax: this.#getConditionListAjaxOptions(dataSource, thisIndex, dimension)
                }).on('select2:select', (e) => {
                const dimensionName = $(e.target).attr('name');

                const selectedOption = $(e.target).find(':selected');
                this.mDimensions[dimensionName] = {
                    dimension: dimensionName,
                    matcher: {
                        type: 'equal',
                        pattern: $(selectedOption).val()
                    }
                };
                console.log(this.mDimensions);
            }).on('select2:clear', (e) => {
                //retrieve id again
                const dimensionName = $(e.target).attr('name');

                delete this.mDimensions[dimensionName];
            });
        } //end of for each dimension

        //
        // update metrics UI
        //
        if (this.vMetricOptionsCache[dataSource.name] === undefined) {
            //
            // cache options
            //
            let options = "<option>--select a metric--</option>";
            $.each(dataSource.metricsSpec, (index, metric) => {
                if (metric.visible) {
                    options += "<option value=\"" + metric.name + "\" >" + metric.name + "</option>";
                }
            });
            this.vMetricOptionsCache[dataSource.name] = options;
        }
        this.vMetricSelector.html(this.vMetricOptionsCache[dataSource.name]).change();
    }

    #getConditionListAjaxOptions(dataSource, dimensionIndex, dimension) {
        return {
            cache: true,
            type: 'POST',
            url: apiHost + '/api/datasource/dimensions/v2',
            data: () => {
                const filters = [{
                    dimension: 'appName',
                    matcher: {
                        type: "equal",
                        pattern: this.mApplication
                    }
                }];
                for (let p = 0; p < dimensionIndex; p++) {
                    const dimSpec = dataSource.dimensionsSpec[p];
                    const dimFilter = this.mDimensions[dimSpec.name];
                    if (dimFilter != null) {
                        filters.push(dimFilter);
                    }
                }

                return JSON.stringify({
                    dataSource: dataSource.name,
                    name: dimension.name,
                    filters: filters,
                    startTimeISO8601: moment().utc().subtract(1, 'day').local().toISOString(),
                    endTimeISO8601: moment().utc().local().toISOString(),
                })
            },
            dataType: "json",
            contentType: "application/json",
            beforeRequest: function (option) {
                // for (let p = 0; p < dimensionIndex; p++) {
                //     const dimension = dataSource.dimensionsSpec[p];
                //     if (isBlankString(gEditingConditions[rowId].dimensions[dimension.name].expected.value)) {
                //
                //         $("dimensionCond_" + dimension.name).find("select").select2('close');
                //
                //         showMessage("Please select" + (dimension.displayText));
                //
                //         return false;
                //     }
                // }
                return true;
            },
            processResults: function (data) {
                return {
                    results: data.map(dimension => {
                        return {
                            "id": dimension.value,
                            "text": dimension.value
                        };
                    })
                };
            },
            error: (e) => {
                showMessage(e.responseText);
            }
        }
    }

    #onMetricChanged(metricIndex, metricName) {
        if (metricIndex === 0) {
            // clear metric

            return;
        }

        this.mMetricUnit.val(this.mSchemas[this.mDataSourceName].metricsMap[metricName].unit);
    }
}