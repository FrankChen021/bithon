class AppSelector {

    /**
     * {
     *      appName:    <required>
     *      dataSource: <required>
     *      showInstanceSelector: true,
     *      intervalProvider: fn,
     *      parentId:   <required>
     * }
     */
    constructor(option) {
        const vParent = $('#' + option.parentId);

        this.mIntervalProviderFn = option.intervalProvider;
        this.mRequestFilterFn = option.requestFilterFn;
        this.mDataSource = option.dataSource;
        this.mApplication = option.appName;
        this.mInstance = window.queryParams !== undefined ? window.queryParams['instance'] : null;
        this._listeners = [];
        this.mSelectionChangedListener = [];

        //
        // create app selector
        //
        this.vAppSelector = $('<li class="nav-item"><select class="form-control" style="width:200px"></select></li>').find('select');
        vParent.append(this.vAppSelector);
        $(document).ready(() => {
            this.vAppSelector.select2({
                theme: 'bootstrap4',
                dropdownAutoWidth: true,
                ajax: this.#getApplicationOptions()
            });
        });

        //
        // create instance selector
        //
        if (option.showInstanceSelector) {
            this.vInstanceSelector = $(`<li class="nav-item"><select style="width:150px"></select></li>`).find('select');
            console.log(window.queryParams);
            console.log(this.mInstance);
            if (this.mInstance != null) {
                this.vInstanceSelector.append(`<option value="${this.mInstance}">${this.mInstance}</option>`);
                this.vInstanceSelector.change();
            }

            this.vInstanceSelector.on('change', (event) => {
                if (event.target.selectedIndex == null || event.target.selectedIndex < 0) {
                    this.mInstance = null;
                } else {
                    // get selected dimension
                    this.mInstance = event.target.selectedOptions[0].value;
                }

                this.#onSelectionChanged('instance', this.mInstance);
            });
            vParent.append(this.vInstanceSelector);

            $(document).ready(() => {
                this.vInstanceSelector.select2({
                    theme: 'bootstrap4',
                    allowClear: true,
                    dropdownAutoWidth: true,
                    placeholder: 'instance',
                    ajax: this.#getDimensionAjaxOptions(),
                });
            });
        }

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
    }

    /**
     * Deprecated
     * @param listener fn that accepts (text, application)
     */
    registerAppChangedListener(listener) {
        this._listeners.push(listener);
        return this;
    }

    /**
     * @param listener fn that accepts (dimensionName, dimensionValue)
     */
    registerChangedListener(listener) {
        this.mSelectionChangedListener.push(listener);
        return this;
    }

    getSelectedFilters() {
        const filters = [{
            dimension: 'appName',
            matcher: {
                type: 'equal',
                pattern: this.mApplication
            }
        }];
        if (this.mInstance != null) {
            filters.push({
                dimension: 'instanceName',
                matcher: {
                    type: 'equal',
                    pattern: this.mInstance
                }
            });
        }
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

    #getDimensionAjaxOptions() {
        return {
            cache: true,
            type: 'POST',
            url: apiHost + '/api/datasource/dimensions',
            data: () => {
                const filters = [{
                    dimension: "appName",
                    matcher: {
                        type: "equal",
                        pattern: this.mApplication
                    }
                }];

                if (this.mRequestFilterFn !== undefined) {
                    this.mRequestFilterFn(filters);
                }

                const interval = this.mIntervalProviderFn();
                return JSON.stringify({
                    dataSource: this.mDataSource,
                    dimension: 'instanceName',
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