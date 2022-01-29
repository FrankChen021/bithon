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

        this.mIntervalProvider = option.intervalProvider;
        this.mDataSource = option.dataSource;
        this.mAppName = option.appName;
        this._listeners = [];

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
            if (g_SelectedInstance != null) {
                this.vInstanceSelector.append(`<option value="${g_SelectedInstance}">${g_SelectedInstance}</option>`);
                this.vInstanceSelector.change();
            }

            this.vInstanceSelector.on('change', (event) => {
                if (event.target.selectedIndex == null || event.target.selectedIndex < 0) {
                    g_SelectedInstance = null;
                    return;
                }

                // get selected dimension
                g_SelectedInstance = event.target.selectedOptions[0].value;
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

        this.vAppSelector.append(`<option value="${this.mAppName}">${this.mAppName}</option>`).change();

        this.vAppSelector.change((e) => {
            const text = e.target.selectedOptions[0].text;
            const value = e.target.selectedOptions[0].value;
            this.mAppName = value;
            $.each(this._listeners, (index, listener) => {
                listener(text, value);
            });
        });
    }

    registerAppChangedListener(listener) {
        this._listeners.push(listener);
        return this;
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
                        pattern: this.mAppName
                    }
                }];

                const interval = this.mIntervalProvider();
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