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
        this.mLastQuery = {};
        this.mQueryCache = [];
        this.mQueryVariablepPrefix = option.queryVariablePrefix || '';

        this.mFilterNames = [];

        this.mSelectedFilters = {};
    }

    createAppSelector() {
        g_SelectedApp = window.queryParams['appName'];
        //
        // create app selector
        //
        this.vAppSelector = $('<li class="nav-item"><select class="form-control" style="width:200px"></select></li>').find('select');
        this.vParent.append(this.vAppSelector);
        $(document).ready(() => {
            this.vAppSelector.select2({
                theme: 'bootstrap4',
                dropdownAutoWidth: true,
                placeholder: 'select application',
                ajax: this.#getApplicationOptions()
            });
        });

        if (g_SelectedApp !== undefined) {
            this.vAppSelector.append(`<option value="${g_SelectedApp}">${g_SelectedApp}</option>`).change();
            this.#addFilter("appName", g_SelectedApp);
        }

        this.vAppSelector.change((e) => {
            const application = e.target.selectedOptions[0].value;
            g_SelectedApp = application;
            this.#addFilter("appName", application);
            this.#onSelectionChanged('application', application);
        });

        return this;
    }

    createFilter(dataSourceName, keepAppFilter = false) {
        this.mDataSource = dataSourceName;
        new SchemaApi().getSchema({
            name: dataSourceName,
            async: false,
            successCallback: (schema) => this.createFilterFromSchema(schema, keepAppFilter)
        });
        return this;
    }

    createFilterFromSchema(schema, keepAppFilter = false) {
        this.mDataSource = schema.name;
        this.mSchema = schema;

        // Note: first two dimensions MUST be app/instance
        for (let index = 0; index < schema.dimensionsSpec.length; index++) {
            const dimension = schema.dimensionsSpec[index];
            if (!dimension.visible)
                continue;

            if (index === 0 && dimension.alias === 'appName' && !keepAppFilter) {
                // for appName filter, createAppSelector should be explicitly called
                continue;
            }

            this.#createDimensionFilter(index, dimension.alias, dimension.displayText);
        }
    }

    getFilterName() {
        return this.mFilterNames;
    }

    #createDimensionFilter(dimensionIndex, dimensionName, displayText) {
        this.mFilterNames.push(dimensionName);

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
            type: 'dimension',
            nameType: 'alias',
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
            data: (params) => {
                return {type: 'APPLICATION', search: params.term};
            },
            type: "POST",
            async: true,
            dataType: "json",
            contentType: "application/json",

            transport: (params, success, failure) => this.#getAndCache('applications',
                params,
                success,
                failure,
                (appList) => {
                    return {
                        results: appList.map(app => {
                            return {
                                "id": app.applicationName,
                                "text": app.applicationName
                            };
                        })
                    };
                }),
        };
    }

    // following 'transport' function is based on: https://github.com/select2/select2/issues/110#issuecomment-694495292
    #getAndCache(cacheName, params, success, failure, processResults) {
        const search = params.data['search'];
        delete params.data['search'];
        params.data = JSON.stringify(params.data);

        if (this.mLastQuery[cacheName] !== params.data) {
            // remove cache for this type
            delete this.mQueryCache[cacheName];
        }
        this.mLastQuery[cacheName] = params.data;

        if ('undefined' !== typeof this.mQueryCache[cacheName]) {
            success(this.#filterUserInput(search, this.mQueryCache[cacheName]));
            return; /* noop */
        }

        const $request = $.ajax(params);
        $request.then((data) => {
            if (processResults !== undefined) {
                data = processResults(data);
            }

            //store data in cache
            this.mQueryCache[cacheName] = data;

            //display the results
            success(this.mQueryCache[cacheName]);
        });
        $request.fail(failure);
        return $request;
    }

    getDimensionAjaxOptions(dimensionIndex, dimensionName) {
        return {
            cache: true,
            type: 'POST',
            url: apiHost + '/api/datasource/dimensions/v2',
            data: (params) => {
                const filters = [];

                for (let p = 0; p < dimensionIndex; p++) {
                    const dim = this.mSchema.dimensionsSpec[p];
                    const dimFilter = this.mSelectedFilters[this.mQueryVariablepPrefix + dim.alias];
                    if (dimFilter != null) {
                        filters.push({
                            // use name instead of alias to query dimensions
                            dimension: dim.name,
                            matcher: dimFilter.matcher
                        });
                    }
                }

                // merge user-provided filters
                if (this.mRequestFilterFn !== undefined) {
                    this.mRequestFilterFn(filters);
                }

                const interval = this.mIntervalProviderFn();
                return {
                    search: params.term,

                    dataSource: this.mDataSource,
                    name: dimensionName,
                    filters: filters,
                    type: "alias",
                    startTimeISO8601: interval.start,
                    endTimeISO8601: interval.end
                };
            },
            dataType: "json",
            contentType: "application/json",

            transport: (params, success, failure) => this.#getAndCache(dimensionName,
                params,
                success,
                failure,
                (data) => {
                    return {
                        results: data.map(dimension => {
                            return {
                                "id": dimension.value,
                                "text": dimension.value
                            };
                        })
                    };
                }),
        }
    }

    /**
     * @param search keyword of the search
     * @param data { results: [] }
     */
    #filterUserInput(search, data) {
        if (search === undefined || search == null || search.length === 0) {
            return data;
        }

        const newItems = [];
        for (let i = 0; i < data.results.length; i++) {
            const item = data.results[i];
            if (item.text.indexOf(search) > -1) {
                newItems.push(item);
            }
        }

        // because the 'data' has been cached, we can't change it
        // So, return a new object instead
        return {results: newItems};
    }
}