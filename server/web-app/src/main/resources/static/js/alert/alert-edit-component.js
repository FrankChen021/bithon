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
            '            <div class="custom-file"\n' +
            '                 style="display: inline-block"> <!--Override .input-group>.custom-file -->\n' +
            '                <input class="form-control" id="expression"/>\n' +
            '            </div>' +
            '            <div class="input-group-append">\n' +
            '                <button class="btn btn-outline-secondary" id="parse" type="button">Parse</button>\n' +
            '            </div>\n' +
            '        </div>\n' +
            '        <div class="input-group">\n' +
            '            <div class="input-group-prepend">\n' +
            '                <span class="input-group-text" style="width: 160px"><b>every</b></span>\n' +
            '            </div>\n' +
            '            <input class="form-control" id="every" value="1" placeholder="The interval between two evaluations. Maximum is 24 hour"/>\n' +
            '            <div class="input-group-append">\n' +
            '                <select id="everyUnit" class="form-control">\n' +
            '                    <option selected value="m">Minute</option>\n' +
            '                    <option value="h">Hour</option>\n' +
            '                </select>\n' +
            '            </div>\n' +
            '        </div>\n' +
            '        <div class="input-group">\n' +
            '            <div class="input-group-prepend">\n' +
            '                <span class="input-group-text" style="width: 160px"><b>for</b></span>\n' +
            '            </div>\n' +
            '            <input class="form-control" id="for" value="3" placeholder="Consecutive times the alert expression is evaluated to be true before firing the alert" />\n' +
            '            <div class="input-group-append">\n' +
            '                <span class="input-group-text">Times</span>\n' +
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
        this.#initializeAutocomplete();
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

        $('#every').val(alert.payload.every.substring(0, alert.payload.every.length - 1));
        $('#everyUnit').val(alert.payload.every.substring(alert.payload.every.length - 1));

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

    #initializeAutocomplete() {
        const autoCompleteJS = new autoComplete({
            selector: "#expression",
            debounce: 100,
            trigger: (query) => {
                return true;
            },
            data: {
                src: async (query) => {
                    try {
                        // Fetch External Data Source
                        const source = await fetch('/api/alerting/alert/suggest', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify({expression: query})
                        });
                        const data = await source.json();
                        return data.suggestions.sort((a, b) => {
                            return a.text.localeCompare(b.text);
                        });
                    } catch (error) {
                        return error;
                    }
                },
                cache: false
            },
            placeHolder: "Use SPACE or ARROW DOWN to suggest",
            resultsList: {
                element: (list, data) => {
                },
                maxResults: 100,
                noResults: true,
                tabSelect: true
            },
            resultItem: {
                element: (item, data) => {
                    // Modify Results Item Style
                    item.style = "display: flex; justify-content: space-between;";
                    // Modify Results Item Content
                    item.innerHTML = `
      <span style="text-overflow: ellipsis; white-space: nowrap; overflow: hidden;">
        ${data.match}
      </span>
      <span style="display: flex; align-items: center; font-size: 13px; font-weight: 100; text-transform: uppercase; color: rgba(0,0,0,.2);">
        ${data.value.tag === null ? '' : data.value.tag.tagText}
      </span>`;
                },
                highlight: true
            },
            searchEngine: (query, record) => {
                // Override the default search engine
                // directly return the text so that all record will be displayed
                return record.text;
            },
            events: {
                input: {
                    beforeinput: (evt) => {
                    },
                    input: (evt) => {
                        const inputAscii = autoCompleteJS.input.value.substring(autoCompleteJS.input.selectionStart - 1).charCodeAt(0);
                        if (inputAscii >= 0x30 && inputAscii <= 0x39
                            // A-Z
                            || (inputAscii >= 65 && inputAscii < 65 + 26)
                            // a-z
                            || (inputAscii >= 97 && inputAscii < 97 + 26)
                            || (inputAscii === '_'.charCodeAt(0))
                            || (inputAscii === '-'.charCodeAt(0))
                            || (inputAscii === '$'.charCodeAt(0))) {
                            // For letter and digits input, disable the suggestion
                            return false;
                        }

                        autoCompleteJS.start(autoCompleteJS.input.value);
                        autoCompleteJS.suggestionPosition = autoCompleteJS.input.selectionStart;
                    },
                    selection: (evt) => {
                        const feedback = evt.detail;

                        // match is the value returned from 'searchEngine' above
                        autoCompleteJS.input.value = autoCompleteJS.input.value.substring(0, autoCompleteJS.suggestionPosition)
                            + feedback.selection.match;
                        autoCompleteJS.input.focus();
                    }
                }
            }
        });
    }
}
