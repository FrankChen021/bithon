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
        this.mSelectionChangedListener = [];
        this.mLastQuery = null;
        this.mQueryCache = [];

        this._selectedDimensions = {};
        this.addDimension("appName", option.appName);
        if (this.mInstance != null) {
            this.addDimension("instanceName", this.mInstance);
            g_SelectedInstance = this.mInstance;
        }

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
            this.mApplication = e.target.selectedOptions[0].value;

            this.#onSelectionChanged('application', this.mApplication);
        });
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

        // Note: first two dimensions MUST be app/instance
        for (let index = 1; index < schema.dimensionsSpec.length; index++) {
            const dimension = schema.dimensionsSpec[index];
            if (!dimension.visible)
                continue;

            this.#createDimensionFilter(index, dimension.name, dimension.displayText);
        }
    }

    #createDimensionFilter(dimensionIndex, dimensionName, displayText) {
        const appendedSelect = this.vParent.append(`<li class="nav-item"><select style="width:150px"></select></li>`).find('select').last();
        if (dimensionIndex === 1 && this.mInstance != null) {
            appendedSelect.append(`<option value="${this.mDataSource}">${this.mInstance}</option>`).change();
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
                this.rmvDimension(dimensionName);
            } else {
                // get selected dimension
                dimensionValue = event.target.selectedOptions[0].value;
                this.addDimension(dimensionName, dimensionValue);
            }
            if (dimensionIndex === 1) {
                g_SelectedInstance = dimensionValue;
            }
            this.#onSelectionChanged(dimensionName, dimensionValue);
        });
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
            },

            // following 'transport' function is based on: https://github.com/select2/select2/issues/110#issuecomment-694495292
            //cache result transport
            transport: (params, success, failure) => {
                //retrieve the cached key or default to _ALL_
                const __cachekey = params.data.q || '_ALL_';
                if (this.mLastQuery !== __cachekey) {
                    //remove caches not from last query
                    this.mQueryCache = [];
                }
                this.mLastQuery = __cachekey;
                if ('undefined' !== typeof this.mQueryCache[__cachekey]) {
                    // if('undefined' !== typeof params.data.search){
                    //     success(__cache[__cachekey]));
                    //     return;
                    // }
                    //display the cached results
                    success(this.mQueryCache[__cachekey]);
                    return; /* noop */
                }
                const $request = $.ajax(params);
                $request.then((data) => {
                    //store data in cache
                    this.mQueryCache[__cachekey] = data;
                    //display the results
                    success(this.mQueryCache[__cachekey]);
                });
                $request.fail(failure);
                return $request;
            },
        };
    }

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