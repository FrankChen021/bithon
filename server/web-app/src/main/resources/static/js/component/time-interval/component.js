class TimeInterval {

    constructor(defaultInterval) {
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
        ];

        this._control = $('<select id="intervalSelector" class="form-control"></select>');
        this._viewModel.forEach(model => {
            const option = $(`<option id="${model.id}" value="${model.value}" data-unit="${model.unit}">${model.text}</option>`);
            this._control.append(option);
            if (model.id === defaultInterval) {
                option.attr('selected', true);
            }
        });
        if (this.getSelectedIndex() === 0) {
            this._control.find(`option[id="5m"]`).attr('selected', true);
        }

        this._control.change(() => {
            const selectedIndex = this.getSelectedIndex();
            const selectedModel = this._viewModel[selectedIndex];
            $.each(this._listeners, (index, listener) => {
                listener(selectedModel);
            });
        });

        // trigger the change event to load default value
        this._control.change();
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
        const selectedModel = this._viewModel[this.getSelectedIndex()];

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
}

