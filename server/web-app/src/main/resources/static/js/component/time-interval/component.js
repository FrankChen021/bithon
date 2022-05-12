class TimeInterval {

    constructor(defaultIntervalId, includeAll) {
        this._listeners = [];

        this._viewModel = [
            {id: "1m", value: 1, unit: "minute", text: "Last 1m"},
            {id: "3m", value: 3, unit: "minute", text: "Last 3m"},
            {id: "5m", value: 5, unit: "minute", text: "Last 5m"},
            {id: "15m", value: 15, unit: "minute", text: "Last 15m"},
            {id: "30m", value: 30, unit: "minute", text: "Last 30m"},
            {id: "1h", value: 1, unit: "hour", text: "Last 1h"},
            {id: "3h", value: 3, unit: "hour", text: "Last 3h"},
            {id: "6h", value: 6, unit: "hour", text: "Last 6h"},
            {id: "12h", value: 12, unit: "hour", text: "Last 12h"},
            {id: "24h", value: 24, unit: "hour", text: "Last 24h"},
            {id: "today", value: "today", unit: "day", text: "Today"},
            {id: "yesterday", value: "yesterday", unit: "day", text: "Yesterday"},
            {id: "custom", value: "", text: "Customer"}
            // {id: "user", value: "user", text: "Customer", start, end}
        ];

        if (defaultIntervalId !== undefined && defaultIntervalId.startsWith("c:")) {
            const interval = defaultIntervalId.substring(2);
            const parts = interval.split("/");
            const start = moment(parts[0]);
            const end = moment(parts[1]);
            this._viewModel.push({
                id: "user",
                value: "user",
                start: start,
                end: end,
                text: parts[0] + '~' + parts[1]
            });
            defaultIntervalId = "user";
        }

        if (includeAll) {
            this._viewModel.push({id: "all", value: "all", unit: "day", text: "All"});
        }

        this._control = $('<select id="intervalSelector" class="form-control"></select>');
        this._viewModel.forEach(model => {
            const option = $(`<option id="${model.id}" value="${model.value}" data-unit="${model.unit}">${model.text}</option>`);
            this._control.append(option);
            if (model.id === defaultIntervalId) {
                option.attr('selected', true);
            }
        });
        if (this.getSelectedIndex() === 0) {
            this._control.find(`option[id="5m"]`).attr('selected', true);
        }

        this._control.on('focus', () => {
            this._prevSelectIndex = this.getSelectedIndex();
        }).change(() => {
            const selectedIndex = this.getSelectedIndex();
            const selectedModel = this._viewModel[selectedIndex];

            if (selectedModel.id === 'custom') {
                this.openCustomerDateSelectorDialog(this.#toInterval(this._prevSelectIndex));
            } else {
                $.each(this._listeners, (index, listener) => {
                    listener(selectedModel);
                });
            }
        });

        // trigger the change event to load default value
        this._control.change();
    }

    openCustomerDateSelectorDialog(prevInterval) {
        bootbox.dialog({
            centerVertical: true,
            size: 'xl',
            onEscape: () => {
                // restore prev selection without triggering the selection changed event
                this._control[0].selectedIndex = this._prevSelectIndex;
            },
            backdrop: true,
            message: this.#IntervalTemplate(),
            buttons: {
                ok: {
                    label: "OK",
                    className: 'btn-info',
                    callback: () => {
                        // truncate seconds the millis seconds
                        const startTimestamp = $('#intervalPickerStart').data(tempusDominus.Namespace.dataKey).viewDate.getTime().basedOn(1000 * 60);
                        const endTimestamp = $('#intervalPickerEnd').data(tempusDominus.Namespace.dataKey).viewDate.getTime().basedOn(1000 * 60);
                        if (endTimestamp <= startTimestamp) {
                            $("#dateTimePickAlert").text('start time is less than or equal to the end time').show();
                            return false;
                        }

                        //
                        // update the UI
                        //
                        // check if there's a user item
                        const displayStart = new Date(startTimestamp).format('MM-dd hh:mm');
                        const displayEnd = new Date(endTimestamp).format('MM-dd hh:mm');
                        if (this._control[0].lastChild.id !== 'user') {
                            // no user item
                            this._control.append(`<option id="user" value="user">${displayStart} ~ ${displayEnd}</option>`);

                            this._viewModel.push({id: "user", value: "user"});
                        } else {
                            // change user item
                            this._control[0].lastChild.innerText = `${displayStart} ~ ${displayEnd}`;
                        }

                        // save value to view model
                        const s = moment(startTimestamp).local().toISOString(true);
                        const e = moment(endTimestamp).local().toISOString(true);
                        this._viewModel[this._viewModel.length - 1].start = s;
                        this._viewModel[this._viewModel.length - 1].end = e;

                        // change the selection
                        this._control[0].selectedIndex = this._viewModel.length - 1;
                        this._control.change();

                        return true;
                    }
                },
                cancel: {
                    label: "Cancel",
                    className: 'btn-cancel',
                    callback: () => {
                        // restore prev selection without triggering the selection changed event
                        this._control[0].selectedIndex = this._prevSelectIndex;
                        return true;
                    }
                }
            },
            onShown: () => {

                $('#intervalPickerStart')
                    .val(moment(prevInterval.start).local().format('yyyy-MM-DD HH:mm:ss'))
                    .tempusDominus({
                        display: {
                            components: {
                                useTwentyfourHour: true
                            },
                            sideBySide: true,
                            // inline: true,
                            keepOpen: true
                        },
                        hooks: {
                            inputFormat: (context, date) => formatDateTime(date.truncate(60000), 'yyyy-MM-dd hh:mm:ss'),
                            //inputParse: (context, value) => {}
                        }
                    });
                $('#intervalPickerEnd')
                    .val(moment(prevInterval.end).local().format('yyyy-MM-DD HH:mm:ss'))
                    .tempusDominus({
                        display: {
                            components: {
                                useTwentyfourHour: true
                            },
                            sideBySide: true,
                            //inline: true,
                            keepOpen: true
                        },
                        hooks: {
                            inputFormat: (context, date) => formatDateTime(date.truncate(60000), 'yyyy-MM-dd hh:mm:ss'),
                            //inputParse: (context, value) => {}
                        }
                    });
            },
            onHidden: () => {
                $('#intervalPickerStart').tempusDominus().hide();
                $('#intervalPickerEnd').tempusDominus().hide();
            }
        });
    }

    childOf(element) {
        $(element).append(this._control);
        return this;
    }

    registerIntervalChangedListener(listener) {
        this._listeners.push(listener);
        return this;
    }

    //PUBLIC
    getInterval() {
        return this.#toInterval(this.getSelectedIndex());
    }

    #toInterval(index) {
        const selectedModel = this._viewModel[index];
        if (selectedModel.value === 'today') {
            return {
                start: moment().utc().local().startOf('day').toISOString(),
                end: moment().utc().local().toISOString()
            };
        } else if (selectedModel.value === 'yesterday') {
            const start = moment().utc().local().startOf('day').subtract(1, 'day');
            return {
                start: start.toISOString(),
                end: start.add(1, 'day').toISOString()
            };
        } else if (selectedModel.value === 'all') {
            return {
                start: "2000-01-01T00:00:00.000Z",
                end: "2099-12-31T23:59:59.000Z"
            };
        } else if (selectedModel.value === 'user') {
            return {
                start: selectedModel.start,
                end: selectedModel.end
            }
        } else {
            return {
                start: moment().utc().subtract(selectedModel.value, selectedModel.unit).local().toISOString(),
                end: moment().utc().local().toISOString()
            };
        }
    }

    //PRIVATE
    getSelectedIndex() {
        return this._control.prop('selectedIndex');
    }

    #IntervalTemplate(start, end) {
        return '<form>\n' +
            '    <div class="form-row">\n' +
            '        <div class="col-sm-6">\n' +
            '            <label htmlFor="linkedPickers2Input" class="form-label">From</label>\n' +
            '            <div class="input-group mb-3">\n' +
            '                <input type="text" class="form-control" id="intervalPickerStart">\n' +
            '                <div class="input-group-append">\n' +
            '                    <span class="input-group-text"><i class="fa fa-calendar"></i></span>\n' +
            '                </div>\n' +
            '            </div>\n' +
            '        </div>\n' +
            '        <div class="col-sm-6">\n' +
            '            <label htmlFor="linkedPickers2Input" class="form-label">To</label>\n' +
            '            <div class="input-group mb-3">\n' +
            '                <input type="text" class="form-control" id="intervalPickerEnd">\n' +
            '                <div class="input-group-append">\n' +
            '                    <span class="input-group-text"><i class="fa fa-calendar"></i></span>\n' +
            '                </div>\n' +
            '            </div>\n' +
            '        </div>\n' +
            '    </div>\n' +
            '    <div class="form-group">\n' +
            '        <div class="alert alert-warning" id="dateTimePickAlert" style="display:none" role="alert"></div>\n' +
            '    </div>\n' +
            '</form>';
    }
}

