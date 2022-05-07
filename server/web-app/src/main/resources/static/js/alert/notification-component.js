const gEditingNotification = {
    silencePeriod: 3,
    url: null
};

function notification_Bind(notificationSpec) {
    $("#silencePeriod").val(notificationSpec.silencePeriod).change();
    $("#dingUrl").val(notificationSpec.url).change();
}

$(document).ready(function () {
    $("#silencePeriod").change(function (e) {
        gEditingNotification.silencePeriod = e.target.value;
    });

    $("#dingUrl").change(function (e) {
        gEditingNotification.url = e.target.value;
    });
})