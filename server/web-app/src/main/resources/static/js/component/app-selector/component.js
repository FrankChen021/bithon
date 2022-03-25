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
        this.mSelectionChangedListener = [];
        this.mLastQuery = null;
        this.mQueryCache = [];
        this.mQueryVariablepPrefix = option.queryVariablePrefix || '';

        this.mSelectedFilters = {};
    }

    createAppSelector(appName) {
        this.#addFilter("appName", appName);

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

        this.vAppSelector.append(`<option value="${appName}">${appName}</option>`).change();

        this.vAppSelector.change((e) => {
            const application = e.target.selectedOptions[0].value;
            this.#onSelectionChanged('application', application);
        });

        return this;
    }

    createFilter(dataSourceName) {
        this.mDataSource = dataSourceName;
        new SchemaApi().getSchema({
            name: dataSourceName,
            async: false,
            successCallback: (schema) => this.createFilterFromSchema(schema)
        });
        return this;
    }

    createFilterFromSchema(schema) {
        this.mDataSource = schema.name;
        this.mSchema = schema;

        // Note: first two dimensions MUST be app/instance
        for (let index = 0; index < schema.dimensionsSpec.length; index++) {
            const dimension = schema.dimensionsSpec[index];
            if (!dimension.visible)
                continue;

            if (index === 0 && dimension.alias === 'appName') {
                // for appName filter, createAppSelector should be explicitly called
                continue;
            }

            this.#createDimensionFilter(index, dimension.alias, dimension.displayText);
        }
    }

    #createDimensionFilter(dimensionIndex, dimensionName, displayText) {
        const filterName = this.mQueryVariablepPrefix + dimensionName;

        // create selector
        const appendedSelect = this.vParent.append(`<li class="nav-item"><select style="width:150px"></select></li>`).find('select').last();

        // bind query params if applicable
        const queryValue = window.queryParams[filterName];
        if (queryValue != null) {
            appendedSelect.append(`<option value="${this.mDataSource}">${queryValue}</option>`).change();

            this.#addFilter(filterName, queryValue);
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
                this.#rmvFilter(filterName);
            } else {
                // get selected dimension
                dimensionValue = event.target.selectedOptions[0].value;
                this.#addFilter(filterName, dimensionValue);
            }
            this.#onSelectionChanged(filterName, dimensionValue);
        });
    }

    #addFilter(dimensionName, dimensionValue) {
        this.mSelectedFilters[dimensionName] = {
            dimension: dimensionName,
            type: 'alias',
            matcher: {
                type: 'equal',
                pattern: dimensionValue
            }
        };
    }

    #rmvFilter(dimensionName) {
        delete this.mSelectedFilters[dimensionName];
    }

    /**
     * @param listener fn that accepts (dimensionName, dimensionValue)
     */
    registerChangedListener(listener) {
        this.mSelectionChangedListener.push(listener);
        return this;
    }

    /**
     * @returns an array of filters
     */
    getSelectedFilters() {
        const filters = [];
        $.each(this.mSelectedFilters, (name, value) => {
            filters.push(value);
        });
        return filters;
    }

    getSelectedFilter(name) {
        return this.mSelectedFilters[name];
    }

    #onSelectionChanged(name, value) {
        if (name === 'instanceName') {
            g_SelectedInstance = value;
        }
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
            url: apiHost + '/api/datasource/dimensions/v2',
            data: () => {
                const filters = [];

                for (let p = 0; p < dimensionIndex; p++) {
                    const dim = this.mSchema.dimensionsSpec[p];
                    if (this.mSelectedFilters[dim.name] != null) {
                        filters.push(this.mSelectedFilters[dim.name]);
                    }
                }
                if (this.mRequestFilterFn !== undefined) {
                    this.mRequestFilterFn(filters);
                }

                const interval = this.mIntervalProviderFn();
                return JSON.stringify({
                    dataSource: this.mDataSource,
                    name: dimensionName,
                    filters: filters,
                    type: "alias",
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