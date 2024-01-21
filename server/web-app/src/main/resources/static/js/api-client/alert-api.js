class AlertApi {

    getAlertById(alertId, onGetCallback) {
        $.ajax({
            type: "POST",
            url: "/alerting/api/alert/getAlertById",
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