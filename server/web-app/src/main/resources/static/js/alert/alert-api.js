class AlertApi {

    create(alert, onCreatedCallback) {
        $.ajax({
            type: "POST",
            url: "/api/alerting/alert/create",
            async: false,
            data: JSON.stringify(alert),
            dataType: "json",
            contentType: "application/yaml",
            success: (data) => {
                if (data.code !== 200) {
                    bootbox.alert({
                        backdrop: true,
                        title: "Error",
                        message: data.message
                    });
                    return;
                }
                onCreatedCallback(data.data);
            },
            error: (data) => {
                let message = '';
                if (data.responseJSON == null) {
                    message = data.responseText
                } else {
                    $.each(data.responseJSON, (p, v) => {
                        if (v !== null) {
                            message += '<b>' + p + '</b>';
                            message += ': ';
                            message += v;
                            message += '\n';
                        }
                    });
                }

                bootbox.alert({
                    backdrop: true,
                    title: "Error",
                    message: '<pre>' + message + '</pre>'
                });
            }
        });
    }

    update(alert, onUpdateCallback) {
        $.ajax({
            type: "POST",
            url: "/api/alerting/alert/update",
            async: false,
            data: JSON.stringify(alert),
            dataType: "json",
            contentType: "application/yaml",
            success: (data) => {
                if (data.code !== 200) {
                    bootbox.alert({
                        backdrop: true,
                        title: "Error",
                        message: data.message
                    });
                    return;
                }
                onUpdateCallback();
            },
            error: (data) => {
                let message = '';
                if (data.responseJSON == null) {
                    message = data.responseText
                } else {
                    $.each(data.responseJSON, (p, v) => {
                        if (v !== null) {
                            message += '<b>' + p + '</b>';
                            message += ': ';
                            message += v;
                            message += '\n';
                        }
                    });
                }

                bootbox.alert({
                    backdrop: true,
                    title: "Error",
                    message: '<pre>' + message + '</pre>'
                });
            }
        });
    }

    getAlertById(alertId, onGetCallback) {
        $.ajax({
            type: "POST",
            url: "/api/alerting/alert/get",
            async: true,
            data: JSON.stringify({
                alertId: alertId
            }),
            dataType: "json",
            contentType: "application/json",
            success: function (data) {
                console.log(data);
                if (data.code > 200) {
                    showMessage(data.message, "Error");
                    return;
                }
                onGetCallback(data.data);
            },
            error: function (data) {
                console.log(data);
                showMessage(data.responseJSON.message, "Error");
            }
        });
    }
}