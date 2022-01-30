class AppSelector {

    /**
     * {
     *      appName:    <required>
     *      intervalProvider: fn,
     *      parentId:   <required>
     * }
     */
    constructor(option) {
        this.vParent = $('#' + option.parentId);

        this.mIntervalProviderFn = option.intervalProvider;
        this.mRequestFilterFn = option.requestFilterFn;
        this.mApplication = option.appName;
        this.mInstance = window.queryParams !== undefined ? window.queryParams['instance'] : null;
        this._listeners = [];
        this.mSelectionChangedListener = [];

        this._selectedDimensions = {};
        this.addDimension("appName", option.appName);

        //
        // create app selector
        //
        this.vAppSelector = $('<li class="nav-item"><select class="form-control" style="width:200px"></select></li>').find('select');
        this.vParent.append(this.vAppSelector);
        $(document).ready(() => {
            this.vAppSelector.select2({
                theme: 'bootstrap4',
                dropdownAutoWidth: true,
                ajax: this.#getApplicationOptions()
            });
        });

        this.vAppSelector.append(`<option value="${this.mApplication}">${this.mApplication}</option>`).change();

        this.vAppSelector.change((e) => {
            const text = e.target.selectedOptions[0].text;
            const value = e.target.selectedOptions[0].value;
            this.mApplication = value;
            $.each(this._listeners, (index, listener) => {
                listener(text, value);
            });

            this.#onSelectionChanged('application', this.mApplication);
        });

        //
        // create instance selector
        //
        if (option.showInstanceSelector) {

        }
    }

    createFilter(dataSourceName) {
        this.mDataSource = dataSourceName;
        new SchemaApi().getSchema(
            dataSourceName,
            (schema) => this.createFilterFromSchema(schema)
        );
        return this;
    }

    createFilterFromSchema(schema) {
        this.mDataSource = schema.name;
        this.mSchema = schema;

        let index = 0;

        // Note: first two dimensions MUST be app/instance
        for (index = 1; index < schema.dimensionsSpec.length; index++) {
            const dimension = schema.dimensionsSpec[index];
            if (!dimension.visible)
                continue;

            this.#createDimensionFilter(index, dimension.name, dimension.displayText);
        }
    }

    #createDimensionFilter(dimensionIndex, dimensionName, displayText) {
        const appendedSelect = this.vParent.append(`<li class="nav-item"><select style="width:150px"></select></li>`).find('select').last();
        if (dimensionIndex === 1 && g_SelectedInstance != null) {
            appendedSelect.append(`<option value="${g_SelectedInstance}">${g_SelectedInstance}</option>`);
        }
        appendedSelect.select2({
            theme: 'bootstrap4',
            allowClear: true,
            dropdownAutoWidth: true,
            placeholder: displayText,
            ajax: this.getDimensionAjaxOptions(dimensionIndex, dimensionName),
        }).on('change', (event) => {
            let dimensionValue = null;
            if (event.target.selectedIndex == null || event.target.selectedIndex < 0) {
                if (dimensionIndex === 1) {
                    g_SelectedInstance = null;
                }
                this.rmvDimension(dimensionName);
            } else {
                // get selected dimension
                dimensionValue = event.target.selectedOptions[0].value;

                if (dimensionIndex === 1) {
                    g_SelectedInstance = dimensionValue;
                }
                this.addDimension(dimensionName, dimensionValue);
            }
            this.#onSelectionChanged(dimensionName, dimensionValue);
        });

        if (dimensionIndex === 1 && g_SelectedInstance != null) {
            appendedSelect.change();
        }
    }

    addDimension(dimensionName, dimensionValue) {
        this._selectedDimensions[dimensionName] = {
            dimension: dimensionName,
            matcher: {
                type: 'equal',
                pattern: dimensionValue
            }
        };
    }

    rmvDimension(dimensionName) {
        delete this._selectedDimensions[dimensionName];
    }

    /**
     * @param listener fn that accepts (dimensionName, dimensionValue)
     */
    registerChangedListener(listener) {
        this.mSelectionChangedListener.push(listener);
        return this;
    }

    getSelectedFilters() {
        const filters = [];
        $.each(this._selectedDimensions, (name, value) => {
            filters.push(value);
        });
        return filters;
    }

    #onSelectionChanged(name, value) {
        $.each(this.mSelectionChangedListener, (index, listener) => {
            listener(name, value);
        });
    }

    #getApplicationOptions() {
        return {
            cache: true,
            url: apiHost + "/api/meta/getMetadataList",
            data: JSON.stringify({type: 'APPLICATION'}),
            type: "POST",
            async: true,
            dataType: "json",
            contentType: "application/json",
            processResults: (appList) => {
                return {
                    results: appList.map(app => {
                        return {
                            "id": app.applicationName,
                            "text": app.applicationName
                        };
                    })
                };
            }
        };
    }

    // #getDimensionAjaxOptions() {
    //     return {
    //         cache: true,
    //         type: 'POST',
    //         url: apiHost + '/api/datasource/dimensions',
    //         data: () => {
    //             const filters = [{
    //                 dimension: "appName",
    //                 matcher: {
    //                     type: "equal",
    //                     pattern: this.mApplication
    //                 }
    //             }];
    //
    //             if (this.mRequestFilterFn !== undefined) {
    //                 this.mRequestFilterFn(filters);
    //             }
    //
    //             const interval = this.mIntervalProviderFn();
    //             return JSON.stringify({
    //                 dataSource: this.mDataSource,
    //                 dimension: 'instanceName',
    //                 conditions: filters,
    //                 startTimeISO8601: interval.start,
    //                 endTimeISO8601: interval.end,
    //             })
    //         },
    //         dataType: "json",
    //         contentType: "application/json",
    //         processResults: (data) => {
    //             return {
    //                 results: data.map(dimension => {
    //                     return {
    //                         "id": dimension.value,
    //                         "text": dimension.value
    //                     };
    //                 })
    //             };
    //         }
    //     }
    // }

    getDimensionAjaxOptions(dimensionIndex, dimensionName) {
        return {
            cache: true,
            type: 'POST',
            url: apiHost + '/api/datasource/dimensions',
            data: () => {
                const filters = [];

                for (let p = 0; p < dimensionIndex; p++) {
                    const dim = this.mSchema.dimensionsSpec[p];
                    if (this._selectedDimensions[dim.name] != null) {
                        filters.push(this._selectedDimensions[dim.name]);
                    }
                }
                if (this.mRequestFilterFn !== undefined) {
                    this.mRequestFilterFn(filters);
                }

                const interval = this.mIntervalProviderFn();
                return JSON.stringify({
                    dataSource: this.mDataSource,
                    dimension: dimensionName,
                    conditions: filters,
                    startTimeISO8601: interval.start,
                    endTimeISO8601: interval.end,
                })
            },
            dataType: "json",
            contentType: "application/json",
            processResults: (data) => {
                return {
                    results: data.map(dimension => {
                        return {
                            "id": dimension.value,
                            "text": dimension.value
                        };
                    })
                };
            }
        }
    }
}