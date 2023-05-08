class TimeInterval {

    constructor(defaultIntervalId, includeAll) {
        this._listeners = [];

        this._viewModel = [
            {id: "P1M", value: 1, unit: "minute", text: "Last 1m"},
            {id: "P3M", value: 3, unit: "minute", text: "Last 3m"},
            {id: "P5M", value: 5, unit: "minute", text: "Last 5m"},
            {id: "P15M", value: 15, unit: "minute", text: "Last 15m"},
            {id: "P30M", value: 30, unit: "minute", text: "Last 30m"},
            {id: "P1H", value: 1, unit: "hour", text: "Last 1h"},
            {id: "P3H", value: 3, unit: "hour", text: "Last 3h"},
            {id: "P6H", value: 6, unit: "hour", text: "Last 6h"},
            {id: "P12H", value: 12, unit: "hour", text: "Last 12h"},
            {id: "P1D", value: 24, unit: "hour", text: "Last 1d"},
            {id: "P2D", value: 48, unit: "hour", text: "Last 2d"},
            {id: "P3D", value: 72, unit: "hour", text: "Last 3d"},
            {id: "P5D", value: 120, unit: "hour", text: "Last 5d"},
            {id: "P7D", value: 144, unit: "hour", text: "Last 7d"},
            {id: "today", value: "today", unit: "day", text: "Today"},
            {id: "yesterday", value: "yesterday", unit: "day", text: "Yesterday"},
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
                text: this.#formatDisplayText(start.valueOf(), end.valueOf())
            });
            defaultIntervalId = "user";
        }

        if (includeAll) {
            this._viewModel.push({id: "all", value: "all", unit: "day", text: "All"});
        }

        // Must be the last one
        this._viewModel.push({id: "input", value: "", text: "Customization"});

        this._control = $('<select id="intervalSelector" class="form-control"></select>');
        this._viewModel.forEach(model => {
            const option = this.#addIntervalOption(model.id, model.value, model.unit, model.text);
            if (model.id === defaultIntervalId) {
                option.attr('selected', true);
            }
        });
        if (this.getSelectedIndex() === 0) {
            // If the given interval is not in the predefined list, set it the a default one
            this._control.find('option[id="P5M"]').attr('selected', true);
        }

        this._control.on('focus', () => {
            this._prevSelectIndex = this.getSelectedIndex();
        }).change(() => {
            const selectedIndex = this.getSelectedIndex();
            const selectedModel = this._viewModel[selectedIndex];

            if (selectedModel.id === 'input') {
                this.#openCustomerDateSelectorDialog(this._prevSelectIndex, selectedIndex);
            } else {
                $.each(this._listeners, (index, listener) => {
                    listener(selectedModel);
                });
            }
        });

        this.vBuiltInIntervalCount = this._viewModel.length;
        this.vUserInputOption = this._control.find(`option[id='input']`);
    }

    #openCustomerDateSelectorDialog(preIndex, currIndex) {
        const prevInterval = this.#toInterval(preIndex);
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
                        let index = preIndex;

                        // check if there's a user item
                        const displayStart = new Date(startTimestamp).format('MM-dd hh:mm:ss');
                        const displayEnd = new Date(endTimestamp).format('MM-dd hh:mm:ss');
                        if (this._viewModel[preIndex].id !== 'user') {
                            if (this._viewModel.length - this.vBuiltInIntervalCount < 10) {
                                $(`<option id="user" value="user"></option>`).insertBefore(this.vUserInputOption);
                                this._viewModel.splice(this._viewModel.length - 1, 0, {id: "user", value: "user"});

                                index = this._viewModel.length - 2;
                            } else {
                                // Only allow to append 10 items, if the number exceeds, change the last one
                                index = this._viewModel.length - 2;
                            }
                        }

                        // Change UI displayed content
                        this._control[0].children[index].innerText = `${displayStart} ~ ${displayEnd}`;

                        // save value to view model
                        this._viewModel[index].start = moment(startTimestamp).local().toISOString(true);
                        this._viewModel[index].end = moment(endTimestamp).local().toISOString(true);

                        // change the selection
                        this._control[0].selectedIndex = index;
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
                            // truncate millisecond
                            inputFormat: (context, date) => formatDateTime(date.truncate(1000), 'yyyy-MM-dd hh:mm:ss'),
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
                            // truncate millisecond
                            inputFormat: (context, date) => formatDateTime(date.truncate(1000), 'yyyy-MM-dd hh:mm:ss'),
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

    #addIntervalOption(id, value, unit, text) {
        const option = $(`<option id="${id}" value="${value}" data-unit="${unit}">${text}</option>`);
        this._control.append(option);
        return option;
    }

    setInternal(startTimestamp, endTimestamp) {
        const displayText = this.#formatDisplayText(startTimestamp, endTimestamp);

        if (this._viewModel.length - this.vBuiltInIntervalCount < 10) {
            $(`<option id="user">${displayText}</option>`).insertBefore(this.vUserInputOption);
            this._viewModel.splice(this._viewModel.length - 1,  0,{id: "user", value: "user"});
        } else {
            // If the user inputs reaches the limit, change the last one
            // Remember that the last editable one is actually index - 1
        }
        const index = this._viewModel.length - 2;

        // Change view model
        this._viewModel[index].start = moment(startTimestamp).utc().local().toISOString(true);
        this._viewModel[index].end = moment(endTimestamp).utc().local().toISOString(true);

        // Change UI displayed content
        this._control[0].children[index].innerText = displayText;

        // Change UI selection
        this._control[0].selectedIndex = index;
        this._control.change();
    }

    /**
     * @param start timestamp
     * @param end timestamp
     * @return text
     */
    #formatDisplayText(start, end) {
        const displayStart = new Date(start).format('MM-dd hh:mm:ss');
        const displayEnd = new Date(end).format('MM-dd hh:mm:ss');
        return `${displayStart} ~ ${displayEnd}`;
    }
}

