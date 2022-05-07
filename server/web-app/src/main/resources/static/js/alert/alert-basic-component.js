const gEditingAlert = {
    appName: null,
    name: null,
    detectionLength: 3
};

$(document).ready(function () {
    $("#alertName").change(function (e) {
        gEditingAlert.name = e.target.value;
    });

    $("#application-app").change(function (e) {
        if (e.target.selectedIndex === 0) {
            gEditingAlert.appName = null;
        } else {
            gEditingAlert.appName = $(e.target.selectedOptions).text();
        }
    });

    $("#detectionLength").change(function (e) {
        gEditingAlert.detectionLength = e.target.value;
    })
});