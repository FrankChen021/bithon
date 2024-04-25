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

        $('#notifications').select2({
            theme: 'bootstrap4',
            allowClear: true,
            multiple: true,
            dropdownAutoWidth: true,
            placeholder: 'select notification channel'
        });

        this.#loadNotificationChannels();
        this.#initializeAutocomplete();

        // Must be after the autocomplete
        $('#expression').keydown((e)=>{
            if (!e.isDefaultPrevented() && e.keyCode === 13) {
                this.#renderExpression(true);
            }
        }).focus((e) => {
            if (!this._autoCompleteJS.isOpen && this._autoCompleteJS.input.value.trim() === '') {
                this._autoCompleteJS.start('');
            }
        });
    }

    getAlertObject() {
        const name = $("#name").val().trim();
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
            expr: expression,
            every: every + $('#everyUnit').val(),
            for: forValue,
            notifications: notification.map((n) => n.text)
        };
    }

    setAlertObject(alert) {
        $('#name').val(alert.name);
        $('#expression').val(alert.payload.expr);
        $('#for').val(alert.payload.for);
        $('#every').val(alert.payload.every.substring(0, alert.payload.every.length - 1));
        $('#everyUnit').val(alert.payload.every.substring(alert.payload.every.length - 1));

        $('#notifications').val(alert.payload.notifications).trigger('change');

        this.expressionDashboard.renderExpression(alert.payload.expr);
    }

    #renderExpression(checkApp) {
        const expr = $('#expression').val();
        if (checkApp && expr === '') {
            bootbox.alert({
                backdrop: true,
                title: "Error",
                message: '<pre>expression is not provided</pre>'
            });
            return;
        }

        this.expressionDashboard.renderExpression(expr);
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
        this._autoCompleteJS = new autoComplete({
            selector: "#expression",
            debounce: 100,
            submit: true, // Allow ENTER key take effect in INPUT
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
            placeHolder: "",
            resultsList: {
                element: (list, data) => {
                },
                maxResults: 100,

                // Since we control the auto suggestion by special char keyed in, no need to set this property
                noResults: false,
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
      <span style="display: flex; align-items: center; font-size: 13px; font-weight: 100;">
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
                        const inputAscii = this._autoCompleteJS.input.value.substring(this._autoCompleteJS.input.selectionStart - 1).charCodeAt(0);
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

                        this._autoCompleteJS.start(this._autoCompleteJS.input.value);
                        this._autoCompleteJS.suggestionPosition = this._autoCompleteJS.input.selectionStart;
                    },
                    selection: (evt) => {
                        const feedback = evt.detail;

                        // match is the value returned from 'searchEngine' above
                        this._autoCompleteJS.input.value = this._autoCompleteJS.input.value.substring(0, this._autoCompleteJS.suggestionPosition)
                            + feedback.selection.match;
                        this._autoCompleteJS.input.focus();
                    }
                }
            }
        });
    }
}
