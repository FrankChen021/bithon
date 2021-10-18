class TimeInterval {

    constructor(defaultInterval) {
        this._listeners = [];

        this._control = $('<select id="intervalSelector" class="form-control">       ' +
            '    <option value="1" data-unit="minute">Last 1m</option>   ' +
            '    <option value="3" data-unit="minute">Last 3m</option>   ' +
            '    <option value="5" data-unit="minute">Last 5m</option>   ' +
            '    <option value="15" data-unit="minute">Last 15m</option> ' +
            '    <option value="30" data-unit="minute">Last 30m</option> ' +
            '    <option value="1" data-unit="hour">Last 1h</option>    ' +
            '    <option value="3" data-unit="hour">Last 3h</option>    ' +
            '    <option value="6" data-unit="hour">Last 6h</option>    ' +
            '    <option value="12" data-unit="hour">Last 12h</option>  ' +
            '    <option value="24" data-unit="hour">Last 24h</option>  ' +
            '    <option value="today" data-unit="hour">Today</option>  ' +
            '</select>                                                 ');

        this._intervalFn = null;

        $(this._control).change(() => {
            const option = $(this._control).find("option:selected");
            const val = option.val();

            if (val === 'today') {
                this._intervalFn = function () {
                    return {
                        start: moment().utc().local().startOf('day').toISOString(),
                        end: moment().utc().local().toISOString(),
                        val: val
                    };
                }
            } else {
                const unit = option.attr('data-unit');
                this._intervalFn = function () {
                    return {
                        start: moment().utc().subtract(val, unit).local().toISOString(),
                        end: moment().utc().local().toISOString(),
                        val: val
                    };
                }
            }

            $.each(this._listeners, (index, listener) => {
                listener.apply();
            });
        });

        this._control.find(`option[value="${defaultInterval}"]`).attr('selected', true);

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
        return this._intervalFn.apply();
    }
}

