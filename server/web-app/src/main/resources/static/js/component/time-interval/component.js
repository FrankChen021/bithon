class TimeInterval {

    constructor(option) {
        this._listeners = [];

        this._control = $('<select id="intervalSelector" class="form-control">       ' +
            '    <option value="1" data-unit="minute">Last 1m</option>   ' +
            '    <option value="3" data-unit="minute">Last 3m</option>   ' +
            '    <option value="5" data-unit="minute" selected>Last 5m</option>   ' +
            '    <option value="15" data-unit="minute">Last 15m</option> ' +
            '    <option value="30" data-unit="minute">Last 30m</option> ' +
            '    <option value="1" data-unit="hour">Last 1h</option>    ' +
            '    <option value="3" data-unit="hour">Last 3h</option>    ' +
            '    <option value="6" data-unit="hour">Last 6h</option>    ' +
            '    <option value="12" data-unit="hour">Last 12h</option>  ' +
            '    <option value="24" data-unit="hour">Last 24h</option>  ' +
            '    <option value="today" data-unit="hour">Today</option>  ' +
            '</select>                                                 ');

        $(this._control).change(() => {
            const option = $(this._control).find("option:selected");
            const val = option.val();

            let fn;
            if ( val === 'today') {
                fn = function () {
                    return {
                        start: moment().utc().local().startOf('day').toISOString(),
                        end: moment().utc().local().toISOString()
                    }
                };
            } else {
                const unit = option.attr('data-unit');
                fn = function () {
                    return {
                        start: moment().utc().subtract(val, unit).local().toISOString(),
                        end: moment().utc().local().toISOString()
                    }
                };
            }
            $.each(this._listeners, (index, listener) => {
                listener(fn);
            });
        });
    }

    childOf(element) {
        $(element).append(this._control);
        return this;
    }

    registerIntervalChangedListener(listener) {
        this._listeners.push(listener);
    }
}