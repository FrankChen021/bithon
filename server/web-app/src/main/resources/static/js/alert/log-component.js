class LogComponent {

    constructor(option) {
        $('#' + option.containerId).html('<div class="btn-group btn-group-sm" role="group" aria-label="...">' +
            '  <button class="btn btn-default btn-primary" id="log-minute-5" data-minute="5">5 Minutes</button>' +
            '  <button class="btn btn-default" id="log-minute-60" data-minute="60">1 Hour</button>' +
            '  <button class="btn btn-default" id="log-minute-180" data-minute="180">3 Hour</button>' +
            '  <button class="btn btn-default" id="log-minute-720" data-minute="720">12 Hour</button>' +
            '</div>' +
            '<div style="padding-top:10px">' +
            '  <div class="alert text-center" role="alert"></div><pre style="display:none"></pre>' +
            '</div>');

        this._containerId = '#' + option.containerId;
        this._selected = "5";
        if (option.showToolbar) {
            $(this._containerId).find('button').click((e) => {
                $(this._containerId).find('#log-minute-' + this._selected).removeClass('btn-primary');
                this._selected = $(e.target).attr('data-minute');
                $(this._containerId).find('#log-minute-' + this._selected).addClass('btn-primary');

                this.loadLatestLogs(this.alertId);
            });
        } else {
            $(this._containerId).find('.btn-group').css('display', 'none');
        }
    }

    loadLatestLogs(alertId) {
        this.alertId = alertId;
        const end = Date.now();
        const start = end - parseInt(this._selected) * 60 * 1000;
        this.loadLogs(this.alertId, start, end);
    }

    loadLogs(alertId, start, end) {
        $(this._containerId).find('.alert').css('display', '').text('Loading...');

        this.alertId = alertId;
        $.ajax({
            type: "POST",
            url: "/api/alerting/alert/evaluation-log/get",
            async: true,
            data: JSON.stringify({
                alertId: alertId,
                interval: {
                    startISO8601: moment(start).local().toISOString(true),
                    endISO8601: moment(end).local().toISOString(true)
                }
            }),
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            success: (data) => {
                if (data.total <= 0) {
                    $(this._containerId).find('.alert').text('No logs（Notes: logs is kept up to 1 day）');
                    return;
                }

                $(this._containerId).find('.alert').css('display', 'none');

                let minute = 0;
                let logContent = '';
                for (let i = 0; i < data.rows.length; i++) {
                    const item = data.rows[i];

                    const thisMinute = Math.trunc(item.timestamp / 1000 / 60);
                    if (minute !== thisMinute) {
                        if (minute !== 0) {
                            // Insert a line break between two logs where the minute of timestamp are different
                            // So that the logs with the same minute are shown together as a block
                            logContent += '\n';
                        }
                        minute = thisMinute;
                    }

                    logContent += new Date(item.timestamp).format('yyyy-MM-dd hh:mm:ss.S')
                               + ' [' + item.instance + ']'
                               + ' [' + this.truncateOrPad(item.clazz, 24) + '] '
                               + item.message + '\n';
                }
                const container = $(this._containerId).find('pre');
                $(container).html(logContent).css('display', '');
                if ($(container).height() > 600) {
                    $(container).height(600);
                }
            },
            error: (data) => {
                console.log(data);
                $(this._containerId).find('.alert').addClass('alert-warning').text(data.responseJSON.message);
            }
        });
    }

    truncateOrPad(inputString, length) {
        if (inputString.length < length) {
            // Left pad with spaces
            return inputString.padStart(length, ' ');
        } else if (inputString.length > length) {
            // Truncate extra characters at the left
            return inputString.slice(-length);
        } else {
            // Return the string unchanged
            return inputString;
        }
    }
}
