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

        //
        // Create select control
        //
        let selected = false
        this._control = $('<select id="intervalSelector" class="form-control"></select>');
        this._viewModel.forEach(model => {
            const option = this.#addIntervalOption(model.id, model.value, model.unit, model.text);
            if (model.id === defaultIntervalId) {
                option.attr('selected', true);
                selected = true;
            }
        });
        if (!selected) {
            // If the given interval is not in the predefined list, set it to a default one
            this._control.find('option[id="P5M"]').attr('selected', true);
        }

        //
        // Bind event handlers
        //
        this._control.on('focus', () => {
            this._prevSelectIndex = this.getSelectedIndex();
        }).change(() => {
            const selectedIndex = this.getSelectedIndex();
            const selectedModel = this._viewModel[selectedIndex];

            if (selectedModel.id === 'input') {
                this.#openCustomerDateSelectorDialog(this._prevSelectIndex, selectedIndex);
            } else {
                const interval = this.#toInterval(selectedIndex);
                $.each(this._listeners, (index, listener) => {
                    listener(interval);
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
                        // Truncate milliseconds
                        const startTimestamp = $('#intervalPickerStart').data(tempusDominus.Namespace.dataKey).viewDate.getTime().basedOn(1000);
                        const endTimestamp = $('#intervalPickerEnd').data(tempusDominus.Namespace.dataKey).viewDate.getTime().basedOn(1000);
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
                                // Only allow appending 10 items, if the number exceeds, change the last one
                                index = this._viewModel.length - 2;
                            }
                        }

                        // Change UI displayed content
                        this._control[0].children[index].innerText = `${displayStart} ~ ${displayEnd}`;

                        // save value to the view model
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

    /*
     * Callback object:
     * {
     *    id:
     *    start:
     *    end
     * }
     */
    registerIntervalChangedListener(listener) {
        this._listeners.push(listener);
        return this;
    }

    /*
     * P5M/P1H
     * YYYY-MM-DD HH:mm:ss/YYYY-MM-DD HH:mm:ss
     */
    getSelectedId() {
        return this._viewModel[index].id;
    }

    /**
     * {
     *      id:
     *      start: ISO8601 string
     *      end: ISO8601 string
     * }
     */
    getInterval() {
        return this.#toInterval(this.getSelectedIndex());
    }

    #toInterval(index) {
        const selectedModel = this._viewModel[index];
        if (selectedModel.value === 'today') {
            return {
                id: selectedModel.id,
                start: moment().utc().local().startOf('day').toISOString(),
                end: moment().utc().local().toISOString()
            };
        } else if (selectedModel.value === 'yesterday') {
            const start = moment().utc().local().startOf('day').subtract(1, 'day');
            return {
                id: selectedModel.id,
                start: start.toISOString(),
                end: start.add(1, 'day').toISOString()
            };
        } else if (selectedModel.value === 'all') {
            return {
                id: 'c:2000-01-01T00:00:00.000Z/2099-12-31T23:59:59.000Z',
                start: '2000-01-01T00:00:00.000Z',
                end: '2099-12-31T23:59:59.000Z'
            };
        } else if (selectedModel.value === 'user') {
            return {
                id: `c:${selectedModel.start}/${selectedModel.end}`,
                start: selectedModel.start,
                end: selectedModel.end
            }
        } else {
            const s = moment().utc().subtract(selectedModel.value, selectedModel.unit).local().toISOString();
            const e = moment().utc().local().toISOString();
            return {
                id: selectedModel.id,
                start: s,
                end: e
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

    setInterval(startTimestamp, endTimestamp) {
        const startISO8601 = moment(startTimestamp).utc().local().toISOString(true);
        const endISO8601 = moment(endTimestamp).utc().local().toISOString(true);
        const currentSelect = this.getInterval();
        if (startISO8601 === currentSelect.start && endISO8601 === currentSelect.end) {
            return;
        }

        const displayText = this.#formatDisplayText(startTimestamp, endTimestamp);

        if (this._viewModel.length - this.vBuiltInIntervalCount < 10) {
            $(`<option id="user">${displayText}</option>`).insertBefore(this.vUserInputOption);
            this._viewModel.splice(this._viewModel.length - 1,  0,{id: "user", value: "user"});
        } else {
            // If the user's inputs reach the limit, change the last one
            // Remember that the last editable one is actually index - 1
        }
        const index = this._viewModel.length - 2;

        // Change view model
        this._viewModel[index].start = startISO8601;
        this._viewModel[index].end = endISO8601;

        // Change UI displayed content
        this._control[0].children[index].innerText = displayText;

        // Change UI selection
        this._control[0].selectedIndex = index;
        this._control.change();
    }

    /**
     * @param start timestamp
     * @param end timestamp
     * @return string
     */
    #formatDisplayText(start, end) {
        const displayStart = new Date(start).format('MM-dd hh:mm:ss');
        const displayEnd = new Date(end).format('MM-dd hh:mm:ss');
        return `${displayStart} ~ ${displayEnd}`;
    }

    setIntervalById(intervalId) {

    }
}

