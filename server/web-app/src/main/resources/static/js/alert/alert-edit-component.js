class AlertEditComponent {

    constructor(option) {
        this.#initializeComponent($(option.containerSelector));
        this.expressionDashboard = new ExpressionDashboardComponent('metricSection');
    }

    #initializeComponent(container) {
        const component =
            '<div class="input-group">\n' +
            '            <div class="input-group-prepend">\n' +
            '                <span class="input-group-text" style="width: 160px"><b>name</b></span>\n' +
            '            </div>\n' +
            '            <input class="form-control" id="name" placeholder="alert name. Must be unique."/>\n' +
            '        </div>\n' +
            '        <div class="input-group">\n' +
            '            <div class="input-group-prepend">\n' +
            '                <span class="input-group-text" style="width: 160px"><b>application</b></span>\n' +
            '            </div>\n' +
            '            <select class="form-control" id="appName">\n' +
            '            </select>\n' +
            '        </div>\n' +
            '        <div class="input-group">\n' +
            '            <div class="input-group-prepend">\n' +
            '                <span class="input-group-text" style="width: 160px"><b>expression</b></span>\n' +
            '            </div>\n' +
            '            <input class="form-control" id="expression"/>\n' +
            '            <div class="input-group-append">\n' +
            '                <button class="btn btn-outline-secondary" id="parse" type="button">Parse</button>\n' +
            '            </div>\n' +
            '        </div>\n' +
            '        <div class="input-group">\n' +
            '            <div class="input-group-prepend">\n' +
            '                <span class="input-group-text" style="width: 160px"><b>for</b></span>\n' +
            '            </div>\n' +
            '            <input class="form-control" id="for" value="3" placeholder="Consecutive times the alert expression is evaluated to be true before firing the alert" />\n' +
            '        </div>\n' +
            '        <div class="input-group">\n' +
            '            <div class="input-group-prepend">\n' +
            '                <span class="input-group-text" style="width: 160px"><b>every</b></span>\n' +
            '            </div>\n' +
            '            <input class="form-control" id="every" value="1"/>\n' +
            '            <div class="input-group-append">\n' +
            '                <select id="everyUnit" class="form-control">\n' +
            '                    <option selected value="m">Minute</option>\n' +
            '                    <option value="h">Hour</option>\n' +
            '                </select>\n' +
            '            </div>\n' +
            '        </div>\n' +
            '        <div class="input-group">\n' +
            '            <div class="input-group-prepend">\n' +
            '                <span class="input-group-text" style="width: 160px"><b>notification</b></span>\n' +
            '            </div>\n' +
            '            <select class="form-control" id="notifications"></select>\n' +
            '        </div>\n' +
            '        <div id="metricSection"></div>';

        container.prepend(component);

        // Bind event handler
        $('#expression').keydown((e)=>{
            if (e.keyCode === 13) {
                this.#renderExpression(true);
            }
        });
        $('#parse').click(() => {
            this.#renderExpression(true);
        });

        // UI Initialization
        $('#appName').select2({
            theme: 'bootstrap4',
            allowClear: true,
            dropdownAutoWidth: true,
            placeholder: '(optional) select application',
        }).on('change', () => {
            const expr = $('#expression').val();
            if (expr === '') {
                return;
            }
            this.#renderExpression(false);
        });

        $('#notifications').select2({
            theme: 'bootstrap4',
            allowClear: true,
            multiple: true,
            dropdownAutoWidth: true,
            placeholder: 'select notification channel'
        });

        this.#loadApplicationList();
        this.#loadNotificationChannels();
    }

    getAlertObject() {
        const name = $("#name").val().trim();
        const appName = $("#appName").val().trim();
        const expression = $("#expression").val().trim();
        const every = $("#every").val().trim();
        const forValue = $("#for").val().trim();
        const notification = $("#notifications").select2('data');

        if (name === '') {
            bootbox.alert({
                backdrop: true,
                title: "Error",
                message: 'Name cannot be empty'
            });
            return;
        }
        if (expression === '') {
            bootbox.alert({
                backdrop: true,
                title: "Error",
                message: 'expression cannot be empty'
            });
            return;
        }
        if (every === '') {
            bootbox.alert({
                backdrop: true,
                title: "Error",
                message: '\'every\' cannot be empty'
            });
            return;
        }
        if (forValue === '') {
            bootbox.alert({
                backdrop: true,
                title: "Error",
                message: '\'for\' cannot be empty'
            });
            return;
        }
        if (notification === undefined || notification.length === 0) {
            bootbox.alert({
                backdrop: true,
                title: "Error",
                message: '\'notification\' cannot be empty'
            });
            return;
        }
        return {
            name: name,
            appName: appName,
            expr: expression,
            every: every + $('#everyUnit').val(),
            for: forValue,
            notifications: notification.map((n) => n.text)
        };
    }

    setAlertObject(alert) {
        $('#name').val(alert.name);
        $('#appName').val(alert.appName);
        $('#expression').val(alert.payload.expr);
        $('#for').val(alert.payload.for);
        $('#every').val(alert.payload.every);
        $('#notifications').val(alert.payload.notifications).trigger('change');

        this.expressionDashboard.renderExpression(alert.payload.expr);
    }

    #renderExpression(checkApp) {
        const expr = $('#expression').val();
        const appName = $('#appName').val();
        if (checkApp && expr === '') {
            bootbox.alert({
                backdrop: true,
                title: "Error",
                message: '<pre>expression is not provided</pre>'
            });
            return;
        }

        this.expressionDashboard.renderExpression(
            expr,
            appName,
        );
    }

    #loadApplicationList() {
        $.ajax({
            url: '/api/meta/getMetadataList',
            method: 'POST',
            dataType: 'json',
            data: JSON.stringify({type: 'APPLICATION'}),
            async: true,
            contentType: "application/json",
            success: (data) => {
                const applicationList = data.map(app => {
                    return {
                        "id": app.applicationName,
                        "text": app.applicationName,
                        "search": app.applicationName.toLowerCase()
                    };
                });
                $('#appName').select2({
                    data: applicationList
                });
            },

            error: (data) => {
                bootbox.alert({
                    backdrop: true,
                    title: "Error",
                    message: data.responseText
                });
            }
        });
    }

    #loadNotificationChannels() {
        $.ajax({
            url: apiHost + "/api/alerting/channel/names",
            method: 'POST',
            dataType: 'json',
            async: true,
            contentType: "application/json",
            success: (data) => {
                const channelList = data.channels.map((channel) => {
                    return {
                        id: channel,
                        text: channel,
                        search: channel
                    }
                })

                $('#notifications').select2({
                    data: channelList
                });
            },

            error: (data) => {
                bootbox.alert({
                    backdrop: true,
                    title: "Error",
                    message: data.responseText
                });
            }
        });
    }
}
